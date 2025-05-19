package com.example.android.messengerbluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1001
    private lateinit var chatService: BluetoothChatService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkBluetoothPermissions()
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            } else {
                initializeBluetooth()
            }
        } else {
            initializeBluetooth()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun initializeBluetooth() {
        val btnConnect: Button = findViewById(R.id.btnConnect)
        btnConnect.setOnClickListener {
            val intent = Intent(this, DeviceListActivity::class.java)
            startActivity(intent)
        }
        val btnStartServer: Button = findViewById(R.id.btnStartServer)
        btnStartServer.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("is_server", true)
            startActivity(intent)
        }
        val btnShowHistory: Button = findViewById(R.id.btnShowHistory)
        btnShowHistory.setOnClickListener {
            startActivity(Intent(this, ConversationListActivity::class.java))
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        chatService = BluetoothChatService(bluetoothAdapter, this)

        val btnListDevices: Button = findViewById(R.id.btnListDevices)
        val txtDevices: TextView = findViewById(R.id.txtDevices)

        btnListDevices.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                val devices = bluetoothAdapter.bondedDevices
                val list = devices.joinToString("\n") { "${it.name} - ${it.address}" }
                txtDevices.text = "Sparowane urządzenia:\n$list"
            } else {
                Toast.makeText(this, "Brak dostępu do Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}