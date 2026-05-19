package com.clearpath.xray_compose.ui.navigation.sharedviewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

@Composable
fun <T : Any> rememberSharedViewModelStoreNavEntryDecorator(
    viewModelStoreOwner: ViewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
): SharedViewModelStoreNavEntryDecorator<T> {
    val viewModelStoreProvider = rememberViewModelStoreProvider(viewModelStoreOwner)
    return remember(viewModelStoreOwner) {
        SharedViewModelStoreNavEntryDecorator(
            viewModelStoreProvider,
        )
    }
}

class SharedViewModelStoreNavEntryDecorator<T : Any>(
    viewModelStoreProvider: ViewModelStoreProvider
) : NavEntryDecorator<T>(
    onPop = { key -> viewModelStoreProvider.clearKey(key) },
    decorate = { entry ->
        val localContentKey = entry.contentKey
        val localOwner =
            rememberViewModelStoreOwner(
                localContentKey,
                viewModelStoreProvider,
                savedStateRegistryOwner = LocalSavedStateRegistryOwner.current,
            )

        val localValues: MutableList<ProvidedValue<*>> =
            mutableListOf(LocalViewModelStoreOwner provides localOwner)

        // If the entry indicates it has a parent, also provide its parent's ViewModelStore
        val parentContentKey = entry.metadata[ParentKey]
        if (parentContentKey != null) {
            val parentOwner = rememberViewModelStoreOwner(
                parentContentKey,
                viewModelStoreProvider,
                savedStateRegistryOwner = LocalSavedStateRegistryOwner.current,
            )

            localValues.add(LocalSharedViewModelStoreOwner provides parentOwner)
        }
        CompositionLocalProvider(
            values = localValues.toTypedArray()
        ) { entry.Content() }
    },
) {
    companion object {
        /**
         * Use this function to specify a `NavEntry`'s parent. The parent's
         * `ViewModelStoreOwner` will be supplied via `LocalSharedViewModelStoreOwner`
         */
        fun parent(key: Any) = metadata {
            put(ParentKey, key)
        }

        object ParentKey : NavMetadataKey<Any>
    }
}

val LocalSharedViewModelStoreOwner =
    staticCompositionLocalOf<ViewModelStoreOwner> { error("No LocalSharedViewModelStoreOwner provided!") }