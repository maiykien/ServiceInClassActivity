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

private const val PREFS_NAME = "TimerPrefs"
private const val KEY_VALUE = "last_value"
private const val KEY_PAUSED = "was_paused"

class MainActivity : AppCompatActivity() {

    private var timerBinder: TimerService.TimerBinder? = null
    private var isBound = false
    private var lastValue = 100

    // Handler to receive updates from the service
    private val handler = Handler(Looper.getMainLooper()) { msg ->
        lastValue = msg.what
        findViewById<TextView>(R.id.textView).text = lastValue.toString()
        // If timer finishes, clear any saved pause state
        if (lastValue <= 1) {
            clearPausedState()
        }
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

        // Load saved value if it exists to show in UI immediately
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PAUSED, false)) {
            lastValue = prefs.getInt(KEY_VALUE, 100)
            findViewById<TextView>(R.id.textView).text = lastValue.toString()
        }

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
        // If it's running (even if we are in the middle of a session), show Pause
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
                    val binder = timerBinder!!
                    if (binder.isRunning) {
                        // Action: PAUSE
                        binder.pause()
                        savePausedState(lastValue)
                    } else if (binder.paused) {
                        // Action: RESUME (thread is still alive in background)
                        binder.start(lastValue)
                        clearPausedState()
                    } else {
                        // Action: START (fresh start or continuing from closed app)
                        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        if (prefs.getBoolean(KEY_PAUSED, false)) {
                            // Continue from saved value
                            val savedValue = prefs.getInt(KEY_VALUE, 100)
                            binder.start(savedValue)
                        } else {
                            // Start fresh from 100
                            binder.start(100)
                        }
                        clearPausedState()
                    }
                    invalidateOptionsMenu()
                }
                true
            }
            R.id.action_stop -> {
                if (isBound) {
                    timerBinder?.stop()
                    clearPausedState()
                    lastValue = 100
                    findViewById<TextView>(R.id.textView).text = "100"
                    invalidateOptionsMenu()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun savePausedState(value: Int) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_VALUE, value)
            .putBoolean(KEY_PAUSED, true)
            .apply()
    }

    private fun clearPausedState() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PAUSED, false)
            .apply()
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
