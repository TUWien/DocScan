package at.ac.tuwien.caa.docscan.ui.info

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LogViewModel(
    private val fileHandler: FileHandler
) : ViewModel() {

    val observableProgress = MutableLiveData<Boolean>()
    val observableError = MutableLiveData<Event<Throwable>>()
    val observableShareUris = MutableLiveData<Event<Uri>>()

    fun shareLog() {
        viewModelScope.launch(Dispatchers.IO) {
            observableProgress.postValue(true)
            when (val result = fileHandler.exportLogAsZip()) {
                is Failure -> {
                    observableError.postValue(Event(result.exception))
                }
                is Success -> {
                    observableShareUris.postValue(Event(result.data))
                }
            }
            observableProgress.postValue(false)
        }
    }
}
