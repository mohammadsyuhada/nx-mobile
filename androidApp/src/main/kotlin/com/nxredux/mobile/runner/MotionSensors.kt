package com.nxredux.mobile.runner

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * The phone's accelerometer and gyroscope for the Wii's motion controls, turned into
 * Dolphin's Wiimote frame ([WiimoteFrame]) and handed to [onValues] in g and rad/s. Listens only between [start] and
 * [stop] (GameActivity stops it for pause, the menu and the background). [recenter] takes the next gravity reading as level.
 * [startN3ds] listens for a 3DS game instead: the console's frame ([N3dsFrame]), with no level.
 */
class MotionSensors(context: Context, private val onValues: (accelG: Vec3, gyro: Vec3) -> Unit) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel: Sensor? = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyro: Sensor? = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    val hasAccel: Boolean get() = accel != null
    val hasGyro: Boolean get() = gyro != null
    /** Both are needed for Motion Controls (without them there is no button and no row). */
    val hasSensors: Boolean get() = hasAccel && hasGyro

    private var rotation: () -> Int = { 0 }
    private var pad: () -> WiiPad = { WiiPad.SIDEWAYS }
    private var n3ds = false
    private var level = WiimoteFrame.Level.NONE
    private var recenterPending = true
    private var lastAccel = Vec3(0f, 1f, 0f)   // display frame, g
    private var lastGyro = Vec3(0f, 0f, 0f)    // display frame, rad/s
    private var running = false

    fun start(rotation: () -> Int, pad: () -> WiiPad) {
        if (running || !hasSensors) return
        this.rotation = rotation
        this.pad = pad
        n3ds = false
        listen()
    }

    /** 3DS: the accelerometer alone is enough (tilt; the gyro reads still), as Azahar asks for each on its own. */
    fun startN3ds(rotation: () -> Int) {
        if (running || !hasAccel) return
        this.rotation = rotation
        n3ds = true
        listen()
    }

    private fun listen() {
        running = true
        recenterPending = true
        manager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        if (gyro != null) manager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!running) return
        running = false
        manager.unregisterListener(this)
    }

    fun recenter() { recenterPending = true }

    override fun onSensorChanged(event: SensorEvent) {
        val raw = Vec3(event.values[0], event.values[1], event.values[2])
        if (n3ds) return onN3ds(event.sensor.type, raw)
        val d = WiimoteFrame.display(raw, rotation())
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccel = Vec3(d.x / SensorManager.GRAVITY_EARTH, d.y / SensorManager.GRAVITY_EARTH, d.z / SensorManager.GRAVITY_EARTH)
                if (recenterPending) { level = WiimoteFrame.Level.capture(lastAccel); recenterPending = false }
            }
            Sensor.TYPE_GYROSCOPE -> lastGyro = d
            else -> return
        }
        val p = pad()
        onValues(WiimoteFrame.forPad(level.apply(lastAccel), p), WiimoteFrame.forPad(level.apply(lastGyro), p))
    }

    /** 3DS: straight from the phone's axes ([N3dsFrame]); lastAccel/lastGyro hold them in the phone's frame here. */
    private fun onN3ds(type: Int, raw: Vec3) {
        when (type) {
            Sensor.TYPE_ACCELEROMETER -> lastAccel = Vec3(raw.x / SensorManager.GRAVITY_EARTH, raw.y / SensorManager.GRAVITY_EARTH, raw.z / SensorManager.GRAVITY_EARTH)
            Sensor.TYPE_GYROSCOPE -> lastGyro = raw
            else -> return
        }
        val r = rotation()
        onValues(N3dsFrame.accel(lastAccel, r), N3dsFrame.gyro(lastGyro, r))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
