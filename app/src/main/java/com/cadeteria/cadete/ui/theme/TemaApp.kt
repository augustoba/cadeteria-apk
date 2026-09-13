package com.cadeteria.cadete.ui.theme

/** Preferencia de tema elegida por el cadete en Configuración — persiste en SessionManager. */
enum class TemaApp {
    SISTEMA,
    CLARO,
    OSCURO,
    ;

    companion object {
        fun from(nombre: String?): TemaApp = entries.find { it.name == nombre } ?: SISTEMA
    }
}
