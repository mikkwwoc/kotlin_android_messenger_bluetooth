package com.example.android.messengerbluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class DeviceListActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var listView: ListView
    private lateinit var devicesList: List<BluetoothDevice>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        listView = findViewById(R.id.deviceList)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Brak uprawnień do Bluetooth", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        devicesList = bluetoothAdapter.bondedDevices.toList()
        val deviceNames = devicesList.map { "${it.name}\n${it.address}" }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = devicesList[position]
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("device_address", device.address)
            intent.putExtra("is_server", false)
            startActivity(intent)
        }
    }
}