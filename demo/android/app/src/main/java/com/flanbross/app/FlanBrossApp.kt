package com.flanbross.app

import android.app.Application
import com.flanbross.app.data.Servicios

class FlanBrossApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Servicios.iniciar(this)
    }
}
