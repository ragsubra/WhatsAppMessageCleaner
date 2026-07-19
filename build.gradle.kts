package com.example.wacleaner

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.wacleaner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var store: RuleStore
    private var rules = mutableListOf<CleanerRule>()
    private var selectedRuleId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = RuleStore(this)
        rules = store.loadRules()

        binding.addRuleButton.setOnClickListener { addRule() }
        binding.accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.disarmButton.setOnClickListener {
            store.disarm()
            store.setStatus("Stopped by user")
            refreshStatus()
        }
        binding.armButton.setOnClickListener { confirmAndArm() }
        renderRules()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun addRule() {
        val group = binding.groupInput.text?.toString()?.trim().orEmpty()
        val users = binding.usersInput.text?.toString().orEmpty()
            .split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (group.isBlank() || users.isEmpty()) {
            Toast.makeText(this, "Enter a group and at least one user", Toast.LENGTH_SHORT).show()
            return
        }
        val rule = CleanerRule(groupName = group, userNames = users)
        rules.add(rule)
        selectedRuleId = rule.id
        store.saveRules(rules)
        binding.groupInput.setText("")
        binding.usersInput.setText("")
        renderRules()
    }

    private fun renderRules() {
        binding.rulesContainer.removeAllViews()
        rules.forEach { rule ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
            }
            val selector = CheckBox(this).apply {
                text = "${rule.groupName} → ${rule.userNames.joinToString()}"
                isChecked = selectedRuleId == rule.id
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedRuleId = rule.id
                        renderRules()
                    } else if (selectedRuleId == rule.id) selectedRuleId = null
                }
            }
            val delete = Button(this).apply {
                text = "Remove"
                setOnClickListener {
                    rules.removeAll { it.id == rule.id }
                    if (selectedRuleId == rule.id) selectedRuleId = null
                    store.saveRules(rules)
                    renderRules()
                }
            }
            row.addView(selector)
            row.addView(delete)
            binding.rulesContainer.addView(row)
        }
        if (rules.isEmpty()) {
            binding.rulesContainer.addView(TextView(this).apply { text = "No rules yet." })
        }
    }

    private fun confirmAndArm() {
        val rule = rules.firstOrNull { it.id == selectedRuleId }
        if (rule == null) {
            Toast.makeText(this, "Select a saved rule", Toast.LENGTH_SHORT).show()
            return
        }
        val deleteMode = binding.deleteModeSwitch.isChecked
        val action = if (deleteMode) "DELETE matching visible messages from your copy" else "PREVIEW matching visible messages"
        AlertDialog.Builder(this)
            .setTitle(if (deleteMode) "Confirm delete mode" else "Start preview")
            .setMessage("Rule: ${rule.groupName}\nUsers: ${rule.userNames.joinToString()}\n\nThe app will $action. Open the exact group manually. Stop immediately if the detected title is wrong.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Arm") { _, _ ->
                store.arm(rule.id, deleteMode)
                store.setStatus("Armed for '${rule.groupName}' (${if (deleteMode) "DELETE" else "PREVIEW"})")
                openWhatsApp()
                refreshStatus()
            }.show()
    }

    private fun openWhatsApp() {
        val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            ?: packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
        if (intent == null) Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_LONG).show()
        else startActivity(intent)
    }

    private fun refreshStatus() {
        binding.statusText.text = "Status: ${store.status()}\nArmed: ${store.isArmed()}"
    }
}
