package at.ac.tuwien.caa.docscan.sync.transkribus

import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author matej bartalsky
 */
class TranskribusHeaderInterceptor constructor(
    private val preferencesHandler: PreferencesHandler
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()
        // accepts responses only in json format, otherwise the API would return an XML
        requestBuilder.addHeader("Accept", "application/json")
        // appends cookie for authorization, if the cookie is not available, then this request
        // will return a http 401.
        preferencesHandler.transkribusCookie?.let {
            requestBuilder.addHeader("Cookie", "JSESSIONID=$it")
        }
        return chain.proceed(requestBuilder.build())
    }
}
