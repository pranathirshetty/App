package to.kuudere.anisuge.utils

fun Throwable.isNetworkError(): Boolean {
    val causeChain = generateSequence(this) { it.cause }.toList()
    return causeChain.any {
        it is java.net.UnknownHostException ||
        it is java.net.ConnectException ||
        it is java.net.SocketTimeoutException ||
        it is java.net.NoRouteToHostException ||
        (it.message?.contains("Failed to connect") == true) ||
        (it.message?.contains("Unable to resolve host") == true) ||
        (it.message?.contains("No route to host") == true)
    }
}
