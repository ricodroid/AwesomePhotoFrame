package com.example.awesomephotoframe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.awesomephotoframe.data.repository.PhotoRepository
import com.example.awesomephotoframe.data.repository.WeatherRepository

class ViewModelFactory(
    private val photoRepository: PhotoRepository,
    private val weatherRepository: WeatherRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(photoRepository) as T
            }
            modelClass.isAssignableFrom(WeatherViewModel::class.java) -> {
                WeatherViewModel(weatherRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
