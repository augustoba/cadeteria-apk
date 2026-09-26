package com.cadeteria.cadete.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Compartido entre ViajeScreen (fotos de retiro/entrega) y PerfilScreen ("Actualizar mis
 * datos") — con TakePicture() + FileProvider en vez de TakePicturePreview() (el thumbnail de
 * baja resolución que ignoraba el tag EXIF de orientación, bug reportado 2026-09-23).
 */
fun crearArchivoFotoTemporal(context: Context): Pair<File, Uri> {
    val archivo = File(context.cacheDir, "foto_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    return archivo to uri
}

/** Lado largo máximo de una foto subida: alcanza para ver el paquete, la firma o un documento. */
private const val LADO_MAX_PX = 1600
private const val CALIDAD_JPEG = 80

/**
 * La cámara guarda el archivo con la orientación real del sensor marcada en el tag EXIF, sin
 * rotar los píxeles — hay que leer ese tag y rotar la imagen de verdad antes de mostrarla o
 * subirla, si no cualquier visor que no respete EXIF (o el que la reprocesa, como Cloudinary
 * en algunos casos) la muestra girada.
 *
 * También la achica (2026-09-26): la cámara la guarda en resolución completa (3-5 MB en un Moto
 * G32) y así se subía a Cloudinary — ~30 GB a 70 viajes/día con 60 días de retención, fuera del plan
 * gratis, y lenta de subir con datos móviles. A 1600 px y calidad 80 queda en ~0,3 MB. Si ya está
 * derecha y chica no la vuelve a comprimir (se llama más de una vez sobre el mismo archivo).
 */
fun corregirRotacionExif(archivo: File) {
    val grados = try {
        when (ExifInterface(archivo.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(archivo.absolutePath, medidas)
    val ladoLargo = maxOf(medidas.outWidth, medidas.outHeight)
    if (ladoLargo <= 0) return
    if (grados == 0 && ladoLargo <= LADO_MAX_PX) return

    // Decodificar ya reducida (potencia de 2) para no cargar 50 MP en memoria en un celular chico.
    var muestreo = 1
    while (ladoLargo / (muestreo * 2) >= LADO_MAX_PX) muestreo *= 2
    val original = BitmapFactory.decodeFile(archivo.absolutePath, BitmapFactory.Options().apply { inSampleSize = muestreo })
        ?: return
    val escala = minOf(1f, LADO_MAX_PX.toFloat() / maxOf(original.width, original.height))
    val matriz = Matrix().apply {
        postScale(escala, escala)
        postRotate(grados.toFloat())
    }
    val final = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
    FileOutputStream(archivo).use { out -> final.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, out) }
    if (final !== original) final.recycle()
    original.recycle()
}

/** Bitmap ya corregido, para la vista previa en pantalla — el archivo en disco es lo que se sube. */
fun decodificarFotoCorregida(archivo: File): Bitmap? {
    corregirRotacionExif(archivo)
    return BitmapFactory.decodeFile(archivo.absolutePath)
}
