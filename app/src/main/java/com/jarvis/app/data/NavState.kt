package com.jarvis.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The screen the app is showing right now, so the assistant knows where the user is (get_app_state). */
object NavState {
    var route by mutableStateOf("home")
}
