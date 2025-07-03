// YeniOyunScreen.kt
package com.example.kelime_mayinlari_firebase.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.kelime_mayinlari_firebase.viewModel.AuthViewModel
import com.example.kelime_mayinlari_firebase.viewModel.GameMatchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YeniOyunScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel(),
    matchViewModel: GameMatchViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val selectedOption = remember { mutableStateOf<String?>(null) }
    val isWaiting = remember { mutableStateOf(false) }

    val username = viewModel.currentUsername.collectAsState().value ?: return
    val matchRequest by matchViewModel.matchRequest.collectAsState()
    val matchResult by matchViewModel.matchResult.collectAsState() // 🎯 eğer varsa

    // 🔁 Dinleyiciyi başlat
    LaunchedEffect(username) {
        matchViewModel.listenForIncomingMatch(username)
    }

    // ✅ Güvenli yönlendirme — eşleşme verileri dolunca yönlendir
    LaunchedEffect(matchResult) {
        val oyunId = matchResult?.oyunId
        val rakipAdi = matchResult?.rakipKullaniciAdi
        if (!oyunId.isNullOrBlank() && !rakipAdi.isNullOrBlank()) {
            navController.navigate("map_screen/$oyunId/$username")
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Oyun Süresi Seç", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))

            val sureler = listOf("2dk", "5dk", "12sa", "24sa")
            sureler.forEach { sure ->
                Button(
                    onClick = {
                        selectedOption.value = sure
                        isWaiting.value = true
                        matchViewModel.startWaitingLoop(sure, username)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    enabled = !isWaiting.value
                ) {
                    Text(sure)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isWaiting.value) {
                Text("Oyuncu aranıyor...", fontSize = 16.sp)
            }
        }

        // 🎯 Eşleşme bildirimi geldiyse AlertDialog göster
        matchRequest?.let { (matchId, rakip) ->
            AlertDialog(
                onDismissRequest = { /* Gerekirse işlemleri temizle */ },
                title = { Text("Eşleşme bulundu") },
                text = { Text("$rakip ile eşleştiniz. Oyunu başlatmak ister misiniz?") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            matchViewModel.confirmMatch(
                                matchId = matchId,
                                kullaniciAdi = username
                            ) { oyunId, rakipAdi ->
                                matchViewModel.setMatchResult(oyunId, rakipAdi)
                            }
                            matchViewModel.pollMatchResult(matchId, username)
                        }
                    }) {
                        Text("Evet")
                    }

                },
                dismissButton = {
                    TextButton(onClick = {
                        scope.launch {
                            matchViewModel.rejectMatch(matchId)
                            matchViewModel.clearMatchRequest()
                        }
                    }) {
                        Text("Hayır")
                    }
                }
            )
        }
    }
}
