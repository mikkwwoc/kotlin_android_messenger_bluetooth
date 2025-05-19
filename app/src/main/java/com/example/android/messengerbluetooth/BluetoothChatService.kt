package com.example.android.messengerbluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread
import android.Manifest
import android.content.Context

class BluetoothChatService(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) {

    companion object {
        private const val APP_NAME = "Messenger"
        private val MY_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    }

    private var serverSocket: BluetoothServerSocket? = null
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val handler = Handler(Looper.getMainLooper())

    fun startServer(
        onMessageReceived: (String) -> Unit,
        onClientConnected: (String) -> Unit
    ) {
        thread {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)
                    socket = serverSocket!!.accept() // blokujące
                    serverSocket?.close()

                    val remoteDevice = socket!!.remoteDevice
                    val remoteDeviceName = remoteDevice.name ?: remoteDevice.address

                    handler.post {
                        onClientConnected(remoteDeviceName)
                    }
                    manageConnection(socket!!, onMessageReceived)
                } else {
                    Log.e("BluetoothChat", "Brak uprawnień do nasłuchu Bluetooth")
                }
            } catch (se: SecurityException) {
                Log.e("BluetoothChat", "SecurityException – brak uprawnień BLUETOOTH_CONNECT", se)
            } catch (e: IOException) {
                Log.e("BluetoothChat", "Błąd serwera Bluetooth", e)
            }
        }
    }
    fun connectToDevice(
        device: BluetoothDevice,
        onMessageReceived: (String) -> Unit,
        onConnectionFailed: () -> Unit
    ) {
        thread {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    socket = device.createRfcommSocketToServiceRecord(MY_UUID)
                    bluetoothAdapter.cancelDiscovery()
                    socket!!.connect()
                    manageConnection(socket!!, onMessageReceived)
                } else {
                    Log.e("BluetoothChat", "Brak uprawnienia BLUETOOTH_CONNECT")
                }
            } catch (se: SecurityException) {
                Log.e("BluetoothChat", "SecurityException – brak uprawnień do Bluetooth", se)
            } catch (e: IOException) {
                Log.e("BluetoothChat", "Błąd połączenia z urządzeniem", e)
                try {
                    socket?.close()
                } catch (closeException: IOException) {
                    Log.e("BluetoothChat", "Nie udało się zamknąć socketu", closeException)
                }
            }
        }
    }

    private fun manageConnection(socket: BluetoothSocket, onMessageReceived: (String) -> Unit) {
        inputStream = socket.inputStream
        outputStream = socket.outputStream

        thread {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    bytes = inputStream!!.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    handler.post { onMessageReceived(incomingMessage) }
                } catch (e: IOException) {
                    Log.e("BluetoothChat", "Disconnected", e)
                    break
                }
            }
        }
    }

    fun sendMessage(message: String) {
        thread {
            try {
                outputStream?.write(message.toByteArray())
            } catch (e: IOException) {
                Log.e("BluetoothChat", "Error sending message", e)
            }
        }
    }

    fun closeConnection() {
        try {
            socket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothChat", "Error closing connection", e)
        }
    }
}