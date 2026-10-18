package com.nxredux.mobile.achievements

/** Gap between two dorequest.php calls of a sync or prefetch; the server rate-limits clients that hammer it. */
const val RA_PACE_MS = 250L
/** The one wait before retrying a busy answer (a 429, a 5xx, no RA JSON); a second busy answer stops the run. */
const val RA_BACKOFF_MS = 5_000L

/**
 * Paces a run's requests [paceMs] apart (the first goes at once) and retries a busy answer once after [backoffMs].
 * [post] returns null when the server is still busy: the caller stops with a "try later" result. A transport failure
 * ([RaHttpException]) is not retried; it propagates. [delay] is injectable so tests run without waiting.
 */
class RaPacer(
    private val http: RaHttp, private val delay: suspend (Long) -> Unit,
    private val paceMs: Long = RA_PACE_MS, private val backoffMs: Long = RA_BACKOFF_MS,
) {
    private var sent = false

    /** [form] POSTed to [url] (the signed award carries its own); null = still busy after the one retry. */
    suspend fun post(form: String, url: String = RaRequests.URL): RaResponse? {
        if (sent) delay(paceMs)
        sent = true
        http.post(url, form, RaRequests.CONTENT_TYPE).takeIf(::definitive)?.let { return it }
        delay(backoffMs)
        return http.post(url, form, RaRequests.CONTENT_TYPE).takeIf(::definitive)
    }
}

/** RA's answer that the saved login is no good (rcheevos' RC_INVALID_CREDENTIALS / RC_EXPIRED_TOKEN): sign in again. */
internal fun authRefused(response: RaResponse): Boolean {
    if (response.status == 401) return true
    val code = RaJson.parse(RaErrorBody.serializer(), response.body)?.Code
    return code == "invalid_credentials" || code == "expired_token"
}

/** RAWeb's account- or client-wide refusals (`access_denied` on an unverified account, `unsupported_client`): no request will pass. */
internal fun accountRefused(response: RaResponse): Boolean {
    val code = RaJson.parse(RaErrorBody.serializer(), response.body)?.Code
    return code == "access_denied" || code == "unsupported_client"
}
