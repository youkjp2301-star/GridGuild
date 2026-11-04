package com.gridguild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.gridguild.ui.GridGuildApp

class MainActivity : ComponentActivity() {
    private val model by viewModels<GridViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GridGuildApp(model) }
    }
    override fun onStart() { super.onStart();model.resume() }
    override fun onStop() { model.pause();super.onStop() }
}
