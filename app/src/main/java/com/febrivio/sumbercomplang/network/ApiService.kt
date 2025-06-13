package com.febrivio.sumbercomplang.network

import com.febrivio.sumbercomplang.model.DashboardResponse
import com.febrivio.sumbercomplang.model.Kolam
import com.febrivio.sumbercomplang.model.KolamListResponse
import com.febrivio.sumbercomplang.model.KolamResponse
import com.febrivio.sumbercomplang.model.LoginRequest
import com.febrivio.sumbercomplang.model.LoginResponse
import com.febrivio.sumbercomplang.model.ProfileUpdateRequest
import com.febrivio.sumbercomplang.model.ProfileUpdateResponse
import com.febrivio.sumbercomplang.model.RegisterRequest
import com.febrivio.sumbercomplang.model.RegisterResponse
import com.febrivio.sumbercomplang.model.RiwayatTransaksiTiketResponse
import com.febrivio.sumbercomplang.model.TiketRequest
import com.febrivio.sumbercomplang.model.TiketResponse
import com.febrivio.sumbercomplang.model.TiketValidationRequest
import com.febrivio.sumbercomplang.model.TiketValidationResponse
import com.febrivio.sumbercomplang.model.TransaksiTiketRequest
import com.febrivio.sumbercomplang.model.TransaksiTiketResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {

    @POST("register")
    fun registerUser(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @Multipart
    @POST("kolam")
    fun createKolam(
        @Part("nama") nama: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody?,
        @Part("kedalaman") kedalaman: RequestBody?,
        @Part("luas") luas: RequestBody?,
        @Part url_foto: MultipartBody.Part?
    ): Call<KolamResponse>

    // Dengan gambar
    @Multipart
    @POST("kolam/{id}?_method=PUT")
    fun updateKolamWithImage(
        @Path("id") id: Int,
        @Part("nama") nama: RequestBody?,
        @Part("deskripsi") deskripsi: RequestBody?,
        @Part("kedalaman") kedalaman: RequestBody?,
        @Part("luas") luas: RequestBody?,
        @Part url_foto: MultipartBody.Part?
    ): Call<KolamResponse>

    // Tanpa gambar
    @Multipart
    @POST("kolam/{id}?_method=PUT")
    fun updateKolamWithoutImage(
        @Path("id") id: Int,
        @Part("nama") nama: RequestBody?,
        @Part("deskripsi") deskripsi: RequestBody?,
        @Part("kedalaman") kedalaman: RequestBody?,
        @Part("luas") luas: RequestBody?
    ): Call<KolamResponse>

    @GET("kolam")
    fun getKolam(): Call<KolamListResponse>

    @DELETE("kolam/{id}")
    fun deleteKolam(
        @Path("id") id: Int
    ): Call<KolamResponse>

    @GET("tiket")
    fun getTiket(
        @Query("jenis") jenis: String
    ): Call<TiketResponse>

    @PUT("tiket/{id}")
    fun updateTiket(
        @Path("id") id: String,
        @Body tiket: TiketRequest
    ): Call<ResponseBody>

    @POST("transaksi-tiket")
    fun createTransaksiTiket(@Body body: TransaksiTiketRequest): Call<TransaksiTiketResponse>

    @GET("transaksi-tiket")
    fun getRiwayatTransaksi(
        @Query("jenis") jenis: String,
        @Query("status") status: String,
        @Query("page") page: Int,
    ): Call<RiwayatTransaksiTiketResponse>

    @GET("transaksi/detail/{orderId}")
    fun getTransaksiDetail(@Path("orderId") orderId: String): Call<TransaksiTiketResponse>

    @POST("transaksi/{orderId}/cancel")
    fun cancelTransaction(@Path("orderId") orderId: String): Call<ResponseBody>


    @POST("tiket/validate-partial")
    fun validateTickets(@Body request: TiketValidationRequest): Call<TiketValidationResponse>

    @PUT("profile")
    fun updateProfile(
        @Body profileData: ProfileUpdateRequest
    ): Call<ProfileUpdateResponse>

    @Streaming
    @GET("transaksi/export/monthly")
    fun downloadMonthlyReport(
        @Query("bulan") month: String,
        @Query("tahun") year: String,
        @Query("jenis") type: String
    ): Call<ResponseBody>

    @GET("dashboard/statistics")
    fun getDashboardStatistics(
        @Query("jenis") jenis: String
    ): Call<DashboardResponse>

}