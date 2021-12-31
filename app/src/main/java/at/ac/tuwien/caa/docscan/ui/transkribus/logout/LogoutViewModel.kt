package at.ac.tuwien.caa.docscan.ui.transkribus.logout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LogoutViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.logout(initiatedByUser = true)
        }
    }
}
