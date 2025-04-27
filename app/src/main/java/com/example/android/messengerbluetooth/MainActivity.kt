package com.example.android.messengerbluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter

    // Rejestracja requestu o włączenie Bluetooth (jeśli wyłączony)
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Bluetooth został włączony!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth NIE został włączony!", Toast.LENGTH_SHORT).show()
        }
    }

    // Sprawdzanie uprawnień do lokalizacji
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Prośba o uprawnienia do lokalizacji
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    // Stała na kod żądania uprawnienia
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Sprawdzamy uprawnienie do lokalizacji
        if (hasLocationPermission()) {
            setupBluetooth()
        } else {
            requestLocationPermission()
        }

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            if (hasLocationPermission()) {
                showPairedDevices()
            } else {
                Toast.makeText(this, "Brak uprawnień do lokalizacji, aby wyświetlić urządzenia Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Ustawienie Bluetooth i wyświetlanie sparowanych urządzeń
    private fun setupBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    // Wyświetlenie sparowanych urządzeń Bluetooth
    private fun showPairedDevices() {
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth jest wyłączony. Proszę włączyć Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // Sprawdzamy, czy mamy uprawnienie do lokalizacji
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices

            if (pairedDevices.isEmpty()) {
                Toast.makeText(this, "Brak sparowanych urządzeń!", Toast.LENGTH_SHORT).show()
            } else {
                val deviceNames = pairedDevices.map { it.name ?: it.address }
                val deviceList = pairedDevices.toList()

                // Pokazujemy listę sparowanych urządzeń
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Wybierz urządzenie")
                builder.setItems(deviceNames.toTypedArray()) { _, which ->
                    val selectedDevice = deviceList[which]
                    Toast.makeText(this, "Wybrano: ${selectedDevice.name}", Toast.LENGTH_SHORT).show()
                }
                builder.show()
            }
        } else {
            requestLocationPermission()
        }
    }

    // Obsługa odpowiedzi na prośbę o uprawnienia
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Użytkownik zaakceptował uprawnienie, możemy używać Bluetooth
                setupBluetooth()
            } else {
                Toast.makeText(this, "Brak zgody na lokalizację", Toast.LENGTH_SHORT).show()
            }
        }
    }
}