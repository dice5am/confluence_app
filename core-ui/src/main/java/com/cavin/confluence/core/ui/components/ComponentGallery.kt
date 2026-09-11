package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceLayout
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.ConfluenceTypography
import com.cavin.confluence.core.ui.theme.confluenceScreenInner

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentGallery() {
    var selectedChip by remember { mutableStateOf(1) }
    var inputDefault by remember { mutableStateOf("Default") }
    var inputFocus by remember { mutableStateOf("Focus") }
    var inputError by remember { mutableStateOf("Error") }

    Box(
        Modifier
            .fillMaxSize()
            .background(ConfluenceColors.Void),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ConfluenceLayout.outerGutter)
                .confluenceScreenInner(
                    top = ConfluenceLayout.screenTop,
                    bottom = ConfluenceLayout.screenBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.stackGap),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap)) {
                Text(
                    "B1 · ICE + BRIGHT BLUE",
                    style = ConfluenceType.Eyebrow,
                    color = ConfluenceColors.Bloom,
                )
                Text(
                    "void · plasma · bloom · acrylic · bracket",
                    style = ConfluenceTypography.labelSmall,
                    color = ConfluenceColors.Muted,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap)) {
                Text("BUTTONS", style = ConfluenceType.Eyebrow, color = ConfluenceColors.Dim)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ConfluenceLayout.inlineGap),
                    verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.inlineGap),
                ) {
                    AppButton(onClick = {}, style = AppButtonStyle.Primary) { Text("Default") }
                    AppButton(onClick = {}, style = AppButtonStyle.Secondary) { Text("Pressed") }
                    AppButton(onClick = {}, style = AppButtonStyle.Ghost) { Text("Focus") }
                    AppButton(onClick = {}, style = AppButtonStyle.Primary, enabled = false) { Text("Disabled") }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap)) {
                Text("CHIPS", style = ConfluenceType.Eyebrow, color = ConfluenceColors.Dim)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ConfluenceLayout.inlineGap),
                    verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.inlineGap),
                ) {
                    AppChip("Default", selected = selectedChip == 0, onClick = { selectedChip = 0 })
                    AppChip("Selected", selected = selectedChip == 1, onClick = { selectedChip = 1 })
                    AppChip("Acrylic", selected = false, onClick = {}, accent = AppChipAccent.Acrylic)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap)) {
                Text("CARD / LAYER", style = ConfluenceType.Eyebrow, color = ConfluenceColors.Dim)
                GlassCard(glow = true, accentBorder = true) {
                    Text(
                        "Blue plasma face · accent brackets · acrylic under-layer",
                        style = ConfluenceTypography.titleMedium,
                        color = ConfluenceColors.Text,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap)) {
                Text("INPUTS", style = ConfluenceType.Eyebrow, color = ConfluenceColors.Dim)
                AppTextField(value = inputDefault, onValueChange = { inputDefault = it }, placeholder = "Default")
                AppTextField(value = inputFocus, onValueChange = { inputFocus = it })
                AppTextField(value = inputError, onValueChange = { inputError = it }, isError = true)
            }

            Column(verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap)) {
                Text("DOCK", style = ConfluenceType.Eyebrow, color = ConfluenceColors.Dim)
                FloatingDock(
                    items = ConfluenceDockItems,
                    selectedId = "home",
                    onSelect = {},
                    gutter = false,
                )
            }

            Disclaimer()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 860)
@Composable
internal fun ComponentGalleryPreview() {
    ConfluenceTheme {
        ComponentGallery()
    }
}
