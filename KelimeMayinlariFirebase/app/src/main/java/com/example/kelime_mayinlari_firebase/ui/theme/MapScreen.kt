// MapScreen.kt
package com.example.kelime_mayinlari_firebase.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.kelime_mayinlari_firebase.viewModel.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut


@Composable
fun MapScreen(
    oyunId: String,
    kullaniciAdi: String,
    navController: NavHostController,
    viewModel: GameBoardViewModel = viewModel()
) {
    val board by viewModel.board.collectAsState()
    val player by viewModel.currentPlayer.collectAsState()
    val opponent by viewModel.opponent.collectAsState()
    val isMyTurn by viewModel.isMyTurn.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val placedLetters by viewModel.placedLetters.collectAsState()
    val hataMesaji by viewModel.hataMesaji.collectAsState()

    val remainingLettersCount by viewModel.remainingLettersCount.collectAsState()

    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ⏹ Butonlar arasında seçili taşıma koordinatları için ek state
    var showTasimaDialog by remember { mutableStateOf(false) }
    var fromX by remember { mutableStateOf(0) }
    var fromY by remember { mutableStateOf(0) }
    var toX by remember { mutableStateOf(0) }
    var toY by remember { mutableStateOf(0) }
    var showJokerDialog by remember { mutableStateOf(false) }
    var pendingJokerPlacement by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val harfPuanlari = mapOf(
        'A' to 1, 'B' to 3, 'C' to 4, 'Ç' to 4, 'D' to 3,
        'E' to 1, 'F' to 7, 'G' to 5, 'Ğ' to 8, 'H' to 5,
        'I' to 2, 'İ' to 1, 'J' to 10, 'K' to 1, 'L' to 1,
        'M' to 2, 'N' to 1, 'O' to 2, 'Ö' to 7, 'P' to 5,
        'R' to 1, 'S' to 2, 'Ş' to 4, 'T' to 1, 'U' to 2,
        'Ü' to 3, 'V' to 7, 'Y' to 3, 'Z' to 4
    )

    // Büyük emoji gösterimi için
    var showBigEmoji by remember { mutableStateOf(false) }
    var bigEmoji by remember { mutableStateOf("") }


// ✅ Aksiyon Butonları
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = { viewModel.confirmWord(oyunId) }, enabled = isMyTurn) {
            Text("Onayla")
        }
        Button(onClick = { viewModel.undoLastLetter() }, enabled = isMyTurn) {
            Text("Geri Al")
        }
        Button(onClick = { viewModel.pasGec(oyunId) }, enabled = isMyTurn) {
            Text("Pas")
        }
        Button(onClick = { viewModel.teslimOl(oyunId) }) {
            Text("Teslim Ol")
        }
        Button(onClick = { showTasimaDialog = true }, enabled = isMyTurn) {
            Text("Taşı")
        }
    }

