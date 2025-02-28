package com.testdrawer

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.LifecycleEventListener
import android.util.Log
import net.posprinter.IConnectListener
import net.posprinter.IDeviceConnection
import net.posprinter.POSConnect
import net.posprinter.POSPrinter
import net.posprinter.POSConst

class CalendarModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {

    private var curConnect: IDeviceConnection? = null
    private var printer: POSPrinter? = null
    private var isConnected = false

    override fun getName() = "CalendarModule"

    init {
        // Initialize POSConnect with the application context
        POSConnect.init(reactContext.applicationContext)
    }

    override fun initialize() {
        super.initialize()
        reactApplicationContext.addLifecycleEventListener(this)
    }

    // Send events to JavaScript
    private fun sendEventToJS(eventName: String, params: WritableMap) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    // Define the connection listener that provides feedback to React Native
    private val connectListener = IConnectListener { code, connInfo, msg ->
        val params = Arguments.createMap()
        params.putInt("code", code)
        params.putString("message", msg)
        
        when (code) {
            POSConnect.CONNECT_SUCCESS -> {
                Log.d("CalendarModule", "Connection successful: $msg")
                isConnected = true
                params.putBoolean("connected", true)
                sendEventToJS("onPrinterConnected", params)
            }
            POSConnect.CONNECT_FAIL -> {
                Log.e("CalendarModule", "Connection failed: $msg")
                isConnected = false
                params.putBoolean("connected", false)
                sendEventToJS("onPrinterConnectionFailed", params)
            }
            POSConnect.CONNECT_INTERRUPT -> {
                Log.w("CalendarModule", "Connection interrupted: $msg")
                isConnected = false
                params.putBoolean("connected", false)
                sendEventToJS("onPrinterConnectionInterrupted", params)
            }
            POSConnect.SEND_FAIL -> {
                Log.e("CalendarModule", "Send failed: $msg")
                sendEventToJS("onPrinterSendFailed", params)
            }
            POSConnect.USB_DETACHED -> {
                Log.w("CalendarModule", "USB detached")
                isConnected = false
                sendEventToJS("onUsbDetached", params)
            }
            POSConnect.USB_ATTACHED -> {
                Log.i("CalendarModule", "USB attached")
                sendEventToJS("onUsbAttached", params)
            }
        }
    }

    // Method to connect via USB with callback
    @ReactMethod
    fun connectUSB(pathName: String, callback: Callback) {
        Log.d("CalendarModule", "Attempting to connect to USB printer at: $pathName")
        try {
            curConnect?.close() // Close any existing connection
            isConnected = false
            curConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_USB)
            curConnect?.connect(pathName, connectListener)
            callback.invoke(null, "USB connection attempt initiated")
        } catch (e: Exception) {
            Log.e("CalendarModule", "Error connecting to USB: ${e.message}")
            callback.invoke(e.message, null)
        }
    }

    // Method to connect via Ethernet with callback
    @ReactMethod
    fun connectNet(ipAddress: String, callback: Callback) {
        Log.d("CalendarModule", "Attempting to connect to network printer at: $ipAddress")
        try {
            curConnect?.close() // Close any existing connection
            isConnected = false
            curConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_ETHERNET)
            curConnect?.connect(ipAddress, connectListener)
            callback.invoke(null, "Network connection attempt initiated")
        } catch (e: Exception) {
            Log.e("CalendarModule", "Error connecting to network: ${e.message}")
            callback.invoke(e.message, null)
        }
    }

    // Method to connect via Bluetooth with callback
    @ReactMethod
    fun connectBt(macAddress: String, callback: Callback) {
        Log.d("CalendarModule", "Attempting to connect to Bluetooth printer at: $macAddress")
        try {
            curConnect?.close() // Close any existing connection
            isConnected = false
            curConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)
            curConnect?.connect(macAddress, connectListener)
            callback.invoke(null, "Bluetooth connection attempt initiated")
        } catch (e: Exception) {
            Log.e("CalendarModule", "Error connecting to Bluetooth: ${e.message}")
            callback.invoke(e.message, null)
        }
    }

    // Method to check if printer is connected
    @ReactMethod
    fun isConnected(callback: Callback) {
        // Use our internal flag instead of the method that doesn't exist
        callback.invoke(null, isConnected)
    }

    // Method to open the cash drawer with proper error handling
    @ReactMethod
    fun openCashDrawer(pin: Int, callback: Callback) {
        try {
            if (curConnect != null && isConnected) {
                if (printer == null) {
                    printer = POSPrinter(curConnect)
                }
                
                // Use the pin parameter, defaulting to PIN_TWO if an invalid value is provided
                val pinToUse = when (pin) {
                    2 -> POSConst.PIN_TWO
                    5 -> POSConst.PIN_FIVE
                    else -> POSConst.PIN_TWO // Default to PIN_TWO
                }
                
                printer?.openCashBox(pinToUse)
                Log.d("CalendarModule", "Cash drawer open command sent with pin: $pin")
                callback.invoke(null, "Cash drawer open command sent")
            } else {
                Log.e("CalendarModule", "Printer not connected or connection lost")
                callback.invoke("Printer not connected", null)
            }
        } catch (e: Exception) {
            Log.e("CalendarModule", "Failed to open cash drawer", e)
            callback.invoke("Error: ${e.message}", null)
        }
    }

    // Method to close the connection
    @ReactMethod
    fun closeConnection(callback: Callback) {
        try {
            printer = null
            curConnect?.close()
            curConnect = null
            isConnected = false
            Log.d("CalendarModule", "Connection closed")
            callback.invoke(null, "Connection closed successfully")
        } catch (e: Exception) {
            Log.e("CalendarModule", "Error closing connection: ${e.message}")
            callback.invoke(e.message, null)
        }
    }

    // Lifecycle methods
    override fun onHostResume() {
        // Nothing specific needed here
    }

    override fun onHostPause() {
        // Nothing specific needed here
    }

    override fun onHostDestroy() {
        // Clean up resources when the app is destroyed
        try {
            printer = null
            curConnect?.close()
            curConnect = null
            isConnected = false
        } catch (e: Exception) {
            Log.e("CalendarModule", "Error during cleanup: ${e.message}")
        }
    }

    // Print text to the printer (takes str as input)
    @ReactMethod
    fun printText(str: String, callback: Callback) {
        try {
            Log.d("CalendarModule", "starting to print: $str")
            // Check if the printer is connected before printing
            if (curConnect != null && isConnected) {
                printer?.initializePrinter()
                    ?.printString(str)
                    ?.printText(
                        "printText Demo\n",
                        POSConst.ALIGNMENT_CENTER,
                        POSConst.FNT_BOLD or POSConst.FNT_UNDERLINE,
                        POSConst.TXT_1WIDTH or POSConst.TXT_2HEIGHT
                    )
                    ?.cutHalfAndFeed(1)

                Log.d("CalendarModule", "Text printed successfully: $str")
                callback.invoke(null, "Text printed successfully")
            } else {
                callback.invoke("Printer not connected", null)
            }
        } catch (e: Exception) {
            Log.e("CalendarModule", "Error printing text: ${e.message}")
            callback.invoke("Error printing text: ${e.message}", null)
        }
    }
}
