package com.clearpath.xray_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.GlobalConst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader

class LogcatViewModel(application: Application) : AndroidViewModel(application) {
    private val _logcatSourceFlow: Flow<String> = flow {
        var process: Process? = null
        try {
            process =
                ProcessBuilder(
                    listOf(
                        "logcat",
                        "-T", "2048",
                        // "-v", "tag,color",
                        "-v", "tag",
                        "-s", arrayOf(
                            "GoLog",
                            GlobalConst.appId,
                            "AndroidRuntime",
                            "System.err",
                        ).joinToString(",")
                    )
                ).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            while (currentCoroutineContext().isActive) {
                val line = reader.readLine() ?: break
                emit(line)
            }
        } catch (e: Exception) {
            emit("Logcat Error: ${e.localizedMessage}")
        } finally {
            process?.destroy()
        }
    }.flowOn(Dispatchers.IO)

    val logMessages: StateFlow<List<String>> = _logcatSourceFlow
        .scan(emptyList<String>()) { accumulator, value ->
            (accumulator + value).takeLast(2048)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}