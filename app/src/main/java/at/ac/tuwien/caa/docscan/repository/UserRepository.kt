package at.ac.tuwien.caa.docscan.repository

import android.content.Context
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.api.transkribus.TranskribusAPIService
import at.ac.tuwien.caa.docscan.api.transkribus.model.login.LoginResponse
import at.ac.tuwien.caa.docscan.db.dao.UserDao
import at.ac.tuwien.caa.docscan.db.model.User
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.logic.notification.NotificationHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepository(
    private val context: Context,
    private val userDao: UserDao,
    private val api: TranskribusAPIService,
    private val preferencesHandler: PreferencesHandler,
    private val notificationHandler: NotificationHandler
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
                    logout()
                }
                resource
            }
            is Success -> {
                resource
            }
        }
    }

    /**
     * Forces a logout for the user.
     * - Tries to perform a logout request.
     * - Stops all uploads.
     * - Clears the user session.
     */
    suspend fun logout(initiatedByUser: Boolean = false) {
        return withContext(NonCancellable) {
            // logout response doesn't matter
            transkribusResource<Void, Void>(apiCall = { api.logout() })
            // clear the user
            clearUser()

            if (!initiatedByUser) {
                notificationHandler.showLogoutNotification(
                    context.getString(R.string.notification_session_expired_title),
                    context.getString(R.string.notification_session_expired_text)
                )
            }
        }
    }

    suspend fun checkLogin(initiatedByUser: Boolean = true): Resource<LoginResponse> {
        return withContext(NonCancellable) {
            return@withContext when (val credentials = checkCredentials(initiatedByUser)) {
                is Failure -> {
                    Failure(credentials.exception)
                }
                is Success -> {
                    login(username = credentials.data.first, password = credentials.data.second)
                }
            }
        }
    }

    private suspend fun checkCredentials(initiatedByUser: Boolean): Resource<Pair<String, String>> {
        val username = userDao.getUser()?.userName
        val password = preferencesHandler.transkribusPassword
        if (username != null && password != null) {
            return Success(Pair(username, password))
        }
        logout(initiatedByUser)
        return asUnauthorizedFailure()
    }

    private suspend fun clearUser() {
        preferencesHandler.transkribusCookie = null
        userDao.deleteUser()
    }
}
