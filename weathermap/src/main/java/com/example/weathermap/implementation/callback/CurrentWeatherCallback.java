package com.example.weathermap.implementation.callback;


import com.example.weathermap.model.currentweather.CurrentWeather;

public interface CurrentWeatherCallback{
    void onSuccess(CurrentWeather currentWeather);
    void onFailure(Throwable throwable);
}
