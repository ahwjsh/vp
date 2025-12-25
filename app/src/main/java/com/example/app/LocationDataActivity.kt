package com.example.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellSignalStrengthLte
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors

class LocationDataActivity : AppCompatActivity() {
    //lat lon alt time mcc mnc pci rsrp rsrq rssi

    private lateinit var bGoToMainActivity : Button
    private lateinit var sendData : Button

    private val LOG_TAG: String = "LOCATION_ACTIVITY"

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private lateinit var myFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var telephonyManager: TelephonyManager

    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var time: TextView
    private lateinit var mobCode: TextView
    private lateinit var pci: TextView
    private lateinit var rsrp: TextView
    private lateinit var rsrq: TextView
    private lateinit var rssi: TextView

    private var currentLocation: Location? = null
    private var currentCellInfo: CellInfoLTE? = null
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val executor = Executors.newSingleThreadExecutor()

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val timestamp: String
    )

    data class CellInfoLTE(
        val mcc: String,
        val mnc: String,
        val pci: Int,
        val rsrp: Int,
        val rsrq: Int,
        val rssi: Int
    )

    data class TransmissionData(
        val location: LocationData,
        val cellInfoLte: CellInfoLTE,
        val deviceId: String = Build.MODEL
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_location_data)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bGoToMainActivity = findViewById(R.id.go_to_main_activity)
        sendData = findViewById(R.id.send_data)

        myFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        tvLat = findViewById(R.id.tv_lat)
        tvLon = findViewById(R.id.tv_lon)
        tvAlt = findViewById(R.id.tv_alt)
        time = findViewById(R.id.Time)
        mobCode = findViewById(R.id.mcc_mnc)
        pci = findViewById(R.id.pci)
        rsrp = findViewById(R.id.rsrp)
        rsrq = findViewById(R.id.rsrq)
        rssi = findViewById(R.id.rssi)
    }

    private fun requestPermissions() {
        Log.w(LOG_TAG, "requestPermissions()")
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun updateLocationUI(location: Location) {
        runOnUiThread {
            tvLat.text = String.format("%.6f", location.latitude)
            tvLon.text = String.format("%.6f", location.longitude)
            tvAlt.text = String.format("%.2f", location.altitude)
            time.text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Date())
        }
    }

    private fun updateNetworkDataUI(cellInfo: CellInfoLTE) {
        runOnUiThread {
            mobCode.text = "MCC: ${cellInfo.mcc}, MNC: ${cellInfo.mnc}"
            pci.text = "${cellInfo.pci}"
            rsrp.text = "${cellInfo.rsrp} dBm"
            rsrq.text = "${cellInfo.rsrq} dB"
            rssi.text = "${cellInfo.rssi} dBm"
        }
    }

    private fun getCurrentLocation() {
        if (!checkPermissions()) {
            Toast.makeText(this, "Требуются разрешения", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Включите геолокацию в настройках", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        myFusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
            val location: Location? = task.result
            if (location == null) {
                Toast.makeText(applicationContext, "Проблемы с получением местоположения", Toast.LENGTH_SHORT).show()
            } else {
                currentLocation = location
                updateLocationUI(location)
            }
        }
    }

    private fun getNetworkInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            val networkOperator = telephonyManager.networkOperator
            var mcc = ""
            var mnc = ""

            if (networkOperator.isNotEmpty() && networkOperator.length >= 5) {
                mcc = networkOperator.substring(0, 3)
                mnc = networkOperator.substring(3)
            }
            val cellInfoList: List<CellInfo>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                telephonyManager.getAllCellInfo()
            } else {
                telephonyManager.allCellInfo
            }

            var bestCellInfoLte: CellInfoLTE? = null
            var bestRsrp = Int.MIN_VALUE

            cellInfoList?.forEach { cellInfo ->
                if (cellInfo is CellInfoLte) {
                    val cellIdentity = cellInfo.cellIdentity
                    val cellSignal = cellInfo.cellSignalStrength

                    if (cellSignal is CellSignalStrengthLte) {
                        val currentRsrp = cellSignal.rsrp

                        if (currentRsrp > bestRsrp) {
                            bestRsrp = currentRsrp

                            bestCellInfoLte = CellInfoLTE(
                                mcc = mcc,
                                mnc = mnc,
                                pci = cellIdentity.pci,
                                rsrp = cellSignal.rsrp,
                                rsrq = cellSignal.rsrq,
                                rssi = cellSignal.rssi
                            )
                        }
                    }
                }
            }

            bestCellInfoLte?.let { cellInfo ->
                currentCellInfo = cellInfo
                updateNetworkDataUI(cellInfo)
            }

        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "SecurityException: ${e.message}")
            Toast.makeText(this, "Недостаточно прав для получения информации о сети", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting network info: ${e.message}")
        }
    }

    private fun jsonData(): String? {
        val location = currentLocation
        val cellInfo = currentCellInfo
        if (location == null || cellInfo == null) {
            Toast.makeText(this, "no location data", Toast.LENGTH_SHORT).show()
            return null
        }
        val locationData = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Date(location.time))
        )
        val transmissionData = TransmissionData(
            location = locationData,
            cellInfoLte = cellInfo
        )
        return gson.toJson(transmissionData)
    }

    fun startClient() {
        val jsonData = jsonData()
        if (jsonData == null) {
            Toast.makeText(this, "no data to send", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d(LOG_TAG, "Data to send:\n$jsonData")
        Toast.makeText(this, "sending data...", Toast.LENGTH_SHORT).show()
        executor.execute {
            try {
                val context = ZContext()
                val socket = context.createSocket(SocketType.REQ)
                socket.connect("tcp://localhost:2222")
                socket.send(jsonData.toByteArray(ZMQ.CHARSET), 0)
                Log.d(LOG_TAG, "[CLIENT] Sent data to server")
                val reply = socket.recvStr(0)
                Log.d(LOG_TAG, "[CLIENT] Server reply: $reply")

                runOnUiThread {
                    Toast.makeText(this, "data send: $reply", Toast.LENGTH_SHORT).show()
                }
                socket.close()
                context.close()
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error sending data: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "send error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Permission granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
                getNetworkInfo()
            } else {
                Toast.makeText(applicationContext, "Denied by user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bGoToMainActivity.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        sendData.setOnClickListener {
            startClient()
        }
        getCurrentLocation()
        getNetworkInfo()
    }
}
