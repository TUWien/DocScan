package at.ac.tuwien.caa.docscan.sync.transkribus.model.login

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LoginResponse(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("firstname") val firstName: String,
    @SerializedName("lastName") val lastName: String,
)
