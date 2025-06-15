package functions.cbsplit

fun getCROSystemMessage(messageType: String, feature: String): String {
    return when (feature) {
        "cbsplit" -> {
            when (messageType) {
                "welcome" -> "Welcome to CBSplit - Your comprehensive conversion rate optimization platform"
                "test_started" -> "CBSplit A-Z test has been started successfully"
                "test_stopped" -> "CBSplit test has been stopped and data saved"
                "conversion_tracked" -> "Conversion successfully tracked in CBSplit"
                "integration_connected" -> "CBSplit integration connected successfully"
                "pixel_fired" -> "CBSplit pixel tracking event fired"
                "error" -> "CBSplit system error occurred"
                else -> "CBSplit system message"
            }
        }
        else -> "Unknown feature: $feature"
    }
}