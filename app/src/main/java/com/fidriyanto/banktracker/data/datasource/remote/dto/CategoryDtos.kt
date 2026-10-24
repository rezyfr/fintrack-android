package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class CategoryDto(
    @SerializedName("name") val name: String,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    @SerializedName("is_default") val isDefault: Boolean = false,
)

data class SubcategoryDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("category") val category: String,
    @SerializedName("name") val name: String,
    @SerializedName("sort_order") val sortOrder: Int = 0,
)
