package com.example.suoneriavip

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var vipManager: VipContactsManager

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (!allGranted) {
                Toast.makeText(this, "Alcuni permessi sono necessari per il corretto funzionamento", Toast.LENGTH_LONG).show()
            }
        }

    // Launcher per aprire la rubrica nativa e selezionare un contatto
    val pickContactLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri ->
        contactUri?.let { uri ->
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                    val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    if (idIndex != -1 && hasPhoneIndex != -1) {
                        val id = it.getString(idIndex)
                        val hasPhone = it.getInt(hasPhoneIndex)

                        if (hasPhone > 0) {
                            val phonesCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(id),
                                null
                            )
                            phonesCursor?.use { pCursor ->
                                if (pCursor.moveToFirst()) {
                                    val numberIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    val nameIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                                    if (numberIndex != -1) {
                                        val number = pCursor.getString(numberIndex)
                                        val name = if (nameIndex != -1) pCursor.getString(nameIndex) else "Contatto"

                                        // Aggiunge il VIP formattato o pulito
                                        vipManager.addVipNumber(number)
                                        Toast.makeText(this, "Aggiunto VIP: $name", Toast.LENGTH_SHORT).show()
                                        // Ricarica la UI ricreando l'activity o gestendo lo stato
                                        recreate()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(this, "Questo contatto non ha un numero di telefono", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
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
                    MainScreen(
                        vipManager = vipManager,
                        context = this,
                        onPickContact = {
                            pickContactLauncher.launch(null)
                        },
                        onRequestDndPermission = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            startActivity(intent)
                        },
                        onRequestAppPermissions = {
                            checkAndRequestPermissions()
                        }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vipManager: VipContactsManager,
    context: Context,
    onPickContact: () -> Unit,
    onRequestDndPermission: () -> Unit,
    onRequestAppPermissions: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var vipList by remember { mutableStateOf(vipManager.getVipNumbers().toList()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Suoneria VIP") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Text("⭐") },
                    label = { Text("Contatti VIP") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Text("🔒") },
                    label = { Text("Permessi") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedTab == 0) {
                VipContactsView(
                    vipList = vipList,
                    onPickContact = onPickContact,
                    onRemoveVip = { number ->
                        vipManager.removeVipNumber(number)
                        vipList = vipManager.getVipNumbers().toList()
                    }
                )
            } else {
                PermissionsView(
                    context = context,
                    onRequestDndPermission = onRequestDndPermission,
                    onRequestAppPermissions = onRequestAppPermissions
                )
            }
        }
    }
}

@Composable
fun VipContactsView(
    vipList: List<String>,
    onPickContact: () -> Unit,
    onRemoveVip: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = onPickContact,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("➕ Aggiungi Contatto dalla Rubrica")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Elenco Numeri VIP:", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        if (vipList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun contatto VIP aggiunto.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(vipList) { number ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = number, style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { onRemoveVip(number) }) {
                                Text("🗑️")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionsView(
    context: Context,
    onRequestDndPermission: () -> Unit,
    onRequestAppPermissions: () -> Unit
) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val isDndGranted = notificationManager.isNotificationPolicyAccessGranted

    val hasReadPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    val hasReadContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedNameOrVerticalIfNeeded() // Standard spacing
    ) {
        Text("Stato dei Permessi", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Per funzionare correttamente, l'app necessita delle seguenti autorizzazioni di sistema:", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        PermissionCard(
            title = "Modalità 'Non Disturbare'",
            description = "Consente all'app di suonare anche quando il telefono è in modalità Non Disturbare.",
            isGranted = isDndGranted,
            buttonText = "Configura",
            onClick = onRequestDndPermission
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            title = "Stato Telefono e Chiamate",
            description = "Necessario per intercettare i numeri in arrivo in tempo reale.",
            isGranted = hasReadPhoneState,
            buttonText = "Richiedi",
            onClick = onRequestAppPermissions
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            title = "Accesso ai Contatti",
            description = "Permette di selezionare i numeri direttamente dalla rubrica del telefono.",
            isGranted = hasReadContacts,
            buttonText = "Richiedi",
            onClick = onRequestAppPermissions
        )
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = if (isGranted) "✅ Concesso" else "❌ Mancante", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall)

            if (!isGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClick, modifier = Modifier.align(Alignment.End)) {
                    Text(buttonText)
                }
            }
        }
    }
}
