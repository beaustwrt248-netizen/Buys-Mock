package com.buysloans.hub

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Applies symmetric horizontal padding with independent top and bottom values.
 * This keeps the Scan Device capture controls clear of Android system navigation.
 */
internal fun Modifier.padding(horizontal: Dp, top: Dp, bottom: Dp): Modifier =
    padding(start = horizontal, top = top, end = horizontal, bottom = bottom)
