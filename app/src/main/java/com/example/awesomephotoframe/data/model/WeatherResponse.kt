package com.example.awesomephotoframe.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("properties") val properties: Properties
)

data class Properties(
    @SerializedName("timeseries") val timeseries: List<TimeSeries>
)

data class TimeSeries(
    @SerializedName("time") val time: String,
    @SerializedName("data") val data: Data
)

data class Data(
    @SerializedName("instant") val instant: InstantData,
    @SerializedName("next_1_hours") val next1Hours: NextHoursData? = null
)

data class InstantData(
    @SerializedName("details") val details: Details
)

data class Details(
    @SerializedName("air_temperature") val airTemperature: Double,
    @SerializedName("air_pressure_at_sea_level") val airPressure: Double?,
    @SerializedName("cloud_area_fraction") val cloudFraction: Double?,
    @SerializedName("relative_humidity") val humidity: Double?,
    @SerializedName("wind_speed") val windSpeed: Double?,
    @SerializedName("wind_from_direction") val windDirection: Double?,
    @SerializedName("dew_point_temperature") val dewPoint: Double?
)

data class NextHoursData(
    @SerializedName("summary") val summary: Summary
)

data class Summary(
    @SerializedName("symbol_code") val symbolCode: String
)