package com.clearpath.xray_compose.service.formatter.model

import com.clearpath.xray_compose.utils.Utils
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.util.StringValuesBuilder

class EncodedParameters(
    private val inner: ParametersBuilder = ParametersBuilder()
) : ParametersBuilder by inner {
    companion object {
        fun buildFromURLBuilder(urlBuilder: URLBuilder): EncodedParameters {
            return EncodedParameters(urlBuilder.encodedParameters)
        }
    }

    fun appendEncoded(name: String, value: String) {
        inner.append(name, value)
    }

    fun encodeAndAppend(name: String, value: String) {
        val encoded = Utils.encodeURIComponent(value)
        inner.append(name, encoded)
    }

    fun encodeAndAppendIfNotEmpty(name: String?, value: String?) {
        if (name.isNullOrEmpty()) {
            return
        }
        if (value.isNullOrEmpty()) {
            return
        }
        encodeAndAppend(name, value)
    }

    fun getOrEmptyEncoded(name: String): String {
        return inner[name]?.let { Utils.encodeURIComponent(it) } ?: ""
    }

    fun getAllOrEmptyEncoded(name: String): List<String> {
        return inner.getAll(name)?.map { Utils.encodeURIComponent(it) } ?: emptyList()
    }

    fun buildQuery(): String {
        val builder = StringBuilder()
        for (key in names()) {
            for (value in getAll(key) ?: emptyList()) {
                if (builder.isNotEmpty()) {
                    builder.append("&")
                }
                builder.append(key)
                builder.append("=")
                builder.append(value)
            }
        }
        return builder.toString()
    }

    fun toDecodedParameters(): ParametersBuilder {
        val decoded = ParametersBuilder()
        for (key in inner.names()) {
            for (value in inner.getAll(key) ?: emptyList()) {
                decoded.append(key, Utils.decodeURIComponent(value))
            }
        }
        return decoded
    }
}

fun StringValuesBuilder.getOrEmpty(name: String): String {
    return this[name] ?: ""
}

fun StringValuesBuilder.getAllOrEmpty(name: String): List<String> {
    return this.getAll(name) ?: emptyList()
}
