package com.example.cane

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothScanner: BluetoothLeScanner

    private val deviceNameToConnect = "ESP32_Cane"
    private var esp32Device: BluetoothDevice? = null
    private var gattConnection: BluetoothGatt? = null

    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner

        requestBLEPermissions()
    }

    private fun requestBLEPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
        } else {
            startBLEScan()
        }
    }

    private fun startBLEScan() {
        showToast("Scanning for ESP32...")

        bluetoothScanner.startScan(object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    if (device.name == deviceNameToConnect) {
                        showToast("ESP32 found! Connecting...")
                        esp32Device = device
                        bluetoothScanner.stopScan(this)
                        connectToESP32(device)
                    }
                }
            }
        })
    }

    private fun connectToESP32(device: BluetoothDevice) {
        gattConnection = device.connectGatt(this, false, object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    showToast("Connected! Discovering services...")
                    gatt?.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                showToast("Services discovered, enabling notifications...")

                val service = gatt?.services?.firstOrNull()
                val characteristic = service?.characteristics?.firstOrNull()

                characteristic?.let {
                    gatt.setCharacteristicNotification(it, true)
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                showToast("Obstacle detected!")
            }
        })
    }

    private fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
