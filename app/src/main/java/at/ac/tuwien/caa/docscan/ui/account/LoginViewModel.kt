package at.ac.tuwien.caa.docscan.ui.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.repository.UserRepository
import at.ac.tuwien.caa.docscan.api.transkribus.model.login.LoginResponse
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val observableLogin: MutableLiveData<Resource<LoginResponse>> = MutableLiveData()
    val observableProgress: MutableLiveData<Boolean> = MutableLiveData()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            observableProgress.postValue(true)
            observableLogin.postValue(userRepository.login(username, password))
            observableProgress.postValue(false)
        }
    }
}
