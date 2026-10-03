package com.mh.chat

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: ChatAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var input: EditText

    /** user + assistant messages only; sent to the API. Capped at 40. */
    private val conversation = mutableListOf<ChatMessage>()

    /** Everything shown on screen, including system notices and the thinking indicator. */
    private val display = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        adapter = ChatAdapter(display)
        recycler = findViewById(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recycler.adapter = adapter

        input = findViewById(R.id.input)
        findViewById<MaterialButton>(R.id.sendButton).setOnClickListener { send() }

        addSystem("Welcome to MH Chat.\nOpen Settings (top-right menu) to set your endpoint URL, API key and model.")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                clearChat()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearChat() {
        conversation.clear()
        display.clear()
        adapter.notifyDataSetChanged()
        addSystem("Chat cleared.")
    }

    private fun addSystem(text: String) {
        display.add(ChatMessage("system", text))
        adapter.notifyItemInserted(display.size - 1)
        recycler.scrollToPosition(display.size - 1)
    }

    private fun send() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        val endpoint = prefs.endpoint.trim()
        val key = prefs.apiKey.trim()
        val model = prefs.model.trim()
        if (endpoint.isEmpty() || key.isEmpty() || model.isEmpty()) {
            addSystem("Please open Settings (top-right menu) and fill in the endpoint URL, API key and model first.")
            return
        }

        input.text.clear()

        val userMsg = ChatMessage("user", text)
        conversation.add(userMsg)
        capConversation()
        display.add(userMsg)
        adapter.notifyItemInserted(display.size - 1)

        val thinking = ChatMessage("assistant", "Thinking…", thinking = true)
        display.add(thinking)
        adapter.notifyItemInserted(display.size - 1)
        recycler.scrollToPosition(display.size - 1)

        lifecycleScope.launch {
            val result = ChatClient.chat(endpoint, key, model, conversation.toList())
            display.remove(thinking)
            result.fold(
                onSuccess = { reply ->
                    val msg = ChatMessage("assistant", reply)
                    conversation.add(msg)
                    capConversation()
                    display.add(msg)
                },
                onFailure = { e ->
                    display.add(ChatMessage("system", e.message ?: "Request failed."))
                }
            )
            adapter.notifyDataSetChanged()
            recycler.scrollToPosition(display.size - 1)
        }
    }

    private fun capConversation() {
        while (conversation.size > 40) conversation.removeAt(0)
    }
}
