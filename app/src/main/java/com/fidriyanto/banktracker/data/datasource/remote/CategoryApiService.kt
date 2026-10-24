package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.SubcategoryDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface CategoryApiService {
    @GET("v1/categories")
    suspend fun getCategories(): List<CategoryDto>

    @PUT("v1/categories")
    suspend fun putCategory(@Body category: CategoryDto): Response<ResponseBody>

    @DELETE("v1/categories")
    suspend fun deleteCategory(@Query("name") name: String): Response<ResponseBody>

    @GET("v1/subcategories")
    suspend fun getSubcategories(): List<SubcategoryDto>

    @POST("v1/subcategories")
    suspend fun insertSubcategory(@Body subcategory: SubcategoryDto): Response<ResponseBody>

    @DELETE("v1/subcategories")
    suspend fun deleteSubcategory(
        @Query("category") category: String,
        @Query("name") name: String,
    ): Response<ResponseBody>
}
