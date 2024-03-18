package org.example.authorization_feature

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class GuuTokenServiceImpl() : GuuTokensService {
    @Throws(Exception::class)
    override fun extractCsrfTokens(htmlPage: String, result: (csrf: String, csrfToken: String) -> Unit) {
        val doc = Jsoup.parse(htmlPage)

        val csrf = doc.selectFirst("meta[name=csrf-token]")?.attr("content")

        val csrfToken = doc.selectFirst("meta[name=csrf-token-value]")?.attr("content")

        if (csrf.isNullOrEmpty() || csrfToken.isNullOrEmpty()) throw Exception("Failed to parse document")

        result.invoke(csrf, csrfToken)
    }
}