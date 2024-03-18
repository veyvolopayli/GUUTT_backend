package org.example.authorization_feature

import org.jsoup.nodes.Document

interface GuuTokensService {
    fun extractCsrfTokens(htmlPage: String, result: (csrf: String, csrfToken: String) -> Unit)
}