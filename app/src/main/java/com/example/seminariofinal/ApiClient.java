package com.example.seminariofinal;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;

import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8000/";
    private static final CookieManager cookieManager =
            new CookieManager(null, CookiePolicy.ACCEPT_ALL);

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Django exige X-CSRFToken (valor de la cookie csrftoken) en los POST
            Interceptor csrf = chain -> {
                Request original = chain.request();
                Request.Builder b = original.newBuilder();
                if (!"GET".equals(original.method())) {
                    for (HttpCookie c : cookieManager.getCookieStore().getCookies()) {
                        if ("csrftoken".equals(c.getName())) {
                            b.header("X-CSRFToken", c.getValue());
                            b.header("Referer", BASE_URL);
                        }
                    }
                }
                return chain.proceed(b.build());
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(new JavaNetCookieJar(cookieManager))
                    .addInterceptor(csrf)
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}