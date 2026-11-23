package com.flanbross.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.flanbross.app.ui.FlanBrossTema
import com.flanbross.app.ui.Raiz

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlanBrossTema { Raiz() }
        }
    }
}
