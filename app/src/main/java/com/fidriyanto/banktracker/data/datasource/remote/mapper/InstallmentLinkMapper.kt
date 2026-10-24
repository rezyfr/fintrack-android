package com.fidriyanto.banktracker.data.datasource.remote.mapper

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

// ac: installment-link-edit — the PATCH body that removes a transaction's installment link
fun clearInstallmentBody(): RequestBody = """{"installment_id":null}""".toRequestBody("application/json".toMediaType())
