package com.testdrawer

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import android.util.Log
import net.posprinter.IConnectListener
import net.posprinter.IDeviceConnection
import net.posprinter.POSConnect
import net.posprinter.POSPrinter
import net.posprinter.POSConst

class CalendarModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var curConnect: IDeviceConnection? = null
    private var printer: POSPrinter? = null

    override fun getName() = "CalendarModule"

    init {
        // Initialize POSConnect with the application context
        POSConnect.init(reactContext.applicationContext)
    }

    // Define the connection listener
    private val connectListener = IConnectListener { code, connInfo, msg ->
        when (code) {
            POSConnect.CONNECT_SUCCESS -> {
                Log.d("CalendarModule", "Connection successful: $msg")
            }
            POSConnect.CONNECT_FAIL -> {
                Log.e("CalendarModule", "Connection failed: $msg")
            }
            POSConnect.CONNECT_INTERRUPT -> {
                Log.w("CalendarModule", "Connection interrupted: $msg")
            }
            POSConnect.SEND_FAIL -> {
                Log.e("CalendarModule", "Send failed: $msg")
            }
            POSConnect.USB_DETACHED -> {
                Log.w("CalendarModule", "USB detached")
            }
            POSConnect.USB_ATTACHED -> {
                Log.i("CalendarModule", "USB attached")
            }
        }
    }

    // Example method to connect via USB
    @ReactMethod(isBlockingSynchronousMethod = true)
    fun connectUSB(pathName: String) {
        Log.d("CalendarModule", "Attempting to connect to USB printer at: $pathName")
        curConnect?.close() // Close any existing connection
        curConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_USB)
        curConnect?.connect(pathName, connectListener)
    }

    // Example method to connect via Ethernet
    @ReactMethod(isBlockingSynchronousMethod = true)
    fun connectNet(ipAddress: String) {
        Log.d("CalendarModule", "Attempting to connect to network printer at: $ipAddress")
        curConnect?.close()
        curConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_ETHERNET)
        curConnect?.connect(ipAddress, connectListener)
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    fun connectBt(macAddress: String) {
        Log.d("CalendarModule", "Attempting to connect to Bluetooth printer at: $macAddress")
        curConnect?.close() // Close any existing connection
        curConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)

        // Attempt to connect via Bluetooth and rely on the listener for status
        curConnect?.connect(macAddress, connectListener)
    }


    // Method to open the cash drawer
    @ReactMethod
    fun openCashDrawer(pin: Int) {
        try {
            if (curConnect != null) {
                printer = POSPrinter(curConnect)
                // Open the cash drawer with the specified PIN (PIN_TWO is typically used for a two-pin drawer)
                printer?.openCashBox(POSConst.PIN_TWO) 
                Log.d("CalendarModule", "Cash drawer opened successfully.")
            } else {
                Log.e("CalendarModule", "Printer not connected.")
            }
        } catch (e: Exception) {
            Log.e("CalendarModule", "Failed to open cash drawer: ${e.message}")
        }
    }
}
