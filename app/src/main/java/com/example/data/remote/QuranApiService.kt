package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface QuranApiService {

    /**
     * GET /api/v1/quran/surat
     * Mendapatkan daftar seluruh 114 surah Al-Qur’an lengkap dengan nama Arab, Latin,
     * terjemahan, jumlah ayat, dan nomor urut.
     */
    @GET("api/v1/quran/surat")
    suspend fun getAllSurat(): Response<ResponseBody>

    /**
     * GET /api/v1/quran/surat/{nomor}
     * Mendapatkan isi satu surah lengkap dengan teks Arab, latin, terjemahan,
     * audio per ayat, dan informasi surah.
     */
    @GET("api/v1/quran/surat/{nomor}")
    suspend fun getSuratDetail(
        @Path("nomor") nomor: Int
    ): Response<ResponseBody>

    /**
     * GET /api/v1/quran/tafsir/{nomor}
     * Mendapatkan tafsir ayat-ayat suatu surah dari Kementerian Agama RI.
     */
    @GET("api/v1/quran/tafsir/{nomor}")
    suspend fun getSuratTafsir(
        @Path("nomor") nomor: Int
    ): Response<ResponseBody>

    /**
     * GET /api/v1/quran/search
     * Mencari ayat Al-Qur’an berdasarkan kata kunci di terjemahan Bahasa Indonesia,
     * dengan teks Arab + terjemahan dan paginasi.
     */
    @GET("api/v1/quran/search")
    suspend fun searchAyat(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://apiku.irengcloud.com/"

        fun create(): QuranApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Accept", "application/json")
                        .header("User-Agent", "NurQuran-Android/1.0")
                        .build()
                    chain.proceed(request)
                }
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .build()
                .create(QuranApiService::class.java)
        }
    }
}
