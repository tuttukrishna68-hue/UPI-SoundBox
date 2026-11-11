package com.upisoundbox.core.parser

class ParserRouter(
    private val strategies: Map<String, ParserStrategy> = defaultStrategies,
    private val genericFallback: ParserStrategy = GenericFallbackParser()
) {
    private val packageToParserMap = mutableMapOf<String, ParserStrategy>().apply {
        putAll(strategies)
    }

    private val allowList = mutableMapOf<String, UpiAppConfig>()

    init {
        // Seed default known mappings
        register("com.google.android.apps.nbu.paisa.user", GooglePayParser())
        register("com.google.android.apps.nbu.paisa.merchant", GooglePayBusinessParser())
        register("com.phonepe.app", PhonePeParser())
        register("com.phonepe.app.business", PhonePeBusinessParser())
        register("com.phonepe.business", PhonePeBusinessParser())
        register("net.one97.paytm", PaytmParser())
        register("net.one97.paytm.merchant", PaytmBusinessParser())
        register("com.paytm.business", PaytmBusinessParser())
        register("in.org.npci.upiapp", BhimParser())
        register("in.amazon.mShop.android.shopping", AmazonPayParser())
        register("com.dreamplug.androidapp", CredParser())
    }

    fun register(packageName: String, strategy: ParserStrategy) {
        packageToParserMap[packageName] = strategy
    }

    fun loadAllowList(apps: List<UpiAppConfig>) {
        allowList.clear()
        for (app in apps) {
            allowList[app.packageId] = app
        }
    }

    fun isAllowed(packageName: String): Boolean {
        val config = allowList[packageName]
        return config?.enabled ?: packageToParserMap.containsKey(packageName)
    }

    fun parse(notification: RawNotification): ParseResult {
        val strategy = packageToParserMap[notification.packageName] ?: genericFallback
        return strategy.parse(notification)
    }

    companion object {
        val defaultStrategies: Map<String, ParserStrategy> = mapOf(
            "google_pay" to GooglePayParser(),
            "google_pay_business" to GooglePayBusinessParser(),
            "phonepe" to PhonePeParser(),
            "phonepe_business" to PhonePeBusinessParser(),
            "paytm" to PaytmParser(),
            "paytm_business" to PaytmBusinessParser(),
            "bhim" to BhimParser(),
            "amazon_pay" to AmazonPayParser(),
            "cred" to CredParser(),
            "generic" to GenericFallbackParser()
        )
    }
}

