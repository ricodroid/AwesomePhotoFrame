package com.example.awesomephotoframe.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awesomephotoframe.data.model.WeatherResponse
import com.example.awesomephotoframe.data.repository.WeatherRepository
import kotlinx.coroutines.launch

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    private val _weather = MutableLiveData<WeatherResponse>()
    val weather: LiveData<WeatherResponse> = _weather

    fun loadWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val response = repository.fetchWeather(lat, lon)
                if (response.isSuccessful) {
                    response.body()?.let { weatherData ->
                        _weather.postValue(weatherData)
                    }
                } else {
                    // エラー処理（必要ならログやUI通知など）
                    Log.e("WeatherViewModel", "API error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Exception: ${e.message}")
            }
        }
    }
}
