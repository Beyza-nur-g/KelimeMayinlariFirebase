package com.example.kelime_mayinlari_firebase.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitenOyunlarScreen(
    navController: NavController,
    kullaniciAdi: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
)
 {
    var oyunlar by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // Pair<OyunId, Rakip>
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val snapshot = firestore.collection("games")
            .whereArrayContains("players", kullaniciAdi)
            .get()
            .await()

        oyunlar = snapshot.documents.mapNotNull { doc ->
            val status = doc.getString("starter")
            val id = doc.id
            val players = doc.get("players") as? List<*> ?: return@mapNotNull null
            val rakip = players.firstOrNull { it != kullaniciAdi }?.toString() ?: return@mapNotNull null
            if (status == "bitti") Pair(id, rakip) else null
        }
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Biten Oyunlar") }) }) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (oyunlar.isEmpty()) {
                Text("Henüz bitmiş oyun bulunamadı.", style = MaterialTheme.typography.bodyLarge)
            } else {
                oyunlar.forEach { (oyunId, rakip) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                navController.navigate("map_view_only/$oyunId")

                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🧑 Rakip: $rakip", style = MaterialTheme.typography.bodyLarge)
                            Text("🆔 Oyun ID: $oyunId", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
