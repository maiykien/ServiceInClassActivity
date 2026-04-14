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
import android.view.Menu
import android.view.MenuItem
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
            // Refresh menu if binding happens after onCreateOptionsMenu
            invalidateOptionsMenu()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            timerBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind to the service when the activity is created
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val startPauseItem = menu?.findItem(R.id.action_start_pause)
        if (timerBinder?.isRunning == true) {
            startPauseItem?.title = "Pause"
        } else {
            startPauseItem?.title = "Start"
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_start_pause -> {
                if (isBound) {
                    if (timerBinder?.isRunning == true) {
                        timerBinder?.pause()
                    } else {
                        timerBinder?.start(100)
                    }
                    invalidateOptionsMenu()
                }
                true
            }
            R.id.action_stop -> {
                if (isBound) {
                    timerBinder?.stop()
                    invalidateOptionsMenu()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind from the service to avoid memory leaks
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
