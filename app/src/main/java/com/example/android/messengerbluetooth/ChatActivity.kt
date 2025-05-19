package com.example.android.messengerbluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity

class ChatActivity : AppCompatActivity() {

    private lateinit var chatService: BluetoothChatService
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var btnSend: Button
    private lateinit var btnBack: Button
    private lateinit var inputMessage: EditText
    private lateinit var chatView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var roleLabel: TextView

    private lateinit var chatKeyHeader: TextView
    private lateinit var chatKeyContainer: LinearLayout
    private lateinit var editChatKey: EditText
    private lateinit var btnChangeChatKey: Button

    private lateinit var chatKey: String
    private val PREFS_NAME = "ChatPrefs"
    private var isServer = false

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        inputMessage = findViewById(R.id.inputMessage)
        chatView = findViewById(R.id.chatView)
        scrollView = findViewById(R.id.chatScroll)
        roleLabel = findViewById(R.id.roleLabel)

        chatKeyHeader = findViewById(R.id.roleLabel)
        chatKeyContainer = findViewById(R.id.chatKeyContainer)
        editChatKey = findViewById(R.id.editChatKey)
        btnChangeChatKey = findViewById(R.id.btnChangeChatKey)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        chatService = BluetoothChatService(bluetoothAdapter, this)

        isServer = intent.getBooleanExtra("is_server", false)
        val deviceAddress = intent.getStringExtra("device_address")
        val localName = bluetoothAdapter.name ?: "Unknown"

        val remoteName = if (deviceAddress != null) {
            bluetoothAdapter.getRemoteDevice(deviceAddress).name ?: deviceAddress
        } else {
            "Unknown"
        }

        chatKey = if (isServer)
            "chat_server_${localName}_client_${remoteName}"
        else
            "chat_server_${remoteName}_client_${localName}"

        chatKeyHeader.text = if (isServer)
            "Tryb: Serwer ($localName)\nPołączono z: $remoteName\nchatKey: $chatKey"
        else
            "Tryb: Klient ($localName)\nPołączono z: $remoteName\nchatKey: $chatKey"

        chatView.text = ""

        if (isServer) {
            chatKeyContainer.visibility = LinearLayout.VISIBLE

            btnChangeChatKey.setOnClickListener {
                val newKey = editChatKey.text.toString().trim()
                if (newKey.isNotEmpty()) {
                    val oldKey = chatKey
                    chatKey = newKey
                    val msg = "Zmieniono nazwę konwersacji na: $chatKey"
                    addMessageToChat("SYSTEM", msg)
                    chatService.sendMessage("KEYCHANGE:$chatKey")

                    //przeniesienie wiad do chatu
                    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val oldMessages = prefs.getString(oldKey, "") ?: ""
                    val newMessages = prefs.getString(chatKey, "") ?: ""
                    prefs.edit().putString(chatKey, oldMessages + newMessages).remove(oldKey).apply()

                    chatKeyHeader.text =
                        "Tryb: Serwer ($localName)\nPołączono z: $remoteName\nchatKey: $chatKey"
                    editChatKey.text.clear()
                } else {
                    Toast.makeText(this, "Nazwa nie może być pusta", Toast.LENGTH_SHORT).show()
                }
            }

            chatService.startServer(
                onMessageReceived = { message ->
                    runOnUiThread {
                        addMessageToChat("ON", message)
                    }
                },
                onClientConnected = { clientName: String ->
                    runOnUiThread {
                        chatKey = "chat_server_${localName}_client_${clientName}"
                        chatKeyHeader.text =
                            "Tryb: Serwer ($localName)\nPołączono z: $clientName\nchatKey: $chatKey"
                        chatView.text = loadChatHistory()
                    }
                }
            )
        } else {
            if (deviceAddress == null) {
                Toast.makeText(this, "Brak adresu urządzenia", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
            chatView.text = loadChatHistory()

            chatService.connectToDevice(
                device,
                onMessageReceived = { message ->
                    runOnUiThread {
                        if (message.startsWith("KEYCHANGE:")) {
                            val newKey = message.removePrefix("KEYCHANGE:")
                            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val oldMessages = prefs.getString(chatKey, "") ?: ""
                            val newMessages = prefs.getString(newKey, "") ?: ""

                            // nadpisanie chatKey
                            prefs.edit()
                                .putString(newKey, oldMessages + newMessages)
                                .remove(chatKey)
                                .apply()

                            chatKey = newKey
                            addMessageToChat("SYSTEM", "Serwer zmienił nazwę rozmowy na: $chatKey")
                            chatKeyHeader.text =
                                "Tryb: Klient ($localName)\nPołączono z: $remoteName\nchatKey: $chatKey"
                        } else {
                            addMessageToChat("ON", message)
                        }
                    }
                },
                onConnectionFailed = {
                    runOnUiThread {
                        Toast.makeText(this, "Nie można połączyć się z serwerem", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            )
        }

        btnSend.setOnClickListener {
            val msg = inputMessage.text.toString()
            if (msg.isNotEmpty()) {
                chatService.sendMessage(msg)
                addMessageToChat("TY", msg)
                inputMessage.text.clear()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun addMessageToChat(sender: String, message: String) {
        val formatted = "$sender: $message\n"
        chatView.append(formatted)
        saveMessage(formatted)
        scrollView.fullScroll(ScrollView.FOCUS_DOWN)
    }

    private fun saveMessage(message: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val old = prefs.getString(chatKey, "") ?: ""
        prefs.edit().putString(chatKey, old + message).apply()
    }

    private fun loadChatHistory(): String {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(chatKey, "") ?: ""
    }

    override fun onDestroy() {
        super.onDestroy()
        chatService.closeConnection()
    }
}
