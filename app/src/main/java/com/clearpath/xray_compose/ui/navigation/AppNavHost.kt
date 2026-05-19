package com.clearpath.xray_compose.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.clearpath.xray_compose.ui.navigation.bottomsheet.BottomSheetSceneStrategy
import com.clearpath.xray_compose.ui.screen.LocalRootInnerPadding
import com.clearpath.xray_compose.ui.theme.AppAnimation

@Composable
fun AppNavHost() {
    val navigationState = rememberNavigationState(
        startRoute = Home,
        topLevelRoutes = TOP_LEVEL_ROUTES.keys
    )
    val navigator = remember { Navigator(navigationState) }
    val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }

    val entryProvider = entryProvider {
        homeSection()
        profileSection()
        settingsSection()
    }

    fun isTabRoot(sceneKey: Any?): Boolean {
        // NOTE: toString is necessary!
        return TOP_LEVEL_ROUTES.keys.any { it.toString() == sceneKey }
    }

    fun tabRootIndex(sceneKey: Any?): Int {
        return TOP_LEVEL_ROUTES.keys.indexOfFirst { it.toString() == sceneKey }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
        val showBottomBar = currentStack?.size == 1
        val layoutDirection = LocalLayoutDirection.current

        Scaffold { innerPadding ->
            val paddingDecorator = remember(innerPadding, layoutDirection) {
                object : NavEntryDecorator<NavKey>(
                    decorate = { entry ->
                        val padding = remember(innerPadding, layoutDirection) {
                            PaddingValues(
                                start = innerPadding.calculateStartPadding(layoutDirection),
                                top = innerPadding.calculateTopPadding(),
                                end = innerPadding.calculateEndPadding(layoutDirection),
                                bottom = if (isTabRoot(entry.contentKey)) {
                                    innerPadding.calculateBottomPadding() + 80.dp
                                } else {
                                    innerPadding.calculateBottomPadding()
                                }
                            )
                        }
                        CompositionLocalProvider(LocalRootInnerPadding provides padding) {
                            entry.Content()
                        }
                    }
                ) {}
            }

            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    entries = navigationState.toDecoratedEntries(
                        entryProvider = entryProvider,
                        extraDecorators = listOf(paddingDecorator)
                    ),
                    sceneStrategies = listOf(bottomSheetStrategy),
                    onBack = { navigator.goBack() },
                    transitionSpec = {
                        // val isTabSwitch = isTabRoot(initialState.key) && isTabRoot(targetState.key)
                        // if (isTabSwitch) AppAnimation.DefaultTransition else AppAnimation.ForwardTransition
                        val initialTabIndex = tabRootIndex(initialState.key)
                        val targetTabIndex = tabRootIndex(targetState.key)
                        if (initialTabIndex == -1 || targetTabIndex == -1) {
                            AppAnimation.ForwardTransition
                        } else if (initialTabIndex < targetTabIndex) {
                            AppAnimation.ForwardTransition
                        } else {
                            AppAnimation.PopTransition
                        }
                    },
                    popTransitionSpec = {
                        // val isTabSwitch = isTabRoot(initialState.key) && isTabRoot(targetState.key)
                        // if (isTabSwitch) AppAnimation.DefaultTransition else AppAnimation.PopTransition
                        val initialTabIndex = tabRootIndex(initialState.key)
                        val targetTabIndex = tabRootIndex(targetState.key)
                        if (initialTabIndex == -1 || targetTabIndex == -1) {
                            AppAnimation.PopTransition
                        } else if (initialTabIndex < targetTabIndex) {
                            AppAnimation.ForwardTransition
                        } else {
                            AppAnimation.PopTransition
                        }
                    },
                    predictivePopTransitionSpec = { AppAnimation.PopTransition }
                )

                AnimatedVisibility(
                    visible = showBottomBar,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(AppAnimation.spec()) + slideInVertically(AppAnimation.spec()) { it },
                    exit = fadeOut(AppAnimation.spec()) + slideOutVertically(AppAnimation.spec()) { it }
                ) {
                    NavigationBar {
                        TOP_LEVEL_ROUTES.forEach { (key, value) ->
                            val isSelected = key == navigationState.topLevelRoute
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { navigator.navigate(key) },
                                icon = {
                                    Icon(
                                        painter = painterResource(value.icon),
                                        contentDescription = value.description
                                    )
                                },
                                label = { Text(value.description) }
                            )
                        }
                    }
                }
            }
        }
    }
}
