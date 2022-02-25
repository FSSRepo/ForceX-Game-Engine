package com.fastsmartsystem.forcexcore

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to a native method
        val tv = findViewById<View>(R.id.sample_text) as TextView
        tv.setText("Hola mundo")

    }

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("forcex")
        }
    }
}
