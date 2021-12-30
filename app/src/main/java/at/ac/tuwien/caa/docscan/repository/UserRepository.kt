package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.db.dao.UserDao
import at.ac.tuwien.caa.docscan.db.model.User
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.sync.transkribus.TranskribusAPIService
import at.ac.tuwien.caa.docscan.sync.transkribus.model.login.LoginResponse
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepository(
    private val userDao: UserDao,
    private val api: TranskribusAPIService,
    private val preferencesHandler: PreferencesHandler
) {

    fun getUser(): Flow<User?> {
        return userDao.getUserAsFlow()
    }

    suspend fun login(username: String, password: String): Resource<LoginResponse> {
        val resource: Resource<LoginResponse> = transkribusResource(
            apiCall = { api.login(username, password) },
            persistNetworkResponse = {
                // save the user, and the session data
                userDao.insertUser(
                    User(
                        firstName = it.firstName,
                        lastName = it.lastName,
                        userName = it.userName
                    )
                )
                preferencesHandler.transkribusCookie = it.sessionId
                preferencesHandler.transkribusPassword = password
            }
        )
        return when (resource) {
            is Failure -> {
                if (resource.exception.is401() || resource.exception.is403()) {
                    clearUser()
                }
                resource
            }
            is Success -> {
                resource
            }
        }
    }

    suspend fun logout() {
        return withContext(NonCancellable) {
            // logout response doesn't matter
            transkribusResource<Void, Void>(apiCall = { api.logout() })
            clearUser()
        }
    }

    // TODO: This could also check the login via the sessionId
    suspend fun checkLogin(): Resource<LoginResponse> {
        return withContext(NonCancellable) {
            return@withContext when (val credentials = checkCredentials()) {
                is Failure -> {
                    Failure(credentials.exception)
                }
                is Success -> {
                    login(username = credentials.data.first, password = credentials.data.second)
                }
            }
        }
    }

    private suspend fun checkCredentials(): Resource<Pair<String, String>> {
        val username = userDao.getUser()?.userName
        val password = preferencesHandler.transkribusPassword
        if (username != null && password != null) {
            return Success(Pair(username, password))
        }
        clearUser()
        return asUnauthorizedFailure()
    }

    private suspend fun clearUser() {
        preferencesHandler.transkribusCookie = null
        userDao.deleteUser()
    }
}
