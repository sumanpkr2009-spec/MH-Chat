package com.mh.chat

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var endpointInput: TextInputEditText
    private lateinit var keyInput: TextInputEditText
    private lateinit var modelInput: TextInputEditText
    private lateinit var modelSpinner: Spinner
    private lateinit var loadButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        toolbar.setNavigationOnClickListener { finish() }

        endpointInput = findViewById(R.id.endpointInput)
        keyInput = findViewById(R.id.keyInput)
        modelInput = findViewById(R.id.modelInput)
        modelSpinner = findViewById(R.id.modelSpinner)
        loadButton = findViewById(R.id.loadModelsButton)

        endpointInput.setText(prefs.endpoint)
        keyInput.setText(prefs.apiKey)
        modelInput.setText(prefs.model)

        loadButton.setOnClickListener { loadModels() }
        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener { save() }
    }

    private fun save() {
        prefs.endpoint = endpointInput.text.toString().trim()
        prefs.apiKey = keyInput.text.toString().trim()
        prefs.model = modelInput.text.toString().trim()
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun loadModels() {
        val endpoint = endpointInput.text.toString().trim()
        val key = keyInput.text.toString().trim()
        if (endpoint.isEmpty() || key.isEmpty()) {
            Toast.makeText(this, "Enter the endpoint URL and API key first", Toast.LENGTH_SHORT).show()
            return
        }
        loadButton.isEnabled = false
        loadButton.text = "Loading…"
        lifecycleScope.launch {
            val result = ChatClient.loadModels(endpoint, key)
            loadButton.isEnabled = true
            loadButton.text = "Load models"
            result.fold(
                onSuccess = { ids ->
                    if (ids.isEmpty()) {
                        Toast.makeText(this@SettingsActivity, "No models returned", Toast.LENGTH_SHORT).show()
                    } else {
                        val spinnerAdapter = ArrayAdapter(
                            this@SettingsActivity,
                            android.R.layout.simple_spinner_item,
                            ids
                        )
                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        modelSpinner.adapter = spinnerAdapter
                        modelSpinner.visibility = View.VISIBLE
                        val current = modelInput.text.toString().trim()
                        val idx = ids.indexOf(current)
                        if (idx >= 0) modelSpinner.setSelection(idx)
                        modelSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    modelInput.setText(ids[position])
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {}
                            }
                        Toast.makeText(
                            this@SettingsActivity,
                            "${ids.size} models loaded — pick one",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onFailure = { e ->
                    Toast.makeText(
                        this@SettingsActivity,
                        e.message ?: "Failed to load models",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}
