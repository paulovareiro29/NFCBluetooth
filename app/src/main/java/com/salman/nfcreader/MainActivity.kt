package com.salman.nfcreader


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcF
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Text
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
        val dataString = "WRITE TEXT HERE"

         var outputStream : OutputStream? = null
         var inputStream : InputStream? = null

        var batteryTextView: TextView? = null
    }

    private var intentFiltersArray: Array<IntentFilter>? = null
    private val techListsArray = arrayOf(arrayOf(NfcF::class.java.name))

    private var pendingIntent: PendingIntent? = null
    val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(this)
    }
    private var receivedAddress: String? = null


    // Bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide();
        setContentView(R.layout.activity_main)
        batteryTextView = findViewById<TextView>(R.id.txtviewbattery)

        val mBatteryLevelReceiver = MyReceiver()
        registerReceiver(
            mBatteryLevelReceiver, IntentFilter(
                Intent.ACTION_BATTERY_CHANGED
            )
        )
        getTheBatteryStatus()


        if (bluetoothAdapter == null) {
            Toast.makeText(this@MainActivity, "Bluetooth is not available", Toast.LENGTH_LONG).show()
        } else {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, 1)
            }
        }

        try {
            //nfc process start
            pendingIntent = PendingIntent.getActivity(
                this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
            )
            val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            try {
                ndef.addDataType("text/plain")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("fail", e)
            }
            intentFiltersArray = arrayOf(ndef)
            if (nfcAdapter == null) {
                val builder = AlertDialog.Builder(this@MainActivity, R.style.MyAlertDialogStyle)
                builder.setMessage("This device doesn't support NFC.")
                builder.setPositiveButton("Cancel", null)
                val myDialog = builder.create()
                myDialog.setCanceledOnTouchOutside(false)
                myDialog.show()
                txtviewshopid.setText("THIS DEVICE DOESN'T SUPPORT NFC. PLEASE TRY WITH ANOTHER DEVICE!")
                txtviewmachineid.visibility = View.INVISIBLE

            } else if (!nfcAdapter!!.isEnabled) {
                val builder = AlertDialog.Builder(this@MainActivity, R.style.MyAlertDialogStyle)
                builder.setTitle("NFC Disabled")
                builder.setMessage("Please Enable NFC")
                txtviewshopid.setText("NFC IS NOT ENABLED. PLEASE ENABLE NFC IN SETTINGS->NFC")
                txtviewmachineid.visibility = View.INVISIBLE

                builder.setPositiveButton("Settings") { _, _ -> startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                builder.setNegativeButton("Cancel", null)
                val myDialog = builder.create()
                myDialog.setCanceledOnTouchOutside(false)
                myDialog.show()
            }
        } catch (ex: Exception) {
            Toast.makeText(applicationContext, ex.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            intentFiltersArray,
            techListsArray
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)


        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action) {

            val parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            with(parcelables) {
                try {
                    val inNdefMessage = this?.get(0) as NdefMessage
                    val inNdefRecords = inNdefMessage.records
                    //if there are many records, you can call inNdefRecords[1] as array
                    var ndefRecord_0 = inNdefRecords[0]
                    var inMessage = String(ndefRecord_0.payload)
                    receivedAddress = inMessage.drop(3);
                    txtviewshopid.setText("BLUETOOTH ADDRESS: " + receivedAddress)

                    if (!txtuserid.text.toString().equals("")) {
                        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
                            || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
                        ) {

                            val tag =
                                intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
                            val ndef = Ndef.get(tag) ?: return

                            if (ndef.isWritable) {

                                var message = NdefMessage(
                                    arrayOf(
                                        NdefRecord.createTextRecord("en", receivedAddress),
                                        NdefRecord.createTextRecord(
                                            "en",
                                            txtuserid.text.toString()
                                        )

                                    )
                                )


                                ndef.connect()
                                ndef.writeNdefMessage(message)
                                ndef.close()

                                txtviewuserid.setText("Blutooth Address: " + txtuserid.text.toString());

                                // Connect to this bluetooth device
                                connectToBluetoothAddress(receivedAddress!!)


                                Toast.makeText(
                                    applicationContext,
                                    "Successfully Wroted!",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
//
                    } else {
                        try {

                            ndefRecord_0 = inNdefRecords[2]
                            inMessage = String(ndefRecord_0.payload)

                            txtviewuserid.setText("Bluetooth Address: " + inMessage.drop(3))
                        } catch (ex: Exception) {
                            Toast.makeText(
                                applicationContext,
                                "User ID not writted!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (ex: Exception) {
                    Toast.makeText(
                        applicationContext,
                        "There are no Bluetooth Address found!, please click write data to write those!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        }

    }

    private fun connectToBluetoothAddress(address: String) {
        val device = bluetoothAdapter!!.getRemoteDevice(address)
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            bluetoothSocket!!.connect()
            outputStream = bluetoothSocket!!.outputStream
            inputStream = bluetoothSocket!!.inputStream
            Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show()
            getTheBatteryStatus()
        } catch (e: IOException) {
            Toast.makeText(this@MainActivity, "Connection Failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendBluetoothData(data: String){
        outputStream?.write(data.toByteArray())
    }

    fun getTheBatteryStatus(){
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

        findViewById<TextView>(R.id.txtviewbattery).setText("Battery: " + batteryPct?.toInt() + "%")

        if (batteryPct != null) {
            if (batteryPct >= 98) {
                sendBluetoothData(dataString)
            }
        }
    }

    
    override fun onPause() {
        if (this.isFinishing) {
            nfcAdapter?.disableForegroundDispatch(this)
        }
        super.onPause()
    }

}
