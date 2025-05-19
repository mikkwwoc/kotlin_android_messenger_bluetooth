package com.example.android.messengerbluetooth

import android.content.Context
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConversationHistoryActivity : AppCompatActivity() {

    private val PREFS_NAME = "ChatPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_history)

        val chatKey = intent.getStringExtra("chat_key") ?: return
        val chatView: TextView = findViewById(R.id.chatHistoryView)
        val scrollView: ScrollView = findViewById(R.id.chatHistoryScroll)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = prefs.getString(chatKey, "Brak wiadomości.") ?: "Brak wiadomości."

        chatView.text = "[$chatKey]\n\n$history"
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}