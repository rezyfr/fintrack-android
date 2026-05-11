package com.fidriyanto.banktracker.categorization

object RuleBasedMatcher {
    private val rules = mapOf(
        "RATTIKAN LEKDARA" to "Bills",
        "METROPOLITAN ELECTRICITY" to "Bills",
        "TRUEMONEY CO" to "Bills",
        "TRUE MOBILE" to "Bills",
        "TRUEAPP" to "Bills",
        "FEE OTH BAK ATM" to "Bills",
        "MONTHLY CARD CHARGE" to "Bills",
        "ADMIN FEE" to "Bills",
        "BTS" to "Transport",
        "RED LINE" to "Transport",
        "SRT TICKET" to "Transport",
        "TRUE MONEY" to "Food & Drink",
        "TRUEMONEY" to "Food & Drink",
        "LINE PAY" to "Food & Drink",
        "SUSHIRO" to "Food & Drink",
        "SLIDE MORE PIZZA" to "Food & Drink",
        "MCDONALDS" to "Food & Drink",
        "CENTRAL RESTAURANTS" to "Food & Drink",
        "RISE COFFEE" to "Food & Drink",
        "URBAN EATS" to "Food & Drink",
        "FIVE STAR" to "Food & Drink",
        "RATTANA RESTAURANT" to "Food & Drink",
        "SWEET CLOUD" to "Food & Drink",
        "CP AXTRA" to "Groceries",
        "MAKRO" to "Groceries",
        "LOTUS" to "Groceries",
        "SUPER TURTLE" to "Groceries",
        "VENDING BY BOONTERM" to "Groceries",
        "SUN VENDING" to "Groceries",
        "SHOPEE" to "Shopping",
        "DECATHLON" to "Shopping",
        "CITY MALL" to "Shopping",
        "LITTLE BEE" to "Shopping",
        "LIFE POINT CHURCH" to "Entertainment",
        "SJ BARBER" to "Other",
        "CASH ATM" to "Other",
        "K PLUS WALLET" to "Other",
    )

    fun match(normalizedMerchant: String): String? {
        rules[normalizedMerchant]?.let { return it }
        return rules.entries.firstOrNull { (key, _) ->
            normalizedMerchant.contains(key)
        }?.value
    }
}
