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

    /** Lo que este viaje tiene guardado sin señal, para que la pantalla no deje marcarlo de nuevo (2026-09-29). */
    data class EncoladoDelViaje(val finalizar: Boolean, val retiro: RetiradoPendiente?, val paradas: Set<String>)

    suspend fun encoladoDe(pedidoId: String): EncoladoDelViaje = EncoladoDelViaje(
        finalizar = store.listar().any { it.pedidoId == pedidoId },
        retiro = store.listarRetiros().firstOrNull { it.pedidoId == pedidoId },
        paradas = store.listarParadas().filter { it.pedidoId == pedidoId }.map { it.paradaId }.toSet(),
    )

    /** Viajes con algo guardado sin señal (Retirado, parada o Entregado), para marcarlos en Inicio. */
    suspend fun pedidosConEncolados(): Set<String> =
        (store.listar().map { it.pedidoId } + store.listarRetiros().map { it.pedidoId } +
            store.listarParadas().map { it.pedidoId }).toSet()

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
            .onFailure { e -> if (e !is java.io.IOException) contarFallaDeSubida(pathLocal, e) }
            .map { it to false }
    }

    /**
     * Falla de subida que no es falta de señal (ej. Cloudinary sin configurar): la cola la reintenta
     * igual, pero a la [FALLAS_ANTES_DE_AVISAR]ª vez se le avisa al cadete una sola vez, para que no
     * quede esperando para siempre sin saberlo (2026-09-28, pruebas de A + B).
     */
    private val fallasDeSubida = mutableMapOf<String, Int>()

    /** Lo engancha CadeteApp para mostrar la notificación. */
    var avisarFallaDeSubida: ((String) -> Unit)? = null

    private fun contarFallaDeSubida(pathLocal: String, e: Throwable) {
        val veces = (fallasDeSubida[pathLocal] ?: 0) + 1
        fallasDeSubida[pathLocal] = veces
        if (veces == FALLAS_ANTES_DE_AVISAR) {
            avisarFallaDeSubida?.invoke(
                "Hay un Retirado/Entregado guardado sin señal que no se puede mandar porque la foto no sube" +
                    (e.message?.let { ": $it" } ?: ".") + " Avisale a la administración.",
            )
        }
    }

    companion object {
        const val FALLAS_ANTES_DE_AVISAR = 3

        /** 4xx de negocio (400/404/409/422…): reintentar no lo arregla. Sesión, límite y 5xx sí pueden pasar. */
        fun esRechazoDefinitivo(e: Throwable): Boolean {
            val codigo = (e as? retrofit2.HttpException)?.code() ?: return false
            return codigo in 400..499 && codigo !in setOf(401, 403, 408, 429)
        }

        /** El "message" del ApiError del backend (con Gson: org.json no anda en los tests de JVM). */
        fun motivoDelRechazo(e: Throwable): String? {
            val cuerpo = runCatching { (e as? retrofit2.HttpException)?.response()?.errorBody()?.string() }.getOrNull()
                ?: return null
            return runCatching {
                com.google.gson.JsonParser.parseString(cuerpo).asJsonObject.get("message")?.asString
            }.getOrNull()?.takeIf { it.isNotBlank() }
        }
    }

    private suspend fun reintentar(item: FinalizarPendiente): Boolean {
        val (fotoUrl, fotoPerdida) = subirSiHaceFalta(item.fotoUrl, item.fotoPathLocal).getOrElse { return false }
        val (firmaUrl, firmaPerdida) = subirSiHaceFalta(item.firmaUrl, item.firmaPathLocal, "image/jpeg").getOrElse { return false }
        return pedidoRepository.finalizar(
            item.pedidoId, item.receptorNombre, fotoUrl, firmaUrl, item.lat, item.lng, fotoPerdida || firmaPerdida,
            precision = item.precision, marca = marca(item.tocadoEn, item.enElLugar),
        ).salioDeLaCola("la entrega")
    }

    private suspend fun reintentarRetiro(item: RetiradoPendiente): Boolean {
        val (fotoUrl, perdida) = subirSiHaceFalta(item.fotoUrl, item.fotoPathLocal).getOrElse { return false }
        return pedidoRepository.marcarRetirado(
            item.pedidoId, fotoUrl, item.lat, item.lng, perdida,
            precision = item.precision, marca = marca(item.tocadoEn, item.enElLugar),
        ).salioDeLaCola("el Retirado")
    }

    private suspend fun reintentarParada(item: ParadaPendiente): Boolean {
        val (fotoUrl, _) = subirSiHaceFalta(item.fotoUrl, item.fotoPathLocal).getOrElse { return false }
        return pedidoRepository.marcarParadaEntregada(
            item.pedidoId, item.paradaId, item.lat, item.lng, item.precision, fotoUrl, marca(item.tocadoEn, item.enElLugar),
        ).salioDeLaCola("la parada")
    }

    /**
     * true = sale de la cola: se mandó, o el servidor lo rechazó para siempre (2026-09-29). Como el
     * viaje queda bloqueado mientras tiene algo encolado, un rechazo definitivo que se quedara en la
     * cola lo trabaría para siempre: se saca y se le avisa al cadete para que lo vuelva a marcar.
     */
    private fun Result<*>.salioDeLaCola(que: String): Boolean {
        if (isSuccess) return true
        val e = exceptionOrNull() ?: return false
        if (!esRechazoDefinitivo(e)) return false
        avisarFallaDeSubida?.invoke(
            "No se pudo guardar $que que marcaste sin señal" + (motivoDelRechazo(e)?.let { ": $it" } ?: ".") +
                " Entrá al viaje y volvé a marcarlo.",
        )
        return true
    }
}
