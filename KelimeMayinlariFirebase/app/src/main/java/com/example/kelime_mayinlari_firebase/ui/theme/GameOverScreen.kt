package com.example.kelime_mayinlari_firebase.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.kelime_mayinlari_firebase.viewModel.GameBoardViewModel

@Composable
fun GameOverScreen(
    oyunId: String,
    kullaniciAdi: String,
    sonuc: String,
    myScore: Int,
    oppScore: Int,
    kalanHarfSayisi: Int,
    navController: NavHostController
){
    val resultText = when (sonuc) {
        "draw" -> "🤝 Oyun Berabere Bitti"
        "win" -> "🎉 Kazandınız!"
        "lose" -> "😢 Kaybettiniz!"
        else -> ""
    }

    val resultColor = when (sonuc) {
        "draw" -> Color.Gray
        "win" -> Color(0xFF388E3C)
        "lose" -> Color(0xFFD32F2F)
        else -> Color.Black
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(resultText, fontSize = 28.sp, color = resultColor)

            Divider(thickness = 2.dp)

            Text("🧑 Sizin Puanınız: $myScore", fontSize = 20.sp)
            Text("👤 Rakip Puanı: $oppScore", fontSize = 20.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Text("🅰️ Kalan Harf Sayısı: $kalanHarfSayisi", fontSize = 18.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {
                navController.navigate("home") {
                    popUpTo("game_over") { inclusive = true }
                    launchSingleTop = true
                }
            }) {
                Text("Ana Sayfaya Dön")
            }

        }
    }
}
