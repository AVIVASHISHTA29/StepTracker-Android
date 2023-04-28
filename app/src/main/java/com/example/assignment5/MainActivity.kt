package com.example.assignment5


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor
    private lateinit var gyroscopeSensor :Sensor

    private var pressureSensor: Sensor? = null
    private val RArr = FloatArray(9)
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val orientation = FloatArray(3)

    private var stepCount = 0
    private var previousY = 0f
    private var initial = true

    private var xPos = 0f
    private var yPos = 0f

    private var lastPressure: Float = 0f
    private var pressureChange: Float = 0f
    private val pressureThresholdLift = 5f
    private val pressureThresholdStairs = 2f

    private val pressureData = FloatArray(10) // Use a larger window size for more smoothing
    private var pressureDataIndex = 0

    private var lastGyroscopeY = 0f
    private var gyroscopeYChange = 0f
    private val gyroscopeYThreshold = 5f

    private var lastCheckStepCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)

        if(pressureSensor==null){
            Toast.makeText(applicationContext, "No Pressure sensor in this device, using Gyroscope", Toast.LENGTH_SHORT).show()
        }
        val trajectoryView = findViewById<TrajectoryView>(R.id.trajectoryView)
        trajectoryView.post {
            xPos = trajectoryView.width / 2f
            yPos = trajectoryView.height / 2f
        }


        val resetButton: Button = findViewById(R.id.btn_reset)
        resetButton.setOnClickListener {
            reset()
        }
    }

    private fun reset() {
        stepCount = 0
        findViewById<TextView>(R.id.tv_step_count).text = "Steps: 0"
        findViewById<TextView>(R.id.tv_direction).text = "Direction: Unknown"
        findViewById<TrajectoryView>(R.id.trajectoryView).apply {
            path.reset()
            initialize(width / 2f, height / 2f)
            resetZoom()
            invalidate()
        }

    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        pressureSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {

        event?.let {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                    val y = gravity[1]

                    if (!initial) {
                        if (previousY > 0 && y < 0) {
                            stepCount++
                            findViewById<TextView>(R.id.tv_step_count).text = "Steps: $stepCount"

                            val strideLength = 183*0.415f // Replace this with your stride length calculation
                            val deltaX = strideLength * kotlin.math.cos(orientation[0])
                            val deltaY = strideLength * kotlin.math.sin(orientation[0])
                            xPos += deltaX
                            yPos += deltaY

                            findViewById<TrajectoryView>(R.id.trajectoryView).addPoint(deltaX, deltaY)
                        }
                    } else {
                        initial = false
                    }

                    previousY = y
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                    SensorManager.getRotationMatrix(RArr, null, gravity, geomagnetic)
                    SensorManager.getOrientation(RArr, orientation)

                    val azimuth = orientation[0] * (180 / Math.PI).toFloat()
                    val direction = getDirectionFromAzimuth(azimuth)
                    findViewById<TextView>(R.id.tv_direction).text = "Direction: $direction"

                }

                Sensor.TYPE_PRESSURE -> {
                    val pressure = event.values[0]
                    pressureData[pressureDataIndex] = pressure
                    pressureDataIndex = (pressureDataIndex + 1) % pressureData.size

                    val avgPressure = pressureData.average()
                    if (lastPressure != 0f) {
                        pressureChange += kotlin.math.abs(avgPressure.toFloat() - lastPressure)
                    }
                    lastPressure = avgPressure.toFloat()
                }

                Sensor.TYPE_GYROSCOPE ->{
                    val gyroscopeY = event.values[1]
                    gyroscopeYChange += kotlin.math.abs(gyroscopeY - lastGyroscopeY)
                    lastGyroscopeY = gyroscopeY

                    if (gyroscopeYChange > gyroscopeYThreshold) {
                        // Detected stairs or elevator
                        // You can refine this logic to differentiate between stairs and elevators
                        // by analyzing patterns in the gyroscope and accelerometer data
                        Toast.makeText(applicationContext, "In the lift or on stairs ", Toast.LENGTH_SHORT).show()
                        gyroscopeYChange = 0f
                    }
                }

            }
            if (stepCount - lastCheckStepCount >= 5) {
                checkLiftOrStairs()
                lastCheckStepCount = stepCount
            }
        }
    }

    private fun checkLiftOrStairs() {
        if (pressureChange >= pressureThresholdLift) {
            Toast.makeText(applicationContext, "In the lift", Toast.LENGTH_SHORT).show()
        } else if(pressureChange>=pressureThresholdStairs) {
            // The user is likely taking the stairs
            // Do something, e.g., update UI, show a toast, etc.
            Toast.makeText(applicationContext, "On stairs", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this example
    }

    private fun getDirectionFromAzimuth(azimuth: Float): String {
        return when {
            azimuth >= -22.5 && azimuth < 22.5 -> "N"
            azimuth >= 22.5 && azimuth < 67.5 -> "NE"
            azimuth >= 67.5 && azimuth < 112.5 -> "E"
            azimuth >= 112.5 && azimuth < 157.5 -> "SE"
            (azimuth >= 157.5 && azimuth <= 180) || (azimuth >= -180 && azimuth < -157.5) -> "S"
            azimuth >= -157.5 && azimuth < -112.5 -> "SW"
            azimuth >= -112.5 && azimuth < -67.5 -> "W"
            azimuth >= -67.5 && azimuth < -22.5 -> "NW"
            else -> "N/A"
        }
    }
}
