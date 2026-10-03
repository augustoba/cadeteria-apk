package com.cadeteria.cadete.ui.theme

/**
 * Preferencia de tema elegida por el cadete en Configuración — persiste en SessionManager.
 * Sin nada elegido arranca en Claro (2026-10-03): con "Sistema" por defecto la app salía oscura en
 * celulares con el ahorro de batería prendido, y en la calle con sol el claro se lee mejor.
 */
enum class TemaApp {
    SISTEMA,
    CLARO,
    OSCURO,
    ;

    companion object {
        fun from(nombre: String?): TemaApp = entries.find { it.name == nombre } ?: CLARO
    }
}
