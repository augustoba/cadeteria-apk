package com.cadeteria.cadete.ui.home

import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import com.cadeteria.cadete.data.remote.dto.TipoAvisoCalle
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Textos de "Avisos de la calle" (carril C, 2026-09-28). Lógica pura para poder testearla. Una sola
 * línea corta: el cadete la ve manejando.
 */
object AvisosCalleTexto {

    const val SIN_CALLE = "cerca de tu ubicación"

    /** "2026-09-28T23:21:02.843508400Z" (Instant del backend) → ms. Sin java.time para andar en Android 7. */
    fun parsearIso(iso: String): Long? = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(iso.take(19))!!.time
    }.getOrNull()

    fun haceCuanto(creadoEn: String, ahoraMs: Long = System.currentTimeMillis()): String {
        val ms = parsearIso(creadoEn) ?: return ""
        val min = ((ahoraMs - ms) / 60_000).coerceAtLeast(0)
        return when {
            min < 1 -> "recién"
            min < 60 -> "hace $min min"
            else -> "hace ${min / 60} h"
        }
    }

    fun vigente(aviso: AvisoCalleDto, ahoraMs: Long = System.currentTimeMillis()): Boolean =
        (parsearIso(aviso.venceEn) ?: Long.MAX_VALUE) > ahoraMs

    /** "🚨 Control · Mate de Luna 2400 · hace 1 min" */
    fun linea(aviso: AvisoCalleDto, ahoraMs: Long = System.currentTimeMillis()): String =
        listOf("${TipoAvisoCalle.emoji(aviso.tipo)} ${aviso.tipoTexto}", aviso.calle ?: SIN_CALLE, haceCuanto(aviso.creadoEn, ahoraMs))
            .filter { it.isNotBlank() }
            .joinToString(" · ")

    /** Confirmación al que avisó: "Avisaste: Control en Mate de Luna 2400". */
    fun confirmacion(aviso: AvisoCalleDto): String =
        "Avisaste: ${aviso.tipoTexto} " + (aviso.calle?.let { "en $it" } ?: SIN_CALLE)
}
