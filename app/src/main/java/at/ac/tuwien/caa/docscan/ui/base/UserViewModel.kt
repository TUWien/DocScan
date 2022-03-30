package at.ac.tuwien.caa.docscan.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.User
import at.ac.tuwien.caa.docscan.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    val observableUser = MutableLiveData<User?>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getUser().collectLatest {
                observableUser.postValue(it)
            }
        }
    }
}
