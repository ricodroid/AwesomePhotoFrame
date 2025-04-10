package com.example.awesomephotoframe.data.repository


import com.example.awesomephotoframe.data.api.RetrofitClient
import com.example.awesomephotoframe.data.model.WeatherResponse
import retrofit2.Response

class WeatherRepository {
    private val api = RetrofitClient.instance

    suspend fun fetchWeather(lat: Double, lon: Double): Response<WeatherResponse> {
        return api.getWeatherData(lat, lon)
    }
}
