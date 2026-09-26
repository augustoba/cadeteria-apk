package com.cadeteria.cadete.location

import com.cadeteria.cadete.data.remote.dto.PedidoDto
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * "Llegaste al retiro / a la entrega — no te olvides de marcarlo" (2026-09-26).
 *
 * Lógica pura (sin Android) para poder testearla: dado cada ping de ubicación del
 * [LocationTrackingService] y los viajes EN_CURSO del cadete, decide si hay que avisar. No usa
 * la API de geofences de Google (pediría el permiso de ubicación "todo el tiempo", que la app
 * no tiene): el servicio ya recibe la posición mientras el cadete está disponible.
 *
 * Para no avisar al pasar por al lado, hace falta **quedarse** dentro del radio: el primer ping
 * adentro marca la entrada y recién se avisa si un ping posterior sigue adentro después de
 * [PERMANENCIA_MS]. Un solo aviso por punto (retiro, cada parada, entrega); si el cadete ya lo
 * marcó, el punto deja de estar pendiente y el aviso no sale (o se borra si ya estaba).
 */
class AvisoLlegada(
    private val radioM: Double = RADIO_M,
    private val permanenciaMs: Long = PERMANENCIA_MS,
    private val precisionMaximaM: Float = PRECISION_MAXIMA_M,
) {
    companion object {
        /** Holgado a propósito: el pin del origen puede estar corrido y en el centro el GPS rebota. */
        const val RADIO_M = 150.0
        /** Tiempo adentro del radio antes de avisar (un retiro/entrega real lleva más que esto). */
        const val PERMANENCIA_MS = 40_000L
        /** Un ping con más error que esto no sirve para decir "está en la puerta". */
        const val PRECISION_MAXIMA_M = 250f

        fun distanciaM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
            return 2 * r * asin(sqrt(a))
        }
    }

    enum class Tipo { RETIRO, PARADA, ENTREGA }

    data class Punto(
        /** Única por viaje y punto: "pedidoId:RETIRO", "pedidoId:PARADA:paradaId", "pedidoId:ENTREGA". */
        val clave: String,
        val tipo: Tipo,
        val pedidoId: String,
        val numero: Long,
        val direccion: String,
        val lat: Double,
        val lng: Double,
    )

    data class Aviso(val punto: Punto, val titulo: String, val cuerpo: String)

    /** Cuándo entró al radio de cada punto (se borra al salir). */
    private val entradas = mutableMapOf<String, Long>()
    /** Puntos ya avisados: no se repiten aunque salga y vuelva a entrar. */
    private val avisados = mutableSetOf<String>()

    /**
     * Lo que falta marcar en cada viaje EN_CURSO: el retiro si todavía no retiró; si ya retiró,
     * cada parada sin entregar y la entrega final.
     */
    fun puntosPendientes(viajes: List<PedidoDto>): List<Punto> = viajes
        .filter { it.estado.id == "EN_CURSO" }
        .flatMap { p ->
            if (p.retiradoEn == null) {
                listOf(Punto("${p.id}:RETIRO", Tipo.RETIRO, p.id, p.numero, p.origenDireccion, p.origenLat, p.origenLng))
            } else {
                p.paradas.filter { it.entregadoEn == null }.sortedBy { it.orden }.map {
                    Punto("${p.id}:PARADA:${it.id}", Tipo.PARADA, p.id, p.numero, it.direccion, it.lat, it.lng)
                } + Punto("${p.id}:ENTREGA", Tipo.ENTREGA, p.id, p.numero, p.destinoDireccion, p.destinoLat, p.destinoLng)
            }
        }

    /**
     * Procesa un ping. Devuelve los avisos a mostrar ahora (casi siempre ninguno). También
     * olvida los puntos que ya no están pendientes, para que [clavesResueltas] los informe.
     */
    fun procesar(pendientes: List<Punto>, lat: Double, lng: Double, precisionM: Float?, ahoraMs: Long): List<Aviso> {
        val vigentes = pendientes.map { it.clave }.toSet()
        entradas.keys.retainAll(vigentes)
        if (precisionM != null && precisionM > precisionMaximaM) return emptyList()

        val avisos = mutableListOf<Aviso>()
        for (punto in pendientes) {
            if (punto.clave in avisados) continue
            val adentro = distanciaM(lat, lng, punto.lat, punto.lng) <= radioM
            if (!adentro) {
                entradas.remove(punto.clave)
                continue
            }
            val entrada = entradas.getOrPut(punto.clave) { ahoraMs }
            if (ahoraMs - entrada >= permanenciaMs) {
                avisados += punto.clave
                entradas.remove(punto.clave)
                avisos += armarAviso(punto)
            }
        }
        return avisos
    }

    /** Avisos ya mostrados cuyo punto se marcó después: la notificación se puede borrar. */
    fun clavesResueltas(pendientes: List<Punto>): Set<String> {
        val vigentes = pendientes.map { it.clave }.toSet()
        val resueltas = avisados - vigentes
        avisados.removeAll(resueltas)
        return resueltas
    }

    private fun armarAviso(p: Punto): Aviso = when (p.tipo) {
        Tipo.RETIRO -> Aviso(p, "Llegaste al retiro — pedido #${p.numero}",
            "Estás en ${p.direccion}. No te olvides de marcar \"Retirado\" cuando lo tengas.")
        Tipo.PARADA -> Aviso(p, "Llegaste a una parada — pedido #${p.numero}",
            "Estás en ${p.direccion}. No te olvides de marcar la parada como entregada.")
        Tipo.ENTREGA -> Aviso(p, "Llegaste a la entrega — pedido #${p.numero}",
            "Estás en ${p.direccion}. No te olvides de marcar \"Entregado\" al dejarlo.")
    }
}
