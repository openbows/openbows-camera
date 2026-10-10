package com.openbows.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.openbows.camera.ui.OpenbowsRoot
import com.openbows.camera.ui.theme.OpenbowsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenbowsTheme {
                OpenbowsRoot()
            }
        }
    }
}
