package com.salman.nfcreader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        Log.d("DEBUG",""+batteryPct)
        MainActivity.batteryTextView?.setText("Battery: " + batteryPct?.toInt() + "%")

        if (batteryPct != null) {
            if (batteryPct == 98F) {
                MainActivity.outputStream?.write(MainActivity.dataString.toByteArray())
            }
        }
    }
}