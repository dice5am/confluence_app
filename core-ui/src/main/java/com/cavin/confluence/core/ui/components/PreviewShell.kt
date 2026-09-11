package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cavin.confluence.core.ui.theme.ConfluenceColors

/** Preview / screenshot chrome: screen + 3-tab dock on void. */
@Composable
fun PreviewAppShell(
    selectedId: String,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = ConfluenceColors.Void,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            FloatingDock(
                items = ConfluenceDockItems,
                selectedId = selectedId,
                onSelect = {},
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            content()
        }
    }
}
