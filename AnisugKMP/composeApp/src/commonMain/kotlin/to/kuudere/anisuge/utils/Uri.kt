package to.kuudere.anisuge.utils

object Uri {
    fun parseQueryParam(url: String, key: String): String? {
        val queryString = url.substringAfter("?", "")
        if (queryString.isEmpty()) return null
        
        return queryString.split("&")
            .map { it.split("=") }
            .firstOrNull { it.size == 2 && it[0] == key }
            ?.get(1)
    }
}
