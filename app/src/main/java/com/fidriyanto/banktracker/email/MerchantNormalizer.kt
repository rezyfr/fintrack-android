package com.fidriyanto.banktracker.email

object MerchantNormalizer {
    private val legalSuffixes = listOf(
        "PUBLIC COMPANY LIMITED", "COMPANY LIMITED",
        "CO., LTD.", "CO.,LTD.", "CO LTD",
        "PCL.", "PCL", "PLC.", "PLC", "LTD.", "LTD"
    )

    fun normalize(raw: String): String {
        var result = raw.uppercase().trim()
        for (suffix in legalSuffixes) {
            if (result.endsWith(suffix)) {
                result = result.dropLast(suffix.length).trimEnd(',').trim()
                break
            }
        }
        return result.replace("'", "").replace(".", "").trim()
    }
}
