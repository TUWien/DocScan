package at.ac.tuwien.caa.docscan.sync.transkribus

import at.ac.tuwien.caa.docscan.sync.transkribus.model.login.LoginResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Represents the API for the upload of documents.
 *
 * @author matejbart
 */
interface TranskribusAPIService {
    companion object {
        const val BASE_URL = "https://transkribus.eu/TrpServer/rest/"

        const val PARAM_USERNAME = "un"
        const val PARAM_DIVA = "diva"
        const val PARAM_ACTIVATE_TRAFFIC_INFO = "activateTrafficInfo"

        const val PARAM_RELATED_STOP = "relatedStop"
        const val PARAM_RELATED_LINE = "relatedLine"

        const val PARAM_USER = "user"
        const val PARAM_PW = "pw"
    }

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field(PARAM_USER) user: String,
        @Field(PARAM_PW) pw: String
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("auth/logout")
    suspend fun logout(): Response<Void>
}
