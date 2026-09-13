package com.cadeteria.cadete.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Sube una foto directo a Cloudinary desde el celular (unsigned upload preset), igual
 * que hace admin-front — el backend nunca ve el archivo, solo la URL resultante. Se usa
 * para la foto de recepción y la de entrega (spec 3/5).
 */
class CloudinaryUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun subir(cloudName: String, uploadPreset: String, archivo: File, mimeType: String = "image/*"): Result<String> =
        withContext(Dispatchers.IO) {
            if (cloudName.isBlank() || uploadPreset.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Cloudinary no está configurado — pedile al admin que lo cargue en Configuración.")
                )
            }
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload_preset", uploadPreset)
                    .addFormDataPart(
                        "file", archivo.name,
                        archivo.asRequestBody(mimeType.toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$cloudName/auto/upload")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { resp ->
                    val texto = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(Exception("Cloudinary devolvió ${resp.code}: $texto"))
                    }
                    @Suppress("UNCHECKED_CAST")
                    val json = gson.fromJson(texto, Map::class.java) as Map<String, Any?>
                    val url = json["secure_url"] as? String
                        ?: return@withContext Result.failure(Exception("Cloudinary no devolvió secure_url."))
                    Result.success(url)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
