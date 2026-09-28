package com.cadeteria.cadete.data.repository

import com.cadeteria.cadete.data.local.FinalizarPendiente
import com.cadeteria.cadete.data.local.ParadaPendiente
import com.cadeteria.cadete.data.local.PendingActionsStore
import com.cadeteria.cadete.data.local.RetiradoPendiente
import com.cadeteria.cadete.data.remote.dto.MarcaEnLugar
import java.io.File

/**
 * Reintenta las finalizaciones que quedaron encoladas por falta de internet (spec: modo
 * offline básico). CadeteApp la dispara sola cuando vuelve la conexión y también al
 * abrir la app — el cadete no tiene que hacer nada manualmente.
 *
 * Desde 2026-09-28 cada acción guarda la hora del toque, la precisión del GPS y si se usó
 * "Estoy en el lugar": el control de distancia ya se hizo en el teléfono al tocar el botón, y el
 * backend usa esa hora (no la de llegada) para Retirado/Entregado.
 */
class PendingActionsRepository(
    private val store: PendingActionsStore,
    private val pedidoRepository: PedidoRepository,
    private val cadeteRepository: CadeteRepository,
    private val cloudinaryUploader: CloudinaryUploader,
) {

    suspend fun encolarFinalizar(
        pedidoId: String,
        receptorNombre: String?,
        fotoUrl: String?,
        fotoPathLocal: String?,
        firmaUrl: String?,
        firmaPathLocal: String?,
        lat: Double?,
        lng: Double?,
        tocadoEn: String? = null,
        precision: Float? = null,
        enElLugar: Boolean? = null,
    ) {
        store.agregar(
            FinalizarPendiente(
                pedidoId, receptorNombre, fotoUrl, fotoPathLocal, firmaUrl, firmaPathLocal, lat, lng,
                tocadoEn, precision, enElLugar,
            ),
        )
    }

    suspend fun encolarRetirado(
        pedidoId: String,
        fotoUrl: String?,
        fotoPathLocal: String?,
        lat: Double?,
        lng: Double?,
        tocadoEn: String? = null,
        precision: Float? = null,
        enElLugar: Boolean? = null,
    ) {
        store.agregarRetiro(RetiradoPendiente(pedidoId, fotoUrl, fotoPathLocal, lat, lng, tocadoEn, precision, enElLugar))
    }

    suspend fun encolarParada(item: ParadaPendiente) {
        store.agregarParada(item)
    }

    suspend fun hayPendientes(): Boolean =
        store.listar().isNotEmpty() || store.listarRetiros().isNotEmpty() || store.listarParadas().isNotEmpty()

    /**
     * Reintenta todas las encoladas; la que vuelve a fallar por conexión queda para la próxima vez.
     * En el orden del viaje: el backend no acepta una parada ni la entrega sin el Retirado antes.
     */
    suspend fun reintentarTodas() {
        for (item in store.listarRetiros()) {
            if (reintentarRetiro(item)) store.quitarRetiro(item.pedidoId)
        }
        for (item in store.listarParadas()) {
            if (reintentarParada(item)) store.quitarParada(item.paradaId)
        }
        for (item in store.listar()) {
            if (reintentar(item)) store.quitar(item.pedidoId)
        }
    }

    private fun marca(tocadoEn: String?, enElLugar: Boolean?): MarcaEnLugar? =
        tocadoEn?.let { MarcaEnLugar(it, enElLugar == true) }

    /** Sube la foto encolada: la URL, null si no había, o Result.failure si no hay conexión todavía. */
    private suspend fun subirSiHaceFalta(url: String?, pathLocal: String?, tipo: String? = null): Result<Pair<String?, Boolean>> {
        if (url != null || pathLocal.isNullOrBlank()) return Result.success(url to false)
        val archivo = File(pathLocal)
        // Se limpió la cache: se marca igual sin foto antes que perder el viaje. El flag le avisa al
        // backend, que si no lo rechazaría por foto obligatoria y quedaría trabado acá.
        if (!archivo.exists()) return Result.success(null to true)
        val config = cadeteRepository.miConfiguracion().getOrNull() ?: return Result.failure(IllegalStateException())
        return cloudinaryUploader.subir(config.cloudinaryCloudName, config.cloudinaryUploadPreset, archivo, tipo ?: "image/*")
            .map { it to false }
    }

    private suspend fun reintentar(item: FinalizarPendiente): Boolean {
        val (fotoUrl, fotoPerdida) = subirSiHaceFalta(item.fotoUrl, item.fotoPathLocal).getOrElse { return false }
        val (firmaUrl, firmaPerdida) = subirSiHaceFalta(item.firmaUrl, item.firmaPathLocal, "image/jpeg").getOrElse { return false }
        return pedidoRepository.finalizar(
            item.pedidoId, item.receptorNombre, fotoUrl, firmaUrl, item.lat, item.lng, fotoPerdida || firmaPerdida,
            precision = item.precision, marca = marca(item.tocadoEn, item.enElLugar),
        ).isSuccess
    }

    private suspend fun reintentarRetiro(item: RetiradoPendiente): Boolean {
        val (fotoUrl, perdida) = subirSiHaceFalta(item.fotoUrl, item.fotoPathLocal).getOrElse { return false }
        return pedidoRepository.marcarRetirado(
            item.pedidoId, fotoUrl, item.lat, item.lng, perdida,
            precision = item.precision, marca = marca(item.tocadoEn, item.enElLugar),
        ).isSuccess
    }

    private suspend fun reintentarParada(item: ParadaPendiente): Boolean {
        val (fotoUrl, _) = subirSiHaceFalta(item.fotoUrl, item.fotoPathLocal).getOrElse { return false }
        return pedidoRepository.marcarParadaEntregada(
            item.pedidoId, item.paradaId, item.lat, item.lng, item.precision, fotoUrl, marca(item.tocadoEn, item.enElLugar),
        ).isSuccess
    }
}