// 📦 TAŞIMA DİYALOĞU (harf taşıma)
    if (showTasimaDialog) {
        AlertDialog(
            onDismissRequest = { showTasimaDialog = false },
            title = { Text("Harf Taşı") },
            text = {
                Column {
                    Text("Nereden:")
                    Row {
                        OutlinedTextField(value = fromX.toString(), onValueChange = {
                            fromX = it.toIntOrNull() ?: 0
                        }, label = { Text("X1") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = fromY.toString(), onValueChange = {
                            fromY = it.toIntOrNull() ?: 0
                        }, label = { Text("Y1") }, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Nereye:")
                    Row {
                        OutlinedTextField(value = toX.toString(), onValueChange = {
                            toX = it.toIntOrNull() ?: 0
                        }, label = { Text("X2") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = toY.toString(), onValueChange = {
                            toY = it.toIntOrNull() ?: 0
                        }, label = { Text("Y2") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.komsuyaTasima(oyunId, fromX, fromY, toX, toY)
                    showTasimaDialog = false
                }) {
                    Text("Taşı")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTasimaDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.kelimeListesiniYukle(context)
        viewModel.initializePlayerInfo(oyunId, kullaniciAdi)
        viewModel.initializeBoard(oyunId)
        viewModel.listenForMoves(oyunId, kullaniciAdi)
        viewModel.listenForGameStart(oyunId, kullaniciAdi)
    }

    LaunchedEffect(Unit) {
        viewModel.listenForGameEnd(oyunId) { winner, myScore, oppScore ->
            val result = when {
                winner == null -> "draw"
                winner == kullaniciAdi -> "win"
                else -> "lose"
            }

            val kalanHarfSayisi = viewModel.getRemainingLettersCount()

            navController.navigate("gameover/$oyunId/$kullaniciAdi/$result/$myScore/$oppScore/$kalanHarfSayisi") {
                popUpTo("map_screen/$oyunId/$kullaniciAdi") { inclusive = true }
            }
        }
    }

    LaunchedEffect(hataMesaji) {
        hataMesaji?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.temizleHataMesaji()
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .padding(paddingValues)
        ) {

            // 👤 Oyuncu ve Rakip Bilgileri + Sıra ve Süre
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlayerInfoColumn(
                    title = player?.name ?: "Sen",
                    score = player?.score ?: 0,
                    active = isMyTurn
                )
                CenterInfoColumn(
                    remainingTime = remainingTime,
                    isMyTurn = isMyTurn,
                    harfSayisi = remainingLettersCount
                )
                PlayerInfoColumn(
                    title = opponent?.name ?: "Rakip",
                    score = opponent?.score ?: 0,
                    active = !isMyTurn
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🧩 Oyun Tahtası
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
                            .background(getTileColor(tile))
                            .clickable(enabled = isMyTurn && selectedLetter != null && tile.letter == null) {
                                selectedLetter?.let { harf ->
                                    if (harf == '*') {
                                        pendingJokerPlacement = Pair(tile.x, tile.y)
                                        showJokerDialog = true
                                    } else {
                                        // Harfi yerleştir
                                        viewModel.placeLetter(tile.x, tile.y, harf, oyunId)

                                        // 🎁 veya 💣 kontrolü
                                        val emoji = when {
                                            tile.reward != null && !tile.isRewardClaimed -> when (tile.reward) {
                                                RewardType.BOLGE_YASAGI -> "🛡️"
                                                RewardType.HARF_YASAGI -> "🔤"
                                                RewardType.EKSTRA_HAMLE_JOKERI -> "♻️"
                                                else -> "🎖️"
                                            }

                                            tile.trap != TrapType.NONE -> when (tile.trap) {
                                                TrapType.PUAN_BOLUNMESI -> "🧨"
                                                TrapType.PUAN_TRANSFERI -> "💸"
                                                TrapType.HARF_KAYBI -> "✂️"
                                                TrapType.EKSTRA_HAMLE_ENGELI -> "🚫"
                                                TrapType.KELIME_IPTALI -> "❌"
                                                else -> "💣"
                                            }

                                            else -> null
                                        }


                                        selectedLetter = null
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Harfi göster
                        tile.letter?.let {
                            Text(
                                text = it.toString(),
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }

                        // 🎁 veya 💣 sadece revealed ise göster
                        if (tile.revealed && tile.letter == null) {
                            Text(
                                text = when {
                                    tile.reward != null && !tile.isRewardClaimed -> when (tile.reward) {
                                        RewardType.BOLGE_YASAGI -> "\uD83D\uDEE1\uFE0F"
                                        RewardType.HARF_YASAGI -> "\uD83D\uDD24"
                                        RewardType.EKSTRA_HAMLE_JOKERI -> "🎁"
                                        else -> "🎖️"
                                    }

                                    tile.trap != TrapType.NONE -> when (tile.trap) {
                                        TrapType.PUAN_BOLUNMESI -> "🧨"
                                        TrapType.PUAN_TRANSFERI -> "💸"
                                        TrapType.HARF_KAYBI -> "✂️"
                                        TrapType.EKSTRA_HAMLE_ENGELI -> "🚫"
                                        TrapType.KELIME_IPTALI -> "❌"
                                        else -> "💣"
                                    }

                                    else -> ""
                                },
                                fontSize = 14.sp,
                                color = Color.Red
                            )
                        }

                        // Harf puanı
                        tile.letter?.let { harf ->
                            val puan = harfPuanlari[harf] ?: 0
                            Text(
                                text = puan.toString(),
                                fontSize = 8.sp,
                                color = Color.DarkGray,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val kelimePuani by viewModel.kelimePuani.collectAsState()

            Text(
                text = "Toplam Kelime Puanı: $kelimePuani",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            OdulKullanButtonlari(
                kullaniciAdi = kullaniciAdi,
                oyunId = oyunId,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(16.dp))
            // 🔠 Harf Seçimi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
            ) {
                player?.letters?.forEach { harf ->
                    Button(
                        onClick = { selectedLetter = harf },
                        enabled = isMyTurn,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .border(
                                2.dp,
                                if (selectedLetter == harf) Color.Green else Color.Transparent
                            )
                    ) {
                        Text(harf.toString())
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { viewModel.confirmWord(oyunId) }, enabled = isMyTurn) {
                    Text("Onayla")
                }
                Button(onClick = { viewModel.undoLastLetter() }, enabled = isMyTurn) {
                    Text("Geri Al")
                }
                Button(onClick = { viewModel.pasGec(oyunId) }, enabled = isMyTurn) {
                    Text("Pas")
                }
                Button(onClick = { viewModel.teslimOl(oyunId) }, enabled = isMyTurn) {
                    Text("Teslim Ol")
                }
                Button(onClick = { showTasimaDialog = true }, enabled = isMyTurn) {
                    Text("Taşı")
                }
            }
            if (hataMesaji == "Ekstra hamle hakkınız var! Kullanmak ister misiniz?") {
                AlertDialog(
                    onDismissRequest = { viewModel.temizleHataMesaji() },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.ekstraHamleyiKullan(oyunId, kullaniciAdi, true)
                            viewModel.temizleHataMesaji()
                        }) {
                            Text("Evet")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            viewModel.ekstraHamleyiKullan(oyunId, kullaniciAdi, false)
                            viewModel.temizleHataMesaji()
                        }) {
                            Text("Hayır")
                        }
                    },
                    title = { Text("Ekstra Hamle") },
                    text = { Text("Ekstra hamle hakkınızı kullanmak istiyor musunuz?") }
                )
            }
            if (showTasimaDialog) {
                AlertDialog(
                    onDismissRequest = { showTasimaDialog = false },
                    title = { Text("Harf Taşı") },
                    text = {
                        Column {
                            Text("Nereden:")
                            Row {
                                OutlinedTextField(value = fromX.toString(), onValueChange = {
                                    fromX = it.toIntOrNull() ?: 0
                                }, label = { Text("X1") }, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(value = fromY.toString(), onValueChange = {
                                    fromY = it.toIntOrNull() ?: 0
                                }, label = { Text("Y1") }, modifier = Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("Nereye:")
                            Row {
                                OutlinedTextField(value = toX.toString(), onValueChange = {
                                    toX = it.toIntOrNull() ?: 0
                                }, label = { Text("X2") }, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(value = toY.toString(), onValueChange = {
                                    toY = it.toIntOrNull() ?: 0
                                }, label = { Text("Y2") }, modifier = Modifier.weight(1f))
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.komsuyaTasima(oyunId, fromX, fromY, toX, toY)
                            showTasimaDialog = false
                        }) {
                            Text("Taşı")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTasimaDialog = false }) {
                            Text("İptal")
                        }
                    }
                )
            }
        }
    }
    JokerHarfSecDialog(
        showDialog = showJokerDialog,
        pendingPlacement = pendingJokerPlacement,
        onDismiss = {
            showJokerDialog = false
            pendingJokerPlacement = null
        },
        onHarfSecildi = { harf ->
            val (x, y) = pendingJokerPlacement!!
            viewModel.placeLetter(x, y, harf, oyunId, isJoker = true)
            selectedLetter = null
            showJokerDialog = false
            pendingJokerPlacement = null
        }

    )
}

    @Composable
fun PlayerInfoColumn(title: String, score: Int, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 18.sp,
            color = if (active) Color.Green else Color.Gray
        )
        Text("Puan: $score")
    }
}

@Composable
fun CenterInfoColumn(remainingTime: Int, isMyTurn: Boolean, harfSayisi: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Kalan Harf: $harfSayisi", fontSize = 18.sp)
        Text("Süre: $remainingTime sn", fontSize = 14.sp)
        Text(
            text = if (isMyTurn) "⚡ Sıra sizde" else "⌛ Rakip oynuyor",
            color = if (isMyTurn) Color.Green else Color.Red,
            fontSize = 14.sp
        )
    }
}

fun getTileColor(tile: Tile): Color {
    tile.highlightColor?.let {
        return when (it) {
            "green" -> Color(0xFFA5D6A7) // açık yeşil
            "red" -> Color(0xFFEF9A9A)   // açık kırmızı
            else -> Color.White
        }
    }
//    return when (tile.trap) {
//        TrapType.PUAN_BOLUNMESI -> Color(0xFFFFCDD2) // Açık kırmızı
//        TrapType.PUAN_TRANSFERI -> Color(0xFFFFF9C4) // Açık sarı
//        TrapType.HARF_KAYBI -> Color(0xFFD1C4E9)     // Morumsu
//        TrapType.EKSTRA_HAMLE_ENGELI -> Color(0xFFB2EBF2) // Açık mavi
//        TrapType.KELIME_IPTALI -> Color(0xFFEF9A9A)   // Daha koyu kırmızı
//        TrapType.NONE -> when (tile.type) {
//            TileType.H2 -> Color(0xFFBBDEFB)
//            TileType.H3 -> Color(0xFFF8BBD0)
//            TileType.K2 -> Color(0xFFC8E6C9)
//            TileType.K3 -> Color(0xFFD7CCC8)
//            TileType.STAR -> Color.Yellow
//            else -> Color.White
//        }
//    }
    return when {
        tile.trap != TrapType.NONE -> {
            // Eğer hem trap hem bonus varsa, bonus rengini göster
            when (tile.type) {
                TileType.H2 -> Color(0xFFBBDEFB)
                TileType.H3 -> Color(0xFFF8BBD0)
                TileType.K2 -> Color(0xFFC8E6C9)
                TileType.K3 -> Color(0xFFD7CCC8)
                TileType.STAR -> Color.Yellow
                else -> Color.White // sadece trap varsa beyaz
            }
        }

        else -> when (tile.type) {
            TileType.H2 -> Color(0xFFBBDEFB)
            TileType.H3 -> Color(0xFFF8BBD0)
            TileType.K2 -> Color(0xFFC8E6C9)
            TileType.K3 -> Color(0xFFD7CCC8)
            TileType.STAR -> Color.Yellow
            else -> Color.White
        }
    }

}@Composable
fun JokerHarfSecDialog(
    showDialog: Boolean,
    pendingPlacement: Pair<Int, Int>?,
    onDismiss: () -> Unit,
    onHarfSecildi: (Char) -> Unit
) {
    val turkceHarfler = listOf(
        'A','B','C','Ç','D','E','F','G','Ğ','H','I','İ','J','K','L','M',
        'N','O','Ö','P','R','S','Ş','T','U','Ü','V','Y','Z'
    )

    if (showDialog && pendingPlacement != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Joker Harfi Seç") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Row {
                        turkceHarfler.forEach { harf ->
                            Button(
                                onClick = { onHarfSecildi(harf) },
                                modifier = Modifier
                                    .padding(4.dp)
                            ) {
                                Text(harf.toString())
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun OdulKullanButtonlari(
    kullaniciAdi: String,
    oyunId: String,
    viewModel: GameBoardViewModel
) {
    val oduller by viewModel.kullaniciOdulleri.collectAsState()

    if (oduller.isNotEmpty()) {
        Column {
            Text(
                text = "Ödüller:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                oduller.forEach { odul ->
                    Button(
                        onClick = {
                            viewModel.kullanOdul(odul, oyunId, kullaniciAdi)
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                    ) {
                        Text(text = odul.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }
}

@Composable
fun TileBox(tile: Tile, harfPuanlari: Map<Char, Int>) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(1.dp, Color.Black)
            .background(Color.White)
    ) {
        val harf = tile.letter
        val puan = if (harf != null) harfPuanlari[harf] ?: 0 else null

        if (harf != null) {
            Text(
                text = harf.toString(),
                modifier = Modifier.align(Alignment.Center),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = puan.toString(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp),
                fontSize = 10.sp,
                color = Color.DarkGray
            )
        }
    }
}





