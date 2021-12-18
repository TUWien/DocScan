package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.sync.transkribus.TranskribusAPIService
import at.ac.tuwien.caa.docscan.sync.transkribus.model.login.LoginResponse
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class UserRepository(
    private val api: TranskribusAPIService,
    private val preferencesHandler: PreferencesHandler
) {
    suspend fun login(username: String, password: String): Resource<LoginResponse> {
        return transkribusResource(
            apiCall = { api.login(username, password) },
            persistNetworkResponse = {
                // TODO: Save entire login response object
                preferencesHandler.transkribusCookie = it.sessionId
            }
        )
    }

    suspend fun logout(): Resource<Void> {
        return withContext(NonCancellable) {
            val resource = transkribusResource<Void, Void>(
                apiCall = { api.logout() }
            )
            // TODO: Clear all session related stuff
            preferencesHandler.transkribusCookie = null
            Success(null as Void)
        }
    }
}
