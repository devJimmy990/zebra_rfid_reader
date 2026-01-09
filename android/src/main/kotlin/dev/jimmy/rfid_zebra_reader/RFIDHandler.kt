package dev.jimmy.rfid_zebra_reader

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.zebra.rfid.api3.*
import java.util.ArrayList

class RFIDHandler(
    private val context: Context,
    private val tagReadEventSink: RfidZebraReaderPlugin.EventStreamHandler,
    private val statusEventSink: RfidZebraReaderPlugin.EventStreamHandler
) : Readers.RFIDReaderEventHandler {

    private var readers: Readers? = null
    private var availableRFIDReaderList: ArrayList<ReaderDevice>? = null
    private var readerDevice: ReaderDevice? = null
    private var reader: RFIDReader? = null
    private var eventHandler: EventHandler? = null

    @Volatile
    private var isInitialized = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val status: StringBuilder = StringBuilder("RFIDHandler created")
    private var maxPower = 270

    interface ResultCallback {
        fun onSuccess(data: Any?)
        fun onError(errorCode: String, errorMessage: String, errorDetails: Any?)
    }

    // ═══════════════════════════════════════════════════════════════════
    // INITIALIZE - MANUAL CALL FROM FLUTTER
    // ═══════════════════════════════════════════════════════════════════

    fun initialize(callback: ResultCallback) {
        status.append("\n[INIT] Initialize called at ${System.currentTimeMillis()}")
        sendStatusEvent("initializing", "Starting SDK initialization...")

        Thread {
            try {
                if (isInitialized) {
                    status.append("\n[INIT] Already initialized")
                    sendStatusEvent("initialized", "SDK already initialized")
                    mainHandler.post {
                        callback.onSuccess(mapOf(
                            "status" to status.toString(),
                            "message" to "Already initialized"
                        ))
                    }
                    return@Thread
                }

                status.append("\n[INIT] Creating Readers instance...")
                status.append("\n[INIT] Device: ${Build.MODEL} | Android: ${Build.VERSION.RELEASE}")

                val transports = listOf(
                    ENUM_TRANSPORT.BLUETOOTH,
                    ENUM_TRANSPORT.SERVICE_USB,
                    ENUM_TRANSPORT.SERVICE_SERIAL
                )

                var foundTransport: ENUM_TRANSPORT? = null
                var initSuccess = false

                for (transport in transports) {
                    try {
                        status.append("\n[INIT] Trying transport: $transport")

                        val safeContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Android13ContextWrapper(context)
                        } else {
                            context
                        }

                        readers = Readers(safeContext, transport)
                        status.append("\n[INIT] Readers object created")

                        val list = readers?.GetAvailableRFIDReaderList()
                        val count = list?.size ?: 0
                        status.append("\n[INIT] Found $count reader(s) on $transport")

                        if (count > 0) {
                            availableRFIDReaderList = list
                            foundTransport = transport
                            status.append("\n[INIT] SUCCESS on $transport!")

                            list?.forEachIndexed { index, device ->
                                status.append("\n[INIT] Reader[$index]: ${device.getName()} | ${device.getAddress()}")
                            }

                            initSuccess = true
                            break
                        } else {
                            status.append("\n[INIT] No readers on $transport")
                        }
                    } catch (e: InvalidUsageException) {
                        status.append("\n[INIT] InvalidUsageException on $transport: ${e.info}")
                    } catch (e: Exception) {
                        status.append("\n[INIT] Exception on $transport: ${e.message}")
                    }
                }

                if (initSuccess) {
                    isInitialized = true
                    status.append("\n[INIT] SDK initialized on $foundTransport!")
                    sendStatusEvent(
                        "initialized",
                        "SDK ready - ${availableRFIDReaderList?.size ?: 0} reader(s)",
                        mapOf(
                            "transport" to foundTransport.toString(),
                            "readerCount" to (availableRFIDReaderList?.size ?: 0)
                        )
                    )

                    mainHandler.post {
                        callback.onSuccess(mapOf(
                            "status" to status.toString(),
                            "message" to "SDK initialized on $foundTransport",
                            "transport" to foundTransport.toString(),
                            "readerCount" to (availableRFIDReaderList?.size ?: 0)
                        ))
                    }
                } else {
                    status.append("\n[INIT] FAILED: No readers found on any transport")
                    sendStatusEvent("error", "No readers found")

                    mainHandler.post {
                        callback.onError(
                            "INIT_FAILED",
                            "No readers found on any transport",
                            mapOf("status" to status.toString())
                        )
                    }
                }

            } catch (e: Exception) {
                status.append("\n[INIT ERROR] Exception: ${e.message}")
                sendStatusEvent("error", "Initialization exception: ${e.message}")

                mainHandler.post {
                    callback.onError(
                        "INIT_ERROR",
                        "Initialization error: ${e.message}",
                        mapOf("status" to status.toString())
                    )
                }
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET ALL AVAILABLE READERS
    // ═══════════════════════════════════════════════════════════════════

    fun getAllAvailableReaders(callback: ResultCallback) {
        status.append("\n[GET_READERS] Called")

        Thread {
            try {
                if (!isInitialized) {
                    val errorMsg = "SDK not initialized. Call initialize() first."
                    status.append("\n[GET_READERS ERROR] $errorMsg")
                    mainHandler.post {
                        callback.onError("SDK_NOT_INITIALIZED", errorMsg, mapOf("status" to status.toString()))
                    }
                    return@Thread
                }

                val readersList = mutableListOf<Map<String, String>>()

                availableRFIDReaderList?.forEach { device ->
                    try {
                        readersList.add(mapOf(
                            "name" to device.getName(),
                            "address" to (device.getAddress() ?: "N/A"),
                            "model" to (device.getRFIDReader()?.getHostName() ?: "Unknown")
                        ))
                        status.append("\n[GET_READERS] Found: ${device.getName()}")
                    } catch (e: Exception) {
                        status.append("\n[GET_READERS] Error reading device: ${e.message}")
                    }
                }

                status.append("\n[GET_READERS] Total found: ${readersList.size}")

                mainHandler.post {
                    callback.onSuccess(mapOf(
                        "status" to status.toString(),
                        "readers" to readersList
                    ))
                }

            } catch (e: Exception) {
                status.append("\n[GET_READERS ERROR] Exception: ${e.message}")
                mainHandler.post {
                    callback.onError(
                        "GET_READERS_ERROR",
                        "Failed to get readers: ${e.message}",
                        mapOf("status" to status.toString())
                    )
                }
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════════
    // IS READER CONNECTED
    // ═══════════════════════════════════════════════════════════════════

    fun isReaderConnected(callback: ResultCallback) {
        status.append("\n[IS_CONNECTED] Checking connection status")

        Thread {
            try {
                val connected = reader != null && reader!!.isConnected
                status.append("\n[IS_CONNECTED] Result: $connected")

                mainHandler.post {
                    callback.onSuccess(mapOf(
                        "status" to status.toString(),
                        "connected" to connected
                    ))
                }

            } catch (e: Exception) {
                status.append("\n[IS_CONNECTED ERROR] Exception: ${e.message}")
                mainHandler.post {
                    callback.onError(
                        "CONNECTION_CHECK_ERROR",
                        "Failed to check connection: ${e.message}",
                        mapOf("status" to status.toString())
                    )
                }
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════════
    // CONNECT READER
    // ═══════════════════════════════════════════════════════════════════

    fun connectReader(readerName: String?, callback: ResultCallback) {
        status.append("\n[CONNECT] Called with reader: ${readerName ?: "auto-select"}")

        Thread {
            try {
                if (!isInitialized) {
                    val errorMsg = "SDK not initialized"
                    status.append("\n[CONNECT ERROR] $errorMsg")
                    mainHandler.post {
                        callback.onError("SDK_NOT_INITIALIZED", errorMsg, mapOf("status" to status.toString()))
                    }
                    return@Thread
                }

                if (reader != null && reader!!.isConnected) {
                    val msg = "Already connected to ${reader!!.getHostName()}"
                    status.append("\n[CONNECT] $msg")
                    mainHandler.post {
                        callback.onSuccess(mapOf(
                            "status" to status.toString(),
                            "message" to msg
                        ))
                    }
                    return@Thread
                }

                status.append("\n[CONNECT] Fetching available readers...")
                getAvailableReaders()

                if (availableRFIDReaderList.isNullOrEmpty()) {
                    val errorMsg = "No readers available"
                    status.append("\n[CONNECT ERROR] $errorMsg")
                    mainHandler.post {
                        callback.onError("NO_READERS", errorMsg, mapOf("status" to status.toString()))
                    }
                    return@Thread
                }

                if (readerName != null) {
                    val foundReader = availableRFIDReaderList!!.find {
                        it.getName().contains(readerName, ignoreCase = true)
                    }
                    if (foundReader != null) {
                        readerDevice = foundReader
                        reader = readerDevice?.getRFIDReader()
                        status.append("\n[CONNECT] Selected reader by name: ${foundReader.getName()}")
                    } else {
                        val errorMsg = "Reader '$readerName' not found"
                        status.append("\n[CONNECT ERROR] $errorMsg")
                        mainHandler.post {
                            callback.onError("READER_NOT_FOUND", errorMsg, mapOf("status" to status.toString()))
                        }
                        return@Thread
                    }
                } else {
                    readerDevice = availableRFIDReaderList!![0]
                    reader = readerDevice?.getRFIDReader()
                    status.append("\n[CONNECT] Auto-selected first reader: ${readerDevice?.getName()}")
                }

                val result = connectToReader()

                mainHandler.post {
                    if (result.contains("Connected", ignoreCase = true)) {
                        callback.onSuccess(mapOf(
                            "status" to status.toString(),
                            "message" to result
                        ))
                    } else {
                        callback.onError(
                            "CONNECTION_FAILED",
                            result,
                            mapOf("status" to status.toString())
                        )
                    }
                }

            } catch (e: Exception) {
                status.append("\n[CONNECT ERROR] Exception: ${e.message}")
                mainHandler.post {
                    callback.onError(
                        "CONNECT_ERROR",
                        "Connection error: ${e.message}",
                        mapOf("status" to status.toString())
                    )
                }
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════════
    // DISCONNECT READER
    // ═══════════════════════════════════════════════════════════════════

    fun disconnectReader(callback: ResultCallback) {
        status.append("\n[DISCONNECT] Called")

        Thread {
            try {
                if (reader == null) {
                    val msg = "No reader to disconnect"
                    status.append("\n[DISCONNECT] $msg")
                    mainHandler.post {
                        callback.onSuccess(mapOf(
                            "status" to status.toString(),
                            "message" to msg
                        ))
                    }
                    return@Thread
                }

                if (!reader!!.isConnected) {
                    val msg = "Already disconnected"
                    status.append("\n[DISCONNECT] $msg")
                    mainHandler.post {
                        callback.onSuccess(mapOf(
                            "status" to status.toString(),
                            "message" to msg
                        ))
                    }
                    return@Thread
                }

                status.append("\n[DISCONNECT] Removing event listener...")
                eventHandler?.let { reader!!.Events.removeEventsListener(it) }

                status.append("\n[DISCONNECT] Calling reader.disconnect()...")
                reader!!.disconnect()

                status.append("\n[DISCONNECT] Disconnected successfully")
                sendStatusEvent("disconnected", "Reader disconnected")

                mainHandler.post {
                    callback.onSuccess(mapOf(
                        "status" to status.toString(),
                        "message" to "Disconnected successfully"
                    ))
                }

            } catch (e: Exception) {
                status.append("\n[DISCONNECT ERROR] Exception: ${e.message}")
                mainHandler.post {
                    callback.onError(
                        "DISCONNECT_ERROR",
                        "Disconnect failed: ${e.message}",
                        mapOf("status" to status.toString())
                    )
                }
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════════
    // START INVENTORY
    // ═══════════════════════════════════════════════════════════════════

    fun startInventory(callback: ResultCallback) {
        status.append("\n[START_INV] Called")

        Thread {
            try {
                if (reader == null || !reader!!.isConnected) {
                    val errorMsg = "Reader not connected"
                    status.append("\n[START_INV ERROR] $errorMsg")
                    mainHandler.post {
                        callback.onError("READER_NOT_CONNECTED", errorMsg, mapOf("status" to status.toString()))
                    }
                    return@Thread
                }

                status.append("\n[START_INV] Calling reader.Actions.Inventory.perform()...")
                
                try {
                    reader!!.Actions.Inventory.perform()
                    status.append("\n[START_INV] Inventory started successfully")
                    sendStatusEvent("inventory_started", "Inventory started")

                    mainHandler.post {
                        callback.onSuccess(mapOf(
                            "status" to status.toString(),
                            "message" to "Inventory started"
                        ))
                    }
                } catch (e: OperationFailureException) {
                    status.append("\n[START_INV] First attempt failed, retrying...")
                    try {
                        reader!!.Actions.Inventory.stop()
                        Thread.sleep(500)
                        reader!!.Actions.Inventory.perform()
                        status.append("\n[START_INV] Inventory started (retry success)")
                        sendStatusEvent("inventory_started", "Inventory started")

                        mainHandler.post {
                            callback.onSuccess(mapOf(
                                "status" to status.toString(),
                                "message" to "Inventory started"
                            ))
                        }
                    } catch (retryE: Exception) {
                        status.append("\n[START_INV ERROR] Retry failed: ${retryE.message}")
                        mainHandler.post {
                            callback.onError(
                                "START_INVENTORY_FAILED",
                                "Failed to start: ${e.vendorMessage}",
                                mapOf("status" to status.toString())
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                status.append("\n[START_INV ERROR] Exception: ${e.message}")
                mainHandler.post {
                    callback.onError(
                        "START_INVENTORY_ERROR",
                        "Failed to start: ${e.message}",
                        mapOf("status" to status.toString())
                    )
                }
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════════
    // STOP INVENTORY
    // ═══════════════════════════════════════════════════════════════════

    fun stopInventory(callback: ResultCallback) {
        status.append("\n[STOP_INV] Called")

        Thread {
            try {
                if (reader == null || !reader!!.isConnected) {
                    val errorMsg = "Reader not connected"
                    status.append("\n[STOP_INV ERROR] $errorMsg")
                    mainHandler.post {
                        callback.onError("READER_NOT_CONNECTED", errorMsg, mapOf("status" to status.toString()))
                    }
                    return@Thread
                }

                status.append("\n[STOP_INV] Calling reader.Actions.Inventory.stop()...")
                reader!!.Actions.Inventory.stop()
                status.append("\n[STOP_INV] Inventory stopped successfully")

                sendStatusEvent("inventory_stopped", "Inventory stopped")

                mainHandler.post {
                    callback.onSuccess(mapOf(
                        "status" to status.toString(),
                        "message" to "Inventory stopped"
                    ))
                }

            } catch (e: Exception) {
                status.append("\n[STOP_INV ERROR] Exception: ${e.message}")
                mainHandler.post {
                    callback.onError(
                        "STOP_INVENTORY_ERROR",
                        "Failed to stop: ${e.message}",
                        mapOf("status" to status.toString())
                    )
                }
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════════
    // SET ANTENNA POWER
    // ═══════════════════════════════════════════════════════════════════

    fun setAntennaPower(powerLevel: Int, callback: ResultCallback) {
        status.append("\n[SET_POWER] Called with power: $powerLevel")

        Thread {
            try {
                if (reader == null || !reader!!.isConnected) {
                    val errorMsg = "Reader not connected"
                    status.append("\n[SET_POWER ERROR] $errorMsg")
                    mainHandler.post {
                        callback.onError("READER_NOT_CONNECTED", errorMsg, mapOf("status" to status.toString()))
                    }
                    return@Thread
                }

                if (powerLevel !in 0..maxPower) {
                    val errorMsg = "Invalid power level. Must be 0-$maxPower"
                    status.append("\n[SET_POWER ERROR] $errorMsg")
                    mainHandler.post {
                        callback.onError("INVALID_POWER_LEVEL", errorMsg, mapOf("status" to status.toString()))
                    }
                    return@Thread
                }

                status.append("\n[SET_POWER] Getting antenna config...")
                val config = reader!!.Config.Antennas.getAntennaRfConfig(1)
                config.setTransmitPowerIndex(powerLevel)

                status.append("\n[SET_POWER] Setting antenna config...")
                reader!!.Config.Antennas.setAntennaRfConfig(1, config)

                status.append("\n[SET_POWER] Power set to $powerLevel successfully")

                mainHandler.post {
                    callback.onSuccess(mapOf(
                        "status" to status.toString(),
                        "message" to "Power set to $powerLevel",
                        "powerLevel" to powerLevel
                    ))
                }

            } catch (e: Exception) {
                status.append("\n[SET_POWER ERROR] Exception: ${e.message}")
                mainHandler.post {
                    callback.onError(
                        "SET_POWER_ERROR",
                        "Failed to set power: ${e.message}",
                        mapOf("status" to status.toString())
                    )
                }
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET ANTENNA POWER
    // ═══════════════════════════════════════════════════════════════════

    fun getAntennaPower(callback: ResultCallback) {
        status.append("\n[GET_POWER] Called")

        Thread {
            try {
                if (reader == null || !reader!!.isConnected) {
                    val errorMsg = "Reader not connected"
                    status.append("\n[GET_POWER ERROR] $errorMsg")
                    mainHandler.post {
                        callback.onError("READER_NOT_CONNECTED", errorMsg, mapOf("status" to status.toString()))
                    }
                    return@Thread
                }

                status.append("\n[GET_POWER] Getting antenna config...")
                val config = reader!!.Config.Antennas.getAntennaRfConfig(1)
                val currentPower = config.getTransmitPowerIndex()

                status.append("\n[GET_POWER] Current: $currentPower, Max: $maxPower")

                mainHandler.post {
                    callback.onSuccess(mapOf(
                        "status" to status.toString(),
                        "currentPower" to currentPower,
                        "maxPower" to maxPower
                    ))
                }

            } catch (e: Exception) {
                status.append("\n[GET_POWER ERROR] Exception: ${e.message}")
                mainHandler.post {
                    callback.onError(
                        "GET_POWER_ERROR",
                        "Failed to get power: ${e.message}",
                        mapOf("status" to status.toString())
                    )
                }
            }
        }.start()
    }

    // ═══════════════════════════════════════════════════════════════════
    // DISPOSE
    // ═══════════════════════════════════════════════════════════════════

    fun dispose() {
        status.append("\n[DISPOSE] Disposing RFIDHandler...")

        try {
            disconnectReader(object : ResultCallback {
                override fun onSuccess(data: Any?) {
                    status.append("\n[DISPOSE] Disconnected before dispose")
                }
                override fun onError(errorCode: String, errorMessage: String, errorDetails: Any?) {
                    status.append("\n[DISPOSE] Disconnect error: $errorMessage")
                }
            })

            readers?.Dispose()
            readers = null
            reader = null
            isInitialized = false

            status.append("\n[DISPOSE] Disposed successfully")

        } catch (e: Exception) {
            status.append("\n[DISPOSE ERROR] Exception: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════

    private fun configureReader() {
        status.append("\n[CONFIG] Configuring reader: ${reader?.getHostName()}")

        try {
            reader?.let { rfidReader ->
                if (eventHandler == null) {
                    eventHandler = EventHandler()
                }
                rfidReader.Events.addEventsListener(eventHandler)
                status.append("\n[CONFIG] Event listener added")

                rfidReader.Events.setHandheldEvent(true)
                rfidReader.Events.setTagReadEvent(true)
                rfidReader.Events.setAttachTagDataWithReadEvent(false)
                rfidReader.Events.setReaderDisconnectEvent(true)
                status.append("\n[CONFIG] Events enabled")

                rfidReader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true)
                status.append("\n[CONFIG] Trigger mode set to RFID_MODE")

                val triggerInfo = TriggerInfo()
                triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE)
                triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_DURATION)
                triggerInfo.StopTrigger.setDurationMilliSeconds(0)
                rfidReader.Config.setStartTrigger(triggerInfo.StartTrigger)
                rfidReader.Config.setStopTrigger(triggerInfo.StopTrigger)
                status.append("\n[CONFIG] Triggers configured (continuous)")

                val powerLevels = rfidReader.ReaderCapabilities.getTransmitPowerLevelValues()
                maxPower = powerLevels.size - 1
                status.append("\n[CONFIG] Max power: $maxPower")

                val config = rfidReader.Config.Antennas.getAntennaRfConfig(1)
                config.setTransmitPowerIndex(maxPower)
                config.setrfModeTableIndex(0)
                config.setTari(0)
                rfidReader.Config.Antennas.setAntennaRfConfig(1, config)
                status.append("\n[CONFIG] Antenna configured with max power")

                val singulationControl = rfidReader.Config.Antennas.getSingulationControl(1)
                singulationControl.setSession(SESSION.SESSION_S0)
                singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A)
                singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL)
                rfidReader.Config.Antennas.setSingulationControl(1, singulationControl)
                status.append("\n[CONFIG] Singulation set to SESSION_S0")

                rfidReader.Actions.PreFilters.deleteAll()
                status.append("\n[CONFIG] Prefilters cleared")

                status.append("\n[CONFIG] Reader configured successfully")
            }
        } catch (e: Exception) {
            status.append("\n[CONFIG ERROR] Exception: ${e.message}")
            sendStatusEvent("error", "Configuration error: ${e.message}")
        }
    }

    private fun getAvailableReaders() {
        status.append("\n[GET_AVAIL] Getting available readers...")

        try {
            readers?.let { rfidReaders ->
                Readers.attach(this)

                availableRFIDReaderList = rfidReaders.GetAvailableRFIDReaderList()
                val count = availableRFIDReaderList?.size ?: 0
                status.append("\n[GET_AVAIL] Found $count reader(s)")

                if (count > 0) {
                    availableRFIDReaderList!!.forEachIndexed { index, device ->
                        status.append("\n[GET_AVAIL] Reader $index: ${device.getName()}")
                    }
                } else {
                    status.append("\n[GET_AVAIL] No readers detected")
                }
            }
        } catch (e: Exception) {
            status.append("\n[GET_AVAIL ERROR] Exception: ${e.message}")
        }
    }

    private fun connectToReader(): String {
        status.append("\n[CONN_TO] Connecting to reader...")

        return try {
            reader?.let { rfidReader ->
                if (!rfidReader.isConnected) {
                    val hostname = rfidReader.getHostName()
                    status.append("\n[CONN_TO] Connecting to $hostname...")

                    rfidReader.connect()
                    status.append("\n[CONN_TO] connect() called")

                    configureReader()

                    if (rfidReader.isConnected) {
                        val msg = "Connected to $hostname"
                        status.append("\n[CONN_TO] SUCCESS: $msg")
                        sendStatusEvent("connected", msg, mapOf("readerName" to hostname))
                        msg
                    } else {
                        val msg = "Connection failed (not connected after connect())"
                        status.append("\n[CONN_TO ERROR] $msg")
                        msg
                    }
                } else {
                    val msg = "Already connected"
                    status.append("\n[CONN_TO] $msg")
                    msg
                }
            } ?: run {
                val msg = "No reader available"
                status.append("\n[CONN_TO ERROR] $msg")
                msg
            }
        } catch (e: InvalidUsageException) {
            status.append("\n[CONN_TO ERROR] InvalidUsageException: ${e.message}")
            "InvalidUsageException: ${e.message}"
        } catch (e: OperationFailureException) {
            status.append("\n[CONN_TO ERROR] OperationFailureException: ${e.vendorMessage}")
            "OperationFailureException: ${e.vendorMessage}"
        } catch (e: Exception) {
            status.append("\n[CONN_TO ERROR] Exception: ${e.message}")
            "Exception: ${e.message}"
        }
    }

    private fun sendTagReadEvent(tags: List<Map<String, Any>>) {
        mainHandler.post {
            try {
                tagReadEventSink.sendEvent(mapOf(
                    "type" to "tagRead",
                    "tags" to tags,
                    "status" to status.toString()
                ))
            } catch (e: Exception) {
                status.append("\n[EVENT ERROR] Failed to send tag event: ${e.message}")
            }
        }
    }

    private fun sendStatusEvent(type: String, message: String, data: Map<String, Any>? = null) {
        mainHandler.post {
            try {
                val event = mutableMapOf<String, Any?>(
                    "type" to type,
                    "message" to message,
                    "status" to status.toString()
                )
                data?.let { event.putAll(it) }
                statusEventSink.sendEvent(event)
            } catch (e: Exception) {
                status.append("\n[EVENT ERROR] Failed to send status event: ${e.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // READER EVENT HANDLERS
    // ═══════════════════════════════════════════════════════════════════

    override fun RFIDReaderAppeared(readerDevice: ReaderDevice?) {
        val name = readerDevice?.getName() ?: "Unknown"
        status.append("\n[READER_EVENT] Reader appeared: $name")
        sendStatusEvent("readerAppeared", "Reader appeared: $name", mapOf("readerName" to name))
    }

    override fun RFIDReaderDisappeared(readerDevice: ReaderDevice?) {
        val name = readerDevice?.getName() ?: "Unknown"
        status.append("\n[READER_EVENT] Reader disappeared: $name")
        sendStatusEvent("readerDisappeared", "Reader disappeared: $name", mapOf("readerName" to name))
    }

    // ═══════════════════════════════════════════════════════════════════
    // EVENT HANDLER CLASS
    // ═══════════════════════════════════════════════════════════════════

    inner class EventHandler : RfidEventsListener {

        override fun eventReadNotify(e: RfidReadEvents?) {
            try {
                val tags = reader?.Actions?.getReadTags(100)

                tags?.let {
                    if (it.isNotEmpty()) {
                        val tagList = mutableListOf<Map<String, Any>>()

                        for (tag in it) {
                            tagList.add(mapOf(
                                "tagId" to tag.getTagID(),
                                "rssi" to tag.getPeakRSSI(),
                                "antennaId" to tag.getAntennaID(),
                                "count" to tag.getTagSeenCount()
                            ))
                        }

                        status.append("\n[TAG_READ] Read ${it.size} tags")
                        sendTagReadEvent(tagList)
                    }
                }
            } catch (e: Exception) {
                status.append("\n[TAG_READ ERROR] Exception: ${e.message}")
            }
        }

        override fun eventStatusNotify(statusEvent: RfidStatusEvents?) {
            try {
                statusEvent?.let { event ->
                    when (event.StatusEventData.getStatusEventType()) {
                        STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT -> {
                            val pressed = event.StatusEventData.HandheldTriggerEventData
                                .getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED

                            status.append("\n[TRIGGER] ${if (pressed) "PRESSED" else "RELEASED"}")
                            sendStatusEvent(
                                "trigger",
                                if (pressed) "Trigger pressed" else "Trigger released",
                                mapOf("pressed" to pressed)
                            )
                        }
                        STATUS_EVENT_TYPE.DISCONNECTION_EVENT -> {
                            status.append("\n[STATUS] Unexpected disconnection")
                            sendStatusEvent("disconnected", "Reader disconnected unexpectedly")
                        }
                        else -> {
                            status.append("\n[STATUS] Event: ${event.StatusEventData.getStatusEventType()}")
                        }
                    }
                }
            } catch (e: Exception) {
                status.append("\n[STATUS_EVENT ERROR] Exception: ${e.message}")
            }
        }
    }
}

// Android 13+ Context Wrapper to fix SDK crash
class Android13ContextWrapper(base: Context) : ContextWrapper(base) {
    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            super.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            super.registerReceiver(receiver, filter)
        }
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        broadcastPermission: String?,
        scheduler: Handler?
    ): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            super.registerReceiver(receiver, filter, broadcastPermission, scheduler, Context.RECEIVER_EXPORTED)
        } else {
            super.registerReceiver(receiver, filter, broadcastPermission, scheduler)
        }
    }
}