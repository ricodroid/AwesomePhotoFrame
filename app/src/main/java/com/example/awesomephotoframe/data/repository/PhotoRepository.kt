package com.example.awesomephotoframe.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PhotoRepository {

    private val api: GooglePhotosApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://photoslibrary.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GooglePhotosApi::class.java)
    }

    /**
     * 認可コードをアクセストークンに変換する
     */
    suspend fun exchangeAuthCodeForAccessToken(
        authCode: String,
        clientId: String,
        clientSecret: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://oauth2.googleapis.com/token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val postData = listOf(
                "code" to authCode,
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "redirect_uri" to "",
                "grant_type" to "authorization_code"
            ).joinToString("&") { "${it.first}=${URLEncoder.encode(it.second, "UTF-8")}" }

            conn.outputStream.use { it.write(postData.toByteArray()) }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            return@withContext json.getString("access_token")
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Failed to exchange token", e)
            return@withContext null
        }
    }

    /**
     * アクセストークンを使ってランダムな写真URLを取得
     */
    suspend fun fetchPhotoUrl(accessToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val allItems = mutableListOf<MediaItem>()
            var pageToken: String? = null

            do {
                val response = api.listMediaItems("Bearer $accessToken", pageToken)
                response.mediaItems?.let { allItems.addAll(it) }
                pageToken = response.nextPageToken
            } while (pageToken != null)

            if (allItems.isNotEmpty()) {
                return@withContext allItems.random().baseUrl + "=w4096-h4096"
            }

            return@withContext null
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Failed to fetch photo URL", e)
            return@withContext null
        }
    }
}
