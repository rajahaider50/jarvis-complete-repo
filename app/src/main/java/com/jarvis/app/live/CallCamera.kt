package com.jarvis.app.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The live call camera, owned by the app (not the screen) so both the UI and the assistant can switch it.
 * mode: 0 off, 1 front, 2 back.
 */
object CallCamera {
    var mode by mutableStateOf(0)
}
