package com.example.huaweiproject.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.huaweiproject.data.Assistant
import com.example.huaweiproject.data.AssistantDao
import kotlinx.coroutines.*

class AssistantViewModel(private val database: AssistantDao, application: Application):
    AndroidViewModel(application) {

    private var viewModeJob = Job()

    override fun onCleared() {
        super.onCleared()
        viewModeJob.cancel()
    }

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModeJob)
    private var currentMessage = MutableLiveData<Assistant?>()
    val messages = database.getAllMessages()

    init {
        initializeCurrentMessage()
    }

    private fun initializeCurrentMessage() {
        uiScope.launch { currentMessage.value = getCurrentMessageFromDatabase() }
    }

    private suspend fun getCurrentMessageFromDatabase(): Assistant? {
        return  withContext(Dispatchers.IO){
            var message = database.getCurrentMessage()
            if (message?.assistantMessage == "DEFAULT_MESSAGE" ||
                message?.humanMessage == "DEFAULT_MESSAGE"){
                message = null
            }
            message
        }
    }

    fun sendMessageToDataBase(assistantMessage: String, humanMessage: String){
        uiScope.launch {
            val newAssistant = Assistant()
            newAssistant.assistantMessage = assistantMessage
            newAssistant.humanMessage = humanMessage
            insert(newAssistant)
            currentMessage.value = getCurrentMessageFromDatabase()
        }
    }

    private suspend fun insert(message: Assistant){
        withContext(Dispatchers.IO){
            database.insert(message)
        }
    }

    private suspend fun update(message: Assistant){
        withContext(Dispatchers.IO){
            database.update(message)
        }
    }

    fun onClear(){
        uiScope.launch {
            clear()
            currentMessage.value = null
        }
    }

    private suspend fun clear(){
        withContext(Dispatchers.IO){
            database.clear()
        }
    }
}