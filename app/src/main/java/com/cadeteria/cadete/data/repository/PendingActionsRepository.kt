package com.cadeteria.cadete.data.repository

import com.cadeteria.cadete.data.local.FinalizarPendiente
import com.cadeteria.cadete.data.local.PendingActionsStore
import com.cadeteria.cadete.data.local.RetiradoPendiente
import java.io.File

/**
 * Reintenta las finalizaciones que quedaron encoladas por falta de internet (spec: modo
 * offline básico). CadeteApp la dispara sola cuando vuelve la conexión y también al
 * abrir la app — el cadete no tiene que hacer nada manualmente.
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
    ) {
        store.agregar(FinalizarPendiente(pedidoId, receptorNombre, fotoUrl, fotoPathLocal, firmaUrl, firmaPathLocal, lat, lng))
    }

    suspend fun encolarRetirado(pedidoId: String, fotoUrl: String?, fotoPathLocal: String?, lat: Double?, lng: Double?) {
        store.agregarRetiro(RetiradoPendiente(pedidoId, fotoUrl, fotoPathLocal, lat, lng))
    }

    suspend fun hayPendientes(): Boolean = store.listar().isNotEmpty() || store.listarRetiros().isNotEmpty()

    /** Reintenta todas las encoladas; la que vuelve a fallar por conexión queda para la próxima vez. */
    suspend fun reintentarTodas() {
        for (item in store.listar()) {
            if (reintentar(item)) store.quitar(item.pedidoId)
        }
        for (item in store.listarRetiros()) {
            if (reintentarRetiro(item)) store.quitarRetiro(item.pedidoId)
        }
    }

    private suspend fun reintentar(item: FinalizarPendiente): Boolean {
        var fotoUrl = item.fotoUrl
        if (fotoUrl == null && !item.fotoPathLocal.isNullOrBlank()) {
            val archivo = File(item.fotoPathLocal)
            if (archivo.exists()) {
                val config = cadeteRepository.miConfiguracion().getOrNull() ?: return false
                fotoUrl = cloudinaryUploader.subir(config.cloudinaryCloudName, config.cloudinaryUploadPreset, archivo)
                    .getOrElse { return false }
            }
            // si el archivo ya no existe (se limpió la cache), se finaliza igual sin foto antes que perder el viaje.
        }
        var firmaUrl = item.firmaUrl
        if (firmaUrl == null && !item.firmaPathLocal.isNullOrBlank()) {
            val archivo = File(item.firmaPathLocal)
            if (archivo.exists()) {
                val config = cadeteRepository.miConfiguracion().getOrNull() ?: return false
                firmaUrl = cloudinaryUploader.subir(config.cloudinaryCloudName, config.cloudinaryUploadPreset, archivo, "image/jpeg")
                    .getOrElse { return false }
            }
        }
        return pedidoRepository.finalizar(item.pedidoId, item.receptorNombre, fotoUrl, firmaUrl, item.lat, item.lng).isSuccess
    }

    private suspend fun reintentarRetiro(item: RetiradoPendiente): Boolean {
        var fotoUrl = item.fotoUrl
        if (fotoUrl == null && !item.fotoPathLocal.isNullOrBlank()) {
            val archivo = File(item.fotoPathLocal)
            if (archivo.exists()) {
                val config = cadeteRepository.miConfiguracion().getOrNull() ?: return false
                fotoUrl = cloudinaryUploader.subir(config.cloudinaryCloudName, config.cloudinaryUploadPreset, archivo)
                    .getOrElse { return false }
            }
        }
        return pedidoRepository.marcarRetirado(item.pedidoId, fotoUrl, item.lat, item.lng).isSuccess
    }
}
