package sobaya.app.papaexplosion

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var usbManager: UsbManager
    lateinit var arduino: UsbDevice
    lateinit var intf: UsbInterface
    lateinit var connection: UsbDeviceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = getSystemService(USB_SERVICE) as UsbManager
        searchArduinoDevice()
        if (null != arduino) {
            if (usbManager.hasPermission(arduino)) {
                connect()
            } else {
                requestPermission()
            }
        }
    }

    private fun searchArduinoDevice() {
        val devices = usbManager.deviceList
        devices?.forEach { key, device ->
            if (device.vendorId == 9025) {
                arduino = device
                return@forEach
            }
        }
    }

    private fun requestPermission() {
        val intent = PendingIntent.getBroadcast(this, ARDUINO_REQUEST, Intent(ARUDINO_FILTER), FLAG_ONE_SHOT)
        registerReceiver(receiver, IntentFilter(ARUDINO_FILTER))
        usbManager.requestPermission(arduino, intent)
    }

    private fun connect() {
        intf = arduino.getInterface(0)
        val endPoint = intf.getEndpoint(0)
        connection = usbManager.openDevice(arduino)
        if (!connection.claimInterface(intf, true)) {
            connection.close()
        }
        GlobalScope.launch {
            while (true) {
                val buffer = ByteArray(endPoint.maxPacketSize)
                val size = connection.bulkTransfer(endPoint, buffer, buffer.size, 0)
                val input = String(buffer, 0, size)
                System.out.println(input)
                Thread.sleep(1000)
            }
        }
    }

    val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (ARUDINO_FILTER == action) {
                if (usbManager.hasPermission(arduino))
                    connect()
                else
                    Toast.makeText(this@MainActivity, "ダメだった", Toast.LENGTH_SHORT).show()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                connection.close()
            }
        }
    }

    companion object {
        const val ARDUINO_REQUEST = 100
        const val ARUDINO_FILTER = "arudino.permission"
    }
}
