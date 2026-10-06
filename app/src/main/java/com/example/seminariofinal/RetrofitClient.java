package com.example.seminariofinal;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // ⚠️ Si usas el Emulador de Android Studio: usa "http://10.0.2.2:8000/"
    // ⚠️ Si usas un celular físico por USB: usa la IP de tu computadora (ejemplo: "http://192.168.1.15:8000/")
    private static final String BASE_URL = "http://10.0.2.2:8000/";

    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}