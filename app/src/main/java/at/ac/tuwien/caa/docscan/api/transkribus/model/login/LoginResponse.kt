package at.ac.tuwien.caa.docscan.api.transkribus.model.login

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LoginResponse(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("firstname") val firstName: String,
    @SerializedName("lastname") val lastName: String,
    @SerializedName("userName") val userName: String,
    @SerializedName("email") val email: String,
)
