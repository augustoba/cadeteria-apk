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

/**
 * La cámara guarda el archivo con la orientación real del sensor marcada en el tag EXIF, sin
 * rotar los píxeles — hay que leer ese tag y rotar la imagen de verdad antes de mostrarla o
 * subirla, si no cualquier visor que no respete EXIF (o el que la reprocesa, como Cloudinary
 * en algunos casos) la muestra girada.
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
    if (grados == 0) return
    val original = BitmapFactory.decodeFile(archivo.absolutePath) ?: return
    val matriz = Matrix().apply { postRotate(grados.toFloat()) }
    val rotado = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
    FileOutputStream(archivo).use { out -> rotado.compress(Bitmap.CompressFormat.JPEG, 90, out) }
    original.recycle()
    rotado.recycle()
}

/** Bitmap ya corregido, para la vista previa en pantalla — el archivo en disco es lo que se sube. */
fun decodificarFotoCorregida(archivo: File): Bitmap? {
    corregirRotacionExif(archivo)
    return BitmapFactory.decodeFile(archivo.absolutePath)
}
