package com.hamburg274.piano.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper

data class ConnectedMidiDevice(
    val id: Int,
    val name: String,
    val manufacturer: String,
    val state: String
)

class AndroidMidiController(
    private val context: Context,
    private val onNoteOn: (midi: Int, velocity: Int) -> Unit,
    private val onNoteOff: (midi: Int) -> Unit,
    private val onCC: (cc: Int, value: Int) -> Unit,
    private val onPanic: () -> Unit,
    private val onStateUpdated: (devices: List<ConnectedMidiDevice>, status: String, lastMsg: String) -> Unit
) {
    private val midiManager: MidiManager? =
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
        } else {
            null
        }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val openDevices = mutableMapOf<Int, Pair<MidiDevice, List<MidiOutputPort>>>()
    private val deviceList = mutableMapOf<Int, ConnectedMidiDevice>()
    private var lastPacketMessage = "No MIDI traffic yet"
    private var runningStatus = 0

    val isSupported: Boolean get() = midiManager != null

    private val receiver = object : MidiReceiver() {
        override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
            if (msg == null || count <= 0) return
            parseMidiStream(msg, offset, count)
        }
    }

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            openMidiDevice(device)
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            closeMidiDevice(device.id)
            emitState()
        }
    }

    fun start() {
        val mgr = midiManager
        if (mgr == null) {
            onStateUpdated(emptyList(), "Unsupported", "MIDI hardware feature unavailable")
            return
        }
        mgr.registerDeviceCallback(deviceCallback, mainHandler)
        rescan()
    }

    fun rescan() {
        val mgr = midiManager ?: return
        @Suppress("DEPRECATION")
        val infos = mgr.devices
        infos.forEach { info ->
            if (info.outputPortCount > 0 && !openDevices.containsKey(info.id)) {
                openMidiDevice(info)
            }
        }
        emitState()
    }

    fun stop() {
        midiManager?.unregisterDeviceCallback(deviceCallback)
        openDevices.keys.toList().forEach { closeMidiDevice(it) }
    }

    private fun openMidiDevice(info: MidiDeviceInfo) {
        val mgr = midiManager ?: return
        if (info.outputPortCount <= 0) return

        val props = info.properties
        val name = props.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: props.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            ?: "USB / BLE MIDI Piano"
        val manufacturer = props.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) ?: "MIDI Device"

        mgr.openDevice(info, { device ->
            if (device == null) return@openDevice
            val ports = mutableListOf<MidiOutputPort>()
            for (p in 0 until info.outputPortCount) {
                val port = device.openOutputPort(p)
                if (port != null) {
                    port.connect(receiver)
                    ports.add(port)
                }
            }
            openDevices[info.id] = device to ports
            deviceList[info.id] = ConnectedMidiDevice(
                id = info.id,
                name = name,
                manufacturer = manufacturer,
                state = "Connected"
            )
            emitState()
        }, mainHandler)
    }

    private fun closeMidiDevice(id: Int) {
        openDevices.remove(id)?.let { (device, ports) ->
            ports.forEach {
                try {
                    it.disconnect(receiver)
                    it.close()
                } catch (_: Exception) {
                }
            }
            try {
                device.close()
            } catch (_: Exception) {
            }
        }
        deviceList.remove(id)
    }

    /**
     * Strict MIDI parser handling Running Status, explicit 0x8n Note-Off,
     * velocity-zero 0x9n Note-Off (fixing stuck-note glitches), CC64/66/67, and CC120/123 panic.
     */
    private fun parseMidiStream(data: ByteArray, offset: Int, count: Int) {
        var i = offset
        val end = (offset + count).coerceAtMost(data.size)

        while (i < end) {
            val b = data[i].toInt() and 0xFF
            if ((b and 0x80) != 0) {
                if (b >= 0xF8) {
                    // System Real-Time single byte
                    i++
                    continue
                }
                if (b == 0xF0) {
                    // Skip SysEx
                    i++
                    while (i < end && (data[i].toInt() and 0xFF) != 0xF7) i++
                    i++
                    continue
                }
                runningStatus = b
                i++
            }

            val cmd = runningStatus and 0xF0
            when (cmd) {
                0x90 -> {
                    if (i + 1 >= end) break
                    val note = data[i++].toInt() and 0x7F
                    val vel = data[i++].toInt() and 0x7F
                    if (vel == 0) {
                        onNoteOff(note)
                        lastPacketMessage = "Note Off (vel=0) · #$note"
                    } else {
                        onNoteOn(note, vel)
                        lastPacketMessage = "Note On · #$note vel $vel"
                    }
                    emitState()
                }
                0x80 -> {
                    if (i + 1 >= end) break
                    val note = data[i++].toInt() and 0x7F
                    i++ // release velocity
                    onNoteOff(note)
                    lastPacketMessage = "Note Off · #$note"
                    emitState()
                }
                0xB0 -> {
                    if (i + 1 >= end) break
                    val cc = data[i++].toInt() and 0x7F
                    val value = data[i++].toInt() and 0x7F
                    if (cc == 120 || cc == 123) {
                        onPanic()
                        lastPacketMessage = "MIDI Panic (CC$cc)"
                    } else {
                        onCC(cc, value)
                        lastPacketMessage = "CC$cc · $value"
                    }
                    emitState()
                }
                0xC0, 0xD0 -> {
                    i += 1
                }
                0xA0, 0xE0 -> {
                    i += 2
                }
                else -> {
                    i++
                }
            }
        }
    }

    private fun emitState() {
        val status = when {
            !isSupported -> "Unsupported"
            deviceList.isNotEmpty() -> "Connected"
            else -> "Ready (Waiting for USB/BLE)"
        }
        onStateUpdated(deviceList.values.toList(), status, lastPacketMessage)
    }
}
