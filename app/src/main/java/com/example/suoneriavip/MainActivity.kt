package com.example.suoneriavip

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private lateinit var vipManager: VipContactsManager

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (!allGranted) {
                Toast.makeText(this, "I permessi sono necessari per il funzionamento", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vipManager = VipContactsManager(this)

        checkAndRequestPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VipScreen(
                        vipManager = vipManager,
                        onRequestDndPermission = { requestDndPermission() }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun requestDndPermission() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Permesso 'Non Disturbare' già concesso!", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipScreen(vipManager: VipContactsManager, onRequestDndPermission: () -> Unit) {
    var newNumber by remember { mutableStateOf("") }
    var vipList by remember { mutableStateOf(vipManager.getVipNumbers().toList()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Suoneria VIP - Contatti Prioritari") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Button(
                onClick = onRequestDndPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Autorizza gestione 'Non Disturbare'")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = newNumber,
                onValueChange = { newNumber = it },
                label = { Text("Numero di telefono VIP") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (newNumber.isNotBlank()) {
                        vipManager.addVipNumber(newNumber)
                        vipList = vipManager.getVipNumbers().toList()
                        newNumber = ""
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Aggiungi VIP")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Elenco Contatti VIP:", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(vipList) { number ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = number, style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = {
                            vipManager.removeVipNumber(number)
                            vipList = vipManager.getVipNumbers().toList()
                        }) {
                            Text("🗑️")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
