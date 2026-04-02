package edu.temple.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private var timerBinder: TimerService.TimerBinder? = null
    private var isBound = false

    // Handler to receive updates from the service
    private val handler = Handler(Looper.getMainLooper()) { msg ->
        findViewById<TextView>(R.id.textView).text = msg.what.toString()
        true
    }

    // Connection object to manage the service lifecycle
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            timerBinder = service as TimerService.TimerBinder
            timerBinder?.setHandler(handler)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            timerBinder = null
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)


        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        startButton.setOnClickListener {
            if (isBound) {
                if (timerBinder?.isRunning == true) {
                    timerBinder?.pause()
                    startButton.text = "Start"
                } else {
                    timerBinder?.start(100)
                    startButton.text = "Pause"
                }
            }

        }


        stopButton.setOnClickListener{
            if (isBound) {
                timerBinder?.stop()
                startButton.text = "Start"
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }


}



