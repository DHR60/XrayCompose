package com.clearpath.xray_compose.core

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.enums.EngineState
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

object NetworkManager {
    val directClient = HttpClient(OkHttp)
    val proxyClientFlow = AppState.proxyPort
        .map {
            HttpClient(OkHttp) {
                engine {
                    config {
                        proxy = ProxyBuilder.http("http://127.0.0.1:$it")
                    }
                }
            }
        }
        .buffer(0)
        .stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.Eagerly,
            initialValue = HttpClient(OkHttp) {
                engine {
                    config {
                        proxy =
                            ProxyBuilder.http("http://127.0.0.1:${GlobalConst.defaultSocksPort}")
                    }
                }
            }
        )

    suspend inline fun <reified T> safeDirectRequest(
        crossinline block: suspend HttpClient.() -> T
    ): Result<T> {
        return try {
            Result.success(directClient.block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend inline fun <reified T> safeProxyRequest(
        crossinline block: suspend HttpClient.() -> T
    ): Result<T> {
        return try {
            Result.success(proxyClientFlow.value.block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend inline fun <reified T> safeProxyRequestWithFallback(
        crossinline block: suspend HttpClient.() -> T
    ): Result<T> {
        val firstAttempt = safeProxyRequest(block)
        return if (firstAttempt.isSuccess) {
            firstAttempt
        } else {
            safeDirectRequest(block)
        }
    }

    suspend inline fun <reified T> safeRequestAuto(
        crossinline block: suspend HttpClient.() -> T
    ): Result<T> {
        return if (AppState.engineStateFlow.value == EngineState.STARTED) {
            safeProxyRequest(block)
        } else {
            safeProxyRequestWithFallback(block)
        }
    }
}