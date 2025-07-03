package com.example.kelime_mayinlari_firebase.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.kelime_mayinlari_firebase.viewModel.AuthViewModel
import com.example.kelime_mayinlari_firebase.viewModel.GameBoardViewModel
import com.example.kelime_mayinlari_firebase.viewModel.RewardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    gameBoardViewModel: GameBoardViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val username by viewModel.currentUsername.collectAsState()
    val gamesPlayed by viewModel.gamesPlayed.collectAsState()
    val gamesWon by viewModel.gamesWon.collectAsState()

    val basariYuzdesi = remember(gamesPlayed, gamesWon) {
        if (gamesPlayed == 0) 0 else (gamesWon * 100 / gamesPlayed)
    }

// İstatistikleri yükle
    LaunchedEffect(username) {
        username?.let {
            gameBoardViewModel.yukleKullaniciOdulleri(it)
            viewModel.loadUserStats(it) // 🆕 istatistikleri çekiyoruz
        }
    }


    // Firestore'dan ödül bilgilerini yükle
    LaunchedEffect(username) {
        username?.let {
            gameBoardViewModel.yukleKullaniciOdulleri(it)
        }
    }

    val oduller by gameBoardViewModel.kullaniciOdulleri.collectAsState()

    // Emoji eşlemesi
    val odulEmojileri = mapOf(
        RewardType.BOLGE_YASAGI to "🛡️",
        RewardType.HARF_YASAGI to "🔤",
        RewardType.EKSTRA_HAMLE_JOKERI to "\uD83C\uDF81"
    )

    // Ödül adetlerini gruplandır
    val odulSayilari = oduller.groupingBy { it }.eachCount()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hoş geldin, ${username ?: "kullanıcı"}")
                        Text("Başarı: %$basariYuzdesi", style = MaterialTheme.typography.bodySmall)
                    }
                }
            )

        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 🏅 Ödül kutusu
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🎁 Ödüllerin", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (odulSayilari.isEmpty()) {
                        Text("Hiç ödülün yok.")
                    } else {
                        odulSayilari.forEach { (odul, adet) ->
                            val isim = odul.name.replace("_", " ")
                                .lowercase().replaceFirstChar { it.uppercase() }
                            val emoji = odulEmojileri[odul] ?: "🎖️"
                            Text("$emoji $isim: $adet adet")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate("yeni_oyun") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("🎮 Yeni Oyun")
            }

            Button(
                onClick = { navController.navigate("aktif_oyunlar") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("⏳ Aktif Oyunlar")
            }

            Button(
                onClick = { navController.navigate("biten_oyunlar") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("✅ Biten Oyunlar")
            }
        }
    }
}
