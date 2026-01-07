package dev.jimmy.zebra_rfid_reader

import android.content.Context
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class ZebraRfidReaderPlugin : FlutterPlugin, MethodCallHandler {

    companion object {
        private const val CHANNEL_NAME = "zebra_rfid_reader"
        private const val EVENT_CHANNEL = "zebra_rfid_reader/events"
    }

    private lateinit var context: Context
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    
    private var rfidHandler: RFIDHandler? = null
    private var eventStreamHandler: EventStreamHandler? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext

        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
        methodChannel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, EVENT_CHANNEL)
        eventStreamHandler = EventStreamHandler()
        eventChannel.setStreamHandler(eventStreamHandler)

        try {
            rfidHandler = RFIDHandler(context, eventStreamHandler!!, eventStreamHandler!!)
        } catch (e: Exception) {
            // Silent - handler creation failed
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        try {
            when (call.method) {
                "initialize" -> {
                    rfidHandler?.initialize(object : RFIDHandler.ResultCallback {
                        override fun onSuccess(data: Any?) {
                            result.success(data)
                        }
                        override fun onError(errorCode: String, errorMessage: String, errorDetails: Any?) {
                            result.error(errorCode, errorMessage, errorDetails)
                        }
                    })
                }

                "getAllAvailableReaders" -> {
                    rfidHandler?.getAllAvailableReaders(object : RFIDHandler.ResultCallback {
                        override fun onSuccess(data: Any?) {
                            result.success(data)
                        }
                        override fun onError(errorCode: String, errorMessage: String, errorDetails: Any?) {
                            result.error(errorCode, errorMessage, errorDetails)
                        }
                    })
                }

                "isReaderConnected" -> {
                    rfidHandler?.isReaderConnected(object : RFIDHandler.ResultCallback {
                        override fun onSuccess(data: Any?) {
                            result.success(data)
                        }
                        override fun onError(errorCode: String, errorMessage: String, errorDetails: Any?) {
                            result.error(errorCode, errorMessage, errorDetails)
                        }
                    })
                }

                "connectReader" -> {
                    val readerName = call.argument<String>("readerName")
                    rfidHandler?.connectReader(readerName, object : RFIDHandler.ResultCallback {
                        override fun onSuccess(data: Any?) {
                            result.success(data)
                        }
                        override fun onError(errorCode: String, errorMessage: String, errorDetails: Any?) {
                            result.error(errorCode, errorMessage, errorDetails)
                        }
                    })
                }

                "disconnectReader" -> {
                    rfidHandler?.disconnectReader(object : RFIDHandler.ResultCallback {
                        override fun onSuccess(data: Any?) {
                            result.success(data)
                        }
                        override fun onError(errorCode: String, errorMessage: String, errorDetails: Any?) {
                            result.error(errorCode, errorMessage, errorDetails)
                        }
                    })
                }

                "startInventory" -> {
                    rfidHandler?.startInventory(object : RFIDHandler.ResultCallback {
                        override fun onSuccess(data: Any?) {
                            result.success(data)
                        }
                        override fun onError(errorCode: String, errorMessage: String, errorDetails: Any?) {
                            result.error(errorCode, errorMessage, errorDetails)
                        }
                    })
                }

                "stopInventory" -> {
                    rfidHandler?.stopInventory(object : RFIDHandler.ResultCallback {
                        override fun onSuccess(data: Any?) {
                            result.success(data)
                        }
                        override fun onError(errorCode: String, errorMessage: String, errorDetails: Any?) {
                            result.error(errorCode, errorMessage, errorDetails)
                        }
                    })
                }

                "setAntennaPower" -> {
                    val powerLevel = call.argument<Int>("powerLevel")
                    if (powerLevel == null) {
                        result.error("INVALID_ARGUMENT", "powerLevel is required", null)
                        return
                    }
                    rfidHandler?.setAntennaPower(powerLevel, object : RFIDHandler.ResultCallback {
                        override fun onSuccess(data: Any?) {
                            result.success(data)
                        }
                        override fun onError(errorCode: String, errorMessage: String, errorDetails: Any?) {
                            result.error(errorCode, errorMessage, errorDetails)
                        }
                    })
                }

                "getAntennaPower" -> {
                    rfidHandler?.getAntennaPower(object : RFIDHandler.ResultCallback {
                        override fun onSuccess(data: Any?) {
                            result.success(data)
                        }
                        override fun onError(errorCode: String, errorMessage: String, errorDetails: Any?) {
                            result.error(errorCode, errorMessage, errorDetails)
                        }
                    })
                }

                "getPlatformVersion" -> {
                    val version = "Android ${android.os.Build.VERSION.RELEASE}"
                    result.success(mapOf("version" to version))
                }

                else -> {
                    result.notImplemented()
                }
            }
        } catch (e: Exception) {
            result.error("METHOD_CALL_ERROR", "Error executing ${call.method}: ${e.message}", null)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        try {
            methodChannel.setMethodCallHandler(null)
            eventChannel.setStreamHandler(null)
            rfidHandler?.dispose()
            rfidHandler = null
            eventStreamHandler = null
        } catch (e: Exception) {
            // Silent
        }
    }

    class EventStreamHandler : EventChannel.StreamHandler {
        private var eventSink: EventChannel.EventSink? = null

        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            eventSink = events
        }

        override fun onCancel(arguments: Any?) {
            eventSink = null
        }

        fun sendEvent(event: Map<String, Any?>) {
            eventSink?.success(event)
        }

        fun sendError(errorCode: String, errorMessage: String, errorDetails: Any?) {
            eventSink?.error(errorCode, errorMessage, errorDetails)
        }
    }
}