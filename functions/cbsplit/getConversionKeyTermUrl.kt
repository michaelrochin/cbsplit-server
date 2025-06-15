package functions.cbsplit

fun getConversionKeyTermUrl(feature: String, variant: String, keyTerm: String): String {
    return when (feature) {
        "cbsplit" -> {
            val baseUrl = "https://cbsplit.com/track"
            "$baseUrl?feature=$feature&variant=$variant&term=$keyTerm"
        }
        else -> ""
    }
}