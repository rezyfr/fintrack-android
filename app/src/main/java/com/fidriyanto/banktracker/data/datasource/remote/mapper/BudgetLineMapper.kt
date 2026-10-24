package com.fidriyanto.banktracker.data.datasource.remote.mapper

import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

private fun JsonObject.json(): RequestBody = toString().toRequestBody("application/json".toMediaType())

// The POST body of a new line set up by category
fun BudgetLine.toCreateBody(): RequestBody = JsonObject().apply {
    addProperty("name", name)
    addProperty("kind", kind)
    addProperty("currency", currency)
    addProperty("target", target)
    addProperty("sort_order", sortOrder)
    addProperty("active", true)
    category?.let { addProperty("category", it) }
    subcategory?.let { addProperty("subcategory", it) }
    dueDay?.let { addProperty("due_day", it) }
}.json()

// The PATCH body of an edited line. A cleared due_day or subcategory is sent as null. A line that uses
// match rules sends no category fields and no match fields, so its text rule stays as it is.
fun BudgetLine.toUpdateBody(): RequestBody = JsonObject().apply {
    addProperty("name", name)
    addProperty("kind", kind)
    addProperty("currency", currency)
    addProperty("target", target)
    add("due_day", dueDay?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE)
    if (category != null) {
        addProperty("category", category)
        add("subcategory", subcategory?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE)
    }
}.json()
