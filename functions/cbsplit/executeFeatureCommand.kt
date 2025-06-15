package functions.cbsplit

fun executeFeatureCommand(command: String, params: Map<String, Any>): String {
    return when (command) {
        "cbsplit" -> {
            val integration = CBSplitIntegration()
            integration.execute(params)
            "CBSplit command executed successfully"
        }
        else -> "Unknown command: $command"
    }
}