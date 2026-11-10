package com.fundory.app.domain.engine

import java.util.regex.Pattern

data class SharedProductInfo(
    val url: String,
    val marketplace: String,
    val title: String,
    val suggestedCurrency: String
)

object ShareLinkParser {

    private val URL_PATTERN = Pattern.compile("https?://[a-zA-Z0-9.-]+(?:/[^\\s]*)?")

    fun parse(sharedText: String?): SharedProductInfo? {
        if (sharedText.isNullOrBlank()) return null

        val matcher = URL_PATTERN.matcher(sharedText)
        if (!matcher.find()) return null

        val url = matcher.group()
        val lowerUrl = url.lowercase()

        val marketplace: String
        val suggestedCurrency: String

        when {
            lowerUrl.contains("yangkeduo.com") || lowerUrl.contains("pinduoduo.com") || lowerUrl.contains("pdd.com") || sharedText.contains("拼多多") -> {
                marketplace = "Pinduoduo"
                suggestedCurrency = "CNY"
            }
            lowerUrl.contains("taobao.com") || lowerUrl.contains("tb.cn") || sharedText.contains("淘宝") -> {
                marketplace = "Taobao"
                suggestedCurrency = "CNY"
            }
            lowerUrl.contains("tmall.com") || lowerUrl.contains("tmall.hk") || sharedText.contains("天猫") -> {
                marketplace = "Tmall"
                suggestedCurrency = "CNY"
            }
            lowerUrl.contains("1688.com") || sharedText.contains("1688") || sharedText.contains("阿里巴巴") -> {
                marketplace = "1688"
                suggestedCurrency = "CNY"
            }
            lowerUrl.contains("jd.com") || lowerUrl.contains("jd.hk") || sharedText.contains("京东") -> {
                marketplace = "JD.com"
                suggestedCurrency = "CNY"
            }
            lowerUrl.contains("aliexpress.com") || lowerUrl.contains("aliexpress.ru") || sharedText.contains("速卖通") -> {
                marketplace = "AliExpress"
                suggestedCurrency = "USD"
            }
            else -> {
                marketplace = "Other Store"
                suggestedCurrency = "USD"
            }
        }

        // Extract surrounding text as title
        val textWithoutUrl = sharedText.replace(url, "").trim()
        val cleanedTitle = textWithoutUrl
            .replace(Regex("【.*?】"), "") // Remove Chinese brackets
            .replace(Regex("\\[.*?\\]"), "")
            .trim()
            .take(120)
            .ifBlank { "Imported Product" }

        return SharedProductInfo(
            url = url,
            marketplace = marketplace,
            title = cleanedTitle,
            suggestedCurrency = suggestedCurrency
        )
    }
}

