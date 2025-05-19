package com.example.android.messengerbluetooth


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ConversationListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private val PREFS_NAME = "ChatPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_list)

        listView = findViewById(R.id.conversationListView)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val conversationKeys = prefs.all.keys.toList()

        if (conversationKeys.isEmpty()) {
            Toast.makeText(this, "Brak zapisanych rozmów", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, conversationKeys)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val key = conversationKeys[position]
            val intent = Intent(this, ConversationHistoryActivity::class.java)
            intent.putExtra("chat_key", key)
            startActivity(intent)
        }
    }
}