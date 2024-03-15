package com.example.myapplication

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    var isAllowed:Boolean = false
    var isScanning:Boolean = false

    val REQUEST_CODE = 1000;

    val device_name = "Cellstar 210-7C"

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var bleGatt: BluetoothGatt? = null

    private val filters: MutableList<ScanFilter> = ArrayList()
    val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()
    var scanResults: ArrayList<BluetoothDevice>? = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val permisison_btn = findViewById<TextView>(R.id.permission_btn)
        val scan_btn = findViewById<TextView>(R.id.scan_btn)

        setFiltering()

        permisison_btn.setOnClickListener{
            checkPermission()
        }

        scan_btn.setOnClickListener{
            settingBluetooth()
        }
    }

    private fun Hello() {
        System.out.println("hello")
    }

    override fun onResume() {
        super.onResume()

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish()
        }
    }

    @SuppressLint("InlinedApi")
    private fun checkPermission() {
        val requiredPermission = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
        val rejectPermissionArrays = ArrayList<String>()

        requestPermissionLauncher.launch(requiredPermission)

        for (permission in requiredPermission) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                System.out.println("DENIEDDDDD")
                isAllowed = false;
                rejectPermissionArrays.add(permission)
            }
        }

        if (rejectPermissionArrays.isNotEmpty()) {
            val array = arrayOfNulls<String>(rejectPermissionArrays.size)
            ActivityCompat.requestPermissions(
                this,
                rejectPermissionArrays.toArray(array),
                REQUEST_CODE
            )
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        for (result in results.values) {
            isAllowed = result
        }
        if (isAllowed) {
            settingBluetooth()
        } else {
            showWarning()
        }
    }

    private fun settingBluetooth() {
        if (isAllowed) {

            bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

            if (!bluetoothAdapter.isEnabled) {
                Toast.makeText(this, "블루투스가 비활성화 상태입니다.", Toast.LENGTH_SHORT).show()
            } else {
                setFiltering()
                scanDevice(true)
            }
        } else {
            showWarning()
        }
    }

    private fun setFiltering() {
        val scanFilter: ScanFilter = ScanFilter.Builder().setDeviceName(device_name).build()
        filters.add(scanFilter)
    }

    private fun showWarning() {
        Toast.makeText(this, "It's Warninggg !!", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun scanDevice(enable: Boolean) {
        when (enable) {
            true -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    isScanning = false
                    bluetoothLeScanner.stopScan(mScanCallback)
                }, 1 * 60 * 1000)
                isScanning = true;
                //bluetoothLeScanner.startScan(filters, settings, mScanCallback)
                bluetoothLeScanner.startScan(mScanCallback)
            }
            else -> {
                isScanning = false;
                bluetoothLeScanner.stopScan(mScanCallback)
            }
        }
    }

    private val mScanCallback = object: ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "onBatchScanResults : $result")
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d(TAG, "onBatchScanResults: $results")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "onScanResult errorCode: $errorCode")
        }

        private fun addScanResult(result: ScanResult) {
            val device = result.device
            val deviceAddress = device.address
            //val deviceName = device.name

            for (dev in scanResults!!) {
                if (dev.address == deviceAddress) return
            }

            scanResults?.add(result.device)
            System.out.println("add scanned device: $deviceAddress")
        }
    }

    private val mGattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer()
                return
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer()
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                System.out.println("Connected")
                Log.d(TAG, "Connected to the GATT server")
                //gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer()
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)


            // check if the discovery failed
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Device service discovery failed, status: $status")
                return
            }

            // log for successful discovery
            Log.d(TAG, "Services discovery is successful")


        }
    }

    fun disconnectGattServer() {
        Log.d(TAG, "Closing Gatt connection")
        // disconnect and close the gatt
        if (bleGatt != null) {
            //bleGatt!!.disconnect()
            //bleGatt!!.close()
        }
    }
}