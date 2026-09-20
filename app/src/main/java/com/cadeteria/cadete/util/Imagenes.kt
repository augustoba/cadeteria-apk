package com.cadeteria.cadete.util

private const val MARCA = "/image/upload/"

/**
 * Inserta una transformación de Cloudinary en una URL para no bajar el original.
 *
 * Las fotos se suben desde el celular y se sirven crudas: un avatar de 64 px bajaba varios
 * MB. Cloudinary transforma por URL, así que esto arregla también lo ya subido.
 *
 * Si la URL no es de Cloudinary, o ya trae una transformación, se devuelve tal cual —
 * romper una URL que hoy funciona sería peor que el problema.
 */
fun optimizarImagen(url: String?, ancho: Int): String? {
    if (url.isNullOrBlank() || !url.contains("res.cloudinary.com")) return url

    val i = url.indexOf(MARCA)
    if (i == -1) return url

    val resto = url.substring(i + MARCA.length)
    val barra = resto.indexOf('/')
    if (barra == -1) return url

    val primero = resto.substring(0, barra)
    // "v1234567" es la versión del asset; cualquier otro segmento con "_" o "," ya es una
    // transformación puesta por otro lado.
    val esVersion = Regex("^v\\d+$").matches(primero)
    if (!esVersion && (primero.contains('_') || primero.contains(','))) return url

    return url.substring(0, i + MARCA.length) + "f_auto,q_auto,c_limit,w_$ancho/" + resto
}
