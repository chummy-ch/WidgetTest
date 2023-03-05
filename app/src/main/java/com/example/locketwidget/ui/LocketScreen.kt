package com.example.locketwidget.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.example.locketwidget.data.Event
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

@Composable
fun LocketScreen(
    systemUIColor: Color = MaterialTheme.colors.background,
    backgroundColor: Color = MaterialTheme.colors.background,
    message: Event.ErrorMessage? = null,
    disposeEvent: () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    content: @Composable (padding: PaddingValues) -> Unit
) {
    val systemUiController = rememberSystemUiController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(systemUIColor) {
        systemUiController.setStatusBarColor(
            color = systemUIColor
        )
        systemUiController.setNavigationBarColor(systemUIColor)
    }

    DisposableEffect(message) {
        message?.message?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
        onDispose(disposeEvent)
    }

    Scaffold(
        topBar = topBar,
        backgroundColor = backgroundColor,
        snackbarHost = { SnackbarHost(modifier = Modifier.zIndex(2f), hostState = snackbarHostState) }
    ) { paddingValues ->
        content(paddingValues)
    }
}