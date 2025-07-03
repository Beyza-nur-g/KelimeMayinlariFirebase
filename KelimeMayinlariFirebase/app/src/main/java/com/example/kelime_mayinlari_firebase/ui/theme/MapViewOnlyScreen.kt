package com.example.kelime_mayinlari_firebase.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.kelime_mayinlari_firebase.viewModel.Tile
import com.example.kelime_mayinlari_firebase.viewModel.TileType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MapViewOnlyScreen(
    oyunId: String,
    navController: NavController
) {
    var board by remember { mutableStateOf(emptyList<MutableList<Tile>>()) }
    var loading by remember { mutableStateOf(true) }

    var oyuncuAdi by remember { mutableStateOf("") }
    var rakipAdi by remember { mutableStateOf("") }
    var oyuncuPuani by remember { mutableStateOf(0) }
    var rakipPuani by remember { mutableStateOf(0) }
    var kazanan by remember { mutableStateOf<String?>(null) }

    var oyunSuresi by remember { mutableStateOf(0) }

    // Tahtayı ve verileri başlat
    LaunchedEffect(Unit) {
        val emptyBoard = List(15) { y ->
            MutableList(15) { x ->
                Tile(x, y, type = TileType.NORMAL)
            }
        }.toMutableList()
        val firestore = FirebaseFirestore.getInstance()

        // Oyun bilgilerini al
        val gameDoc = firestore.collection("games").document(oyunId).get().await()

        val players = gameDoc.get("players") as? List<*> ?: emptyList<Any?>()

        if (players.size == 2) {
            oyuncuAdi = players[0].toString()
            rakipAdi = players[1].toString()
        }

        val scores = gameDoc.get("scores") as? Map<*, *>
        oyuncuPuani = (scores?.get(oyuncuAdi) as? Long)?.toInt() ?: 0
        rakipPuani = (scores?.get(rakipAdi) as? Long)?.toInt() ?: 0

        kazanan = gameDoc.getString("winner")
        oyunSuresi = (gameDoc.getLong("sure") ?: 0).toInt()

        // Tüm hamleleri uygula
        val snapshot = firestore.collection("games").document(oyunId)
            .collection("hamleler").get().await()

        for (doc in snapshot.documents) {
            val letters = doc.get("letters") as? List<Map<String, Any>> ?: continue
            for (harfData in letters) {
                val x = (harfData["x"] as Long).toInt()
                val y = (harfData["y"] as Long).toInt()
                val letter = (harfData["letter"] as String).first()
                emptyBoard[y][x] = emptyBoard[y][x].copy(letter = letter)
            }
        }

        board = emptyBoard
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗺️ Biten Oyun Görüntüleme") }
            )
        }
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .padding(padding)
            ) {

                // 🧾 Oyun Özeti
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("👤 Oyuncu: $oyuncuAdi - Puan: $oyuncuPuani")
                        Text("🤖 Rakip: $rakipAdi - Puan: $rakipPuani")
                        Text("🏆 Kazanan: ${kazanan ?: "Berabere"}")
                        Text("⏱️ Oyun Süresi: $oyunSuresi sn")
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(15),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    items(board.flatten()) { tile ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.dp, Color.Black)
                                .background(getTileColor(tile)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = tile.letter?.toString() ?: "", fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("🔙 Geri Dön")
                }
            }
        }
    }
}
