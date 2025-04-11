package com.example.awesomephotoframe.data.repository

import com.example.awesomephotoframe.data.model.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface WeatherApi {
    @GET("locationforecast/2.0/compact")
    suspend fun getWeatherData(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Header("User-Agent") userAgent: String = "AwesomePhotoFrame/1.0 ricodroid2024@gmail.com"
    ): Response<WeatherResponse>
}
