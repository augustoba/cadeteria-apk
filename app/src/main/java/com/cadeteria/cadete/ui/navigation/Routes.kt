package com.cadeteria.cadete.ui.navigation

object Routes {
    const val SERVIDOR = "servidor"
    const val LOGIN = "login"
    const val RECUPERAR_PASSWORD = "recuperar_password"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val VIAJE = "viaje/{pedidoId}"
    const val CHAT = "chat"
    const val PERFIL = "perfil"
    const val HISTORIAL = "historial"
    const val AVISOS = "avisos"
    const val AYUDA = "ayuda"

    fun viaje(pedidoId: String) = "viaje/$pedidoId"
}
