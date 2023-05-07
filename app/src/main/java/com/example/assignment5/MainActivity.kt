package com.example.assignment5

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

    private val RArr = FloatArray(9)
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val orientation = FloatArray(3)

    private var stepCount = 0
    private var previousY = 0f
    private var previousZ = 0f
    private var initial = true

    private var lastAccelMagnitude =0f

    private var xPos = 0f
    private var yPos = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)


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
    override fun onDestroy() {
        super.onDestroy()

    }
    private fun reset() {
        stepCount = 0
        findViewById<TextView>(R.id.tv_step_count).text = "Steps: 0"
        findViewById<TextView>(R.id.tv_direction).text = "Direction: Unknown"
        findViewById<TextView>(R.id.tv_status).text = "Status: Normal"
        findViewById<TextView>(R.id.tv_distance).text = "Distance: 0cm"
        findViewById<TextView>(R.id.tv_displacement).text = "Displacement: 0cm"
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

                            val strideLength = 185*0.415f // Replace this with your stride length calculation
                            val deltaX = strideLength * kotlin.math.cos(orientation[0])
                            val deltaY = strideLength * kotlin.math.sin(orientation[0])
                            xPos += deltaX
                            yPos += deltaY

                            findViewById<TrajectoryView>(R.id.trajectoryView).addPoint(deltaX, deltaY)
                            val distance =  findViewById<TrajectoryView>(R.id.trajectoryView).calculateDistance()
                            val displacement =  findViewById<TrajectoryView>(R.id.trajectoryView).calculateDisplacement()
                            findViewById<TextView>(R.id.tv_distance).text = "Distance: ${distance/3} cm"
                            findViewById<TextView>(R.id.tv_displacement).text = "Displacement: ${displacement/3} cm"
                        }

                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        val accelMagnitude = kotlin.math.sqrt(x * x + y * y + z * z)
                        val accelChange = kotlin.math.abs(accelMagnitude - lastAccelMagnitude)


                        if (accelChange > 7f) {
                                findViewById<TextView>(R.id.tv_status).text = "Status: On Stairs"
                        }

                        else if(accelChange<=7f){
                            findViewById<TextView>(R.id.tv_status).text = "Status: Normal"
                        }

                        lastAccelMagnitude = accelMagnitude

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
//                    to check if inside lift
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val magnetometerReading = kotlin.math.sqrt(x * x + y * y + z * z)
                    if(magnetometerReading<=27){
                        findViewById<TextView>(R.id.tv_status).text = "Status: In Lift"
                        Toast.makeText(applicationContext, "In Lift", Toast.LENGTH_LONG).show()
                    }
                    else{
                        findViewById<TextView>(R.id.tv_status).text = "Status: Normal"
                    }

                    findViewById<TextView>(R.id.tv_direction).text = "Direction: $direction"

                }



            }

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
