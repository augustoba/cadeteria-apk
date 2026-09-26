package com.cadeteria.cadete.util

/**
 * Formatos de los datos que carga el cadete (2026-09-26). Son los MISMOS que valida el backend
 * (`common/Validaciones.java`) y el panel (`core/utils/validaciones.ts`): la app avisa antes de
 * enviar, el backend es el que manda. Si cambia uno, cambiar los otros.
 */
object Validaciones {
    val NOMBRE_PERSONA = Regex("^\\s*\\p{L}+(?:[ '.\\-]+\\p{L}+)*\\.?\\s*$")
    val TELEFONO = Regex("^(?=(?:\\D*\\d){7,15}\\D*$)\\s*\\+?[0-9 ()\\-]+\\s*$")
    val PATENTE_MOTO = Regex("^\\s*(?:\\d{3}[\\s\\-]?[A-Za-z]{3}|[A-Za-z][\\s\\-]?\\d{3}[\\s\\-]?[A-Za-z]{3})\\s*$")
    val MARCA_MODELO = Regex("^[\\p{L}0-9 .\\-]{1,40}$")
    val COLOR = Regex("^[\\p{L} ]{1,30}$")
    val CBU = Regex("^\\d{22}$")
    val ALIAS_CBU = Regex("^[A-Za-z0-9.\\-]{6,20}$")
    val CODIGO_6 = Regex("^\\s*\\d{6}\\s*$")
    const val PASSWORD_MIN = 6
    const val PASSWORD_MAX = 72

    const val MSJ_RECEPTOR = "El nombre de quien recibe solo puede tener letras y espacios."
    const val MSJ_TELEFONO = "El teléfono solo puede tener números (7 a 15 dígitos; se aceptan +, espacios y guiones)."
    const val MSJ_PATENTE = "La patente tiene que ser del formato viejo (123ABC) o del nuevo (A123BCD)."
    const val MSJ_MARCA = "La marca solo puede tener letras, números y espacios (hasta 40)."
    const val MSJ_MODELO = "El modelo solo puede tener letras, números y espacios (hasta 40)."
    const val MSJ_COLOR = "El color solo puede tener letras (hasta 30)."
    const val MSJ_CBU = "El CBU/CVU tiene que tener exactamente 22 números."
    const val MSJ_ALIAS = "El alias tiene que tener de 6 a 20 caracteres: letras, números, punto o guion."
    const val MSJ_PASSWORD = "La contraseña tiene que tener entre 6 y 72 caracteres."
    const val MSJ_CODIGO = "El código tiene que ser de 6 números."

    /** Campo opcional: vacío vale; si tiene algo, tiene que cumplir el formato. */
    fun vacioO(re: Regex, valor: String?): Boolean = valor.isNullOrBlank() || re.matches(valor)

    fun passwordValida(p: String): Boolean = p.length in PASSWORD_MIN..PASSWORD_MAX

    /** "a 123 bcd" → "A123BCD". */
    fun normalizarPatente(p: String): String = p.replace(Regex("[\\s\\-]"), "").uppercase()

    /** Junta los problemas: cada par es (hay error, mensaje). Null si está todo bien. */
    fun problemas(vararg chequeos: Pair<Boolean, String>): String? =
        chequeos.filter { it.first }.joinToString(" · ") { it.second }.ifBlank { null }
}
