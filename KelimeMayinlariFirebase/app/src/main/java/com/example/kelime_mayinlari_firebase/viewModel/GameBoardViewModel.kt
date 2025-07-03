package com.example.kelime_mayinlari_firebase.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Job

enum class RewardType {
    BOLGE_YASAGI, HARF_YASAGI, EKSTRA_HAMLE_JOKERI
}

enum class TileType { NORMAL, H2, H3, K2, K3, STAR }

data class PlayerState(
    val name: String,
    var score: Int = 0,
    var letters: MutableList<Char> = mutableListOf()
)
enum class TrapType {
    NONE,
    PUAN_BOLUNMESI,
    PUAN_TRANSFERI,
    HARF_KAYBI,
    EKSTRA_HAMLE_ENGELI,
    KELIME_IPTALI
}

data class Tile(
    val x: Int,
    val y: Int,
    val type: TileType,
    val letter: Char? = null,
    val trap: TrapType = TrapType.NONE,
    val reward: RewardType? = null,
    val isRewardClaimed: Boolean = false,
    val highlightColor: String? = null ,// "green" | "red" | null
    val isJoker: Boolean = false,
    var revealed: Boolean = false
)
class GameBoardViewModel : ViewModel() {

    private val _board = MutableStateFlow<List<MutableList<Tile>>>(emptyList())
    val board = _board.asStateFlow()

    private val _currentPlayer = MutableStateFlow<PlayerState?>(null)
    val currentPlayer = _currentPlayer.asStateFlow()

    private val _opponent = MutableStateFlow<PlayerState?>(null)
    val opponent = _opponent.asStateFlow()

    private val _isMyTurn = MutableStateFlow(true)
    val isMyTurn = _isMyTurn.asStateFlow()

    private val _remainingTime = MutableStateFlow(120)
    val remainingTime = _remainingTime.asStateFlow()

    private val kelimeListesi = mutableSetOf<String>()

    private val harfeGoreKelimeler = mutableMapOf<Char, Set<String>>()
    private val trapLocations = mutableMapOf<Pair<Int, Int>, TrapType>()

    val seededRandom = Random(42) // Her oyun için sabit dağılım sağlar
    val allCoordinates = (0..14).flatMap { y -> (0..14).map { x -> Pair(x, y) } }.shuffled(seededRandom)

    private val _kullaniciOdulleri = MutableStateFlow<List<RewardType>>(emptyList())
    val kullaniciOdulleri = _kullaniciOdulleri.asStateFlow()

    private val _sonHarfKonumu = MutableStateFlow<Pair<Int, Int>?>(null)
    val sonHarfKonumu = _sonHarfKonumu.asStateFlow()
    var kelimePuaniKonumlari = mutableMapOf<Pair<Int, Int>, Int>()

    private val _pasSayaci = MutableStateFlow(0)
    val pasSayaci = _pasSayaci.asStateFlow()

    suspend fun kelimeListesiniYukle(context: Context) {
        try {
            val assetManager = context.assets
            val klasorAdi = "kelimeler"
            val dosyalar = assetManager.list(klasorAdi) ?: return

            for (dosyaAdi in dosyalar) {
                val harf = dosyaAdi.first().lowercaseChar()
                val kelimeSeti = mutableSetOf<String>()

                val inputStream = assetManager.open("$klasorAdi/$dosyaAdi")
                inputStream.bufferedReader().useLines { satirlar ->
                    kelimeSeti.addAll(satirlar.map { it.trim().lowercase() })
                }

                harfeGoreKelimeler[harf] = kelimeSeti
            }

            Log.d("KelimeYükle", "Tüm kelime listeleri yüklendi.")

        } catch (e: Exception) {
            Log.e("KelimeYükle", "Yükleme hatası: ${e.message}")
        }
    }
    private val harfHavuzu = mutableListOf<Char>().apply {
        repeat(12) { add('A') }
        repeat(2) { addAll(listOf('B', 'C', 'Ç', 'D', 'Z', 'Ş', 'Ü')) }
        repeat(8) { add('E') }
        repeat(1) { addAll(listOf('F', 'G', 'H', 'P', 'Ö', 'Ğ')) }
        repeat(5) { addAll(listOf('N', 'T', 'U')) }
        repeat(7) { addAll(listOf('İ', 'K', 'L')) }
        repeat(6) { add('R') }
        repeat(4) { addAll(listOf('I', 'M')) }
        repeat(3) { addAll(listOf('O', 'Y')) }
        repeat(1) { add('V') }
        repeat(20) { add('*') }
        shuffle()
    }

    val harfPuanlari = mapOf(
        'A' to 1, 'B' to 3, 'C' to 4, 'Ç' to 4, 'D' to 3, 'E' to 1, 'F' to 7, 'G' to 5,
        'Ğ' to 8, 'H' to 5, 'I' to 2, 'İ' to 1, 'J' to 10, 'K' to 1, 'L' to 1, 'M' to 4,
        'N' to 1, 'O' to 2, 'Ö' to 7, 'P' to 5, 'R' to 1, 'S' to 2, 'Ş' to 4, 'T' to 1,
        'U' to 3, 'Ü' to 2, 'V' to 7, 'Y' to 3, 'Z' to 4, '*' to 0
    )

    private val _placedLetters = MutableStateFlow<List<Triple<Int, Int, Char>>>(emptyList())
    val placedLetters = _placedLetters.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()

    fun initializeBoard(oyunId: String) {
        val K3 = setOf(
            Pair(0, 2), Pair(14, 12), Pair(0, 12),
            Pair(2, 0), Pair(2, 14),
            Pair(12, 0), Pair(14, 2), Pair(12, 14)
        )
        val K2 = setOf(
            Pair(2, 7),Pair(12, 7), Pair(3, 11), Pair(11, 11), Pair(11, 3),
            Pair(3, 3), Pair(7, 2), Pair(7, 12), Pair(3, 11)
        )
        val H3 = setOf(
            Pair(1, 1), Pair(10, 4),
            Pair(1, 13), Pair(13, 1),
            Pair(4, 4), Pair(13, 13),
            Pair(4, 10),
            Pair(10, 10)
        )
        val H2 = setOf(
            Pair(0, 5), Pair(5, 5), Pair(6, 1), Pair(6, 13),
            Pair(0, 9), Pair(5, 9), Pair(6, 6), Pair(8, 1),
            Pair(1, 6), Pair(5, 15), Pair(6, 8), Pair(8, 6),
            Pair(1, 8), Pair(8, 8), Pair(8, 13), Pair(9, 0),
            Pair(5, 0) ,Pair(9, 5), Pair(9, 9), Pair(9, 15),
            Pair(13, 6), Pair(13, 8), Pair(14, 5), Pair(14, 9),
            Pair(14, 5), Pair(14, 9),Pair(5,14 ),Pair(9, 14),
        )

        // Mayınlar (bombalar)
        trapLocations.clear()
        trapLocations.putAll(generateTrapLocations(oyunId))

        // Ödüller
        val rewardLocations = generateRewardLocations(oyunId.hashCode().toLong())

        // Tahtayı oluştur
        val boardData = List(15) { y ->
            MutableList(15) { x ->
                val type = when {
                    x == 7 && y == 7 -> TileType.STAR
                    K3.contains(Pair(x, y)) -> TileType.K3
                    K2.contains(Pair(x, y)) -> TileType.K2
                    H3.contains(Pair(x, y)) -> TileType.H3
                    H2.contains(Pair(x, y)) -> TileType.H2
                    else -> TileType.NORMAL
                }
                val coord = Pair(x, y)
                val trap = trapLocations[coord] ?: TrapType.NONE
                val reward = rewardLocations[coord] // 👈 ödül varsa ekleniyor
                Tile(x, y, type, null, trap, reward)
            }
        }

        _board.value = boardData
    }

    private fun generateTrapLocations(oyunId: String): Map<Pair<Int, Int>, TrapType> {
        val trapCounts = mapOf(
            TrapType.PUAN_BOLUNMESI to 5,
            TrapType.PUAN_TRANSFERI to 4,
            TrapType.HARF_KAYBI to 3,
            TrapType.EKSTRA_HAMLE_ENGELI to 2,
            TrapType.KELIME_IPTALI to 2
        )

        val seed = oyunId.hashCode().toLong() // Aynı oyunId aynı dizilimi verir
        val seededRandom = Random(seed)
        val allCoordinates = (0..14).flatMap { y -> (0..14).map { x -> Pair(x, y) } }.shuffled(seededRandom)

        val trapLocations = mutableMapOf<Pair<Int, Int>, TrapType>()
        var index = 0
        for ((trapType, count) in trapCounts) {
            repeat(count) {
                val coord = allCoordinates[index++]
                trapLocations[coord] = trapType
            }
        }
        return trapLocations
    }
    private fun generateRewardLocations(seed: Long): Map<Pair<Int, Int>, RewardType> {
        val rewardCounts = mapOf(
            RewardType.BOLGE_YASAGI to 2,
            RewardType.HARF_YASAGI to 3,
            RewardType.EKSTRA_HAMLE_JOKERI to 2
        )
        val seededRandom = Random(seed)
        val shuffledCoords = allCoordinates.shuffled(seededRandom)

        val rewardLocations = mutableMapOf<Pair<Int, Int>, RewardType>()
        var index = 30 // ilk 30 koordinatı mayınlar kullanıyor olabilir

        for ((rewardType, count) in rewardCounts) {
            repeat(count) {
                val coord = shuffledCoords[index++]
                rewardLocations[coord] = rewardType
            }
        }
        return rewardLocations
    }
    private val _remainingLettersCount = MutableStateFlow(0)
    val remainingLettersCount = _remainingLettersCount.asStateFlow()

    private fun drawLetters(count: Int): List<Char> {
        val letters = (1..count).mapNotNull {
            if (harfHavuzu.isNotEmpty()) harfHavuzu.removeAt(0) else null
        }
        _remainingLettersCount.value = harfHavuzu.size
        return letters
    }

    fun initializePlayerInfo(oyunId: String, username: String) {
        firestore.collection("games").document(oyunId).get().addOnSuccessListener { doc ->
            val players = doc.get("players") as? List<*> ?: return@addOnSuccessListener
            val currentTurn = doc.getString("currentTurn") ?: ""
            val opponentName = players.firstOrNull { it != username }?.toString() ?: return@addOnSuccessListener

            val sure = (doc.get("sure") as? Long)?.toInt() ?: 120

            _remainingTime.value = sure
            _currentPlayer.value = PlayerState(username, letters = drawLetters(7).toMutableList())
            _opponent.value = PlayerState(opponentName, letters = drawLetters(7).toMutableList())
            _isMyTurn.value = (currentTurn == username)

            if (_isMyTurn.value) startTimer(oyunId)
            yukleKullaniciOdulleri(username)
        }
    }
    fun confirmWord(oyunId: String) {
        val hamle = _placedLetters.value.sortedWith(compareBy({ it.second }, { it.first }))
        if (hamle.isEmpty()) return

        val xList = hamle.map { it.first }
        val yList = hamle.map { it.second }

        val tekSatirda = yList.distinct().size == 1
        val tekSutunda = xList.distinct().size == 1

        if (!tekSatirda && !tekSutunda) {
            _hataMesaji.value = "Harfler aynı satırda veya sütunda olmalı!"
            return
        }

        if (tekSatirda) {
            val minX = xList.minOrNull() ?: 0
            val maxX = xList.maxOrNull() ?: 0
            if (maxX - minX + 1 != xList.size) {
                _hataMesaji.value = "Harfler yatayda bitişik olmalı!"
                return
            }
        }

        if (tekSutunda) {
            val minY = yList.minOrNull() ?: 0
            val maxY = yList.maxOrNull() ?: 0
            if (maxY - minY + 1 != yList.size) {
                _hataMesaji.value = "Harfler dikeyde bitişik olmalı!"
                return
            }
        }

        if (!komsuKontroluSaglandiMi()) {
            _hataMesaji.value = "Tahtadaki harflerden en az birine komşu yer seçmelisin!"
            return
        }

        val tumKelimeler = olusanTumKelimeler()
        val hepsiGecerliMi = tumKelimeler.all { kelime ->
            val ilkHarf = kelime.firstOrNull() ?: return@all false
            val sozluk = harfeGoreKelimeler[ilkHarf] ?: return@all false
            kelime in sozluk
        }
        if (!hepsiGecerliMi) {
            _hataMesaji.value = "Bazı kelimeler geçerli değil: ${tumKelimeler.joinToString(", ")}"
            return
        }

        val current = _currentPlayer.value ?: return
        var rawScore = hesaplaKelimePuani(_board.value, hamle, harfPuanlari)


        // 📍 Mayın kontrolü
        val trapsHit = hamle.mapNotNull { (x, y, _) -> trapLocations[Pair(x, y)] }.toSet()

        if (trapsHit.contains(TrapType.KELIME_IPTALI)) {
            rawScore = 0
            _hataMesaji.value = "Kelime İptali: Bu kelimeden puan alamadın."
        } else if (trapsHit.contains(TrapType.PUAN_TRANSFERI)) {
            val rakip = _opponent.value ?: return
            rakip.score += rawScore
            _opponent.value = rakip.copy()
            _hataMesaji.value = "Puan Transferi: $rawScore puan rakip oyuncuya aktarıldı."
            rawScore = 0
        } else {
            if (trapsHit.contains(TrapType.PUAN_BOLUNMESI)) {
                val bolunen = (rawScore * 0.3).toInt()
                _hataMesaji.value = "Puan Bölünmesi: $rawScore puandan sadece $bolunen puan alındı."
                rawScore = bolunen
            }
            if (trapsHit.contains(TrapType.EKSTRA_HAMLE_ENGELI)) {
                _hataMesaji.value = "Ekstra Hamle Engeli: Katlardan gelen bonus puanlar iptal edildi."
            }
        }
        current.score += rawScore
        _currentPlayer.value = current.copy(score = current.score)

        // 🅰 Harf kaybı
        if (trapsHit.contains(TrapType.HARF_KAYBI)) {
            current.letters.clear()
            current.letters.addAll(drawLetters(7))
            _currentPlayer.value = current.copy()
        }
        val boardData = _board.value.map { it.toMutableList() }.toMutableList()

        val rewardTiles = hamle.mapNotNull { (x, y, _) ->
            val tile = boardData[y][x]
            if (tile.reward != null && !tile.isRewardClaimed) {
                // ödül verilmemişse işle
                boardData[y][x] = tile.copy(isRewardClaimed = true) // ✔ Artık verildi olarak işaretleniyor
                Pair(Pair(x, y), tile.reward)
            } else null
        }

        rewardTiles.forEach { (_, reward) ->
            val current = _currentPlayer.value ?: return@forEach
            val username = current.name
            firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { docs ->
                    val userDoc = docs.firstOrNull() ?: return@addOnSuccessListener
                    val userId = userDoc.id
                    val currentAwards = userDoc.get("awards") as? List<String> ?: emptyList()
                    val updatedAwards = currentAwards + reward.name
                    firestore.collection("users").document(userId)
                        .update("awards", updatedAwards)
                }

            _hataMesaji.value = "${reward.name.replace("", " ").lowercase().replaceFirstChar { it.uppercase() }} ödülü kazandın!"
        }
        _board.value = boardData
        _placedLetters.value = emptyList()
        drawAdditionalLetters()
        _isMyTurn.value = false

        val updatedScores = mapOf(
            current.name to current.score,
            _opponent.value?.name to (_opponent.value?.score ?: 0)
        )

        firestore.collection("games").document(oyunId)
            .update("scores", updatedScores)

        sendMoveToFirestore(oyunId, hamle)

        firestore.collection("games").document(oyunId).get()
            .addOnSuccessListener { doc ->
                val extraTurnUser = doc.getString("extraTurn")
                val currentUser = _currentPlayer.value?.name

                if (extraTurnUser == currentUser) {
                    _hataMesaji.value = "Ekstra hamle hakkınız var! Kullanmak ister misiniz?"
                    // 👉 Kullanıcıya onay kutusu gösterilmeli (UI tarafında)
                } else {
                    _isMyTurn.value = false
                    firestore.collection("games").document(oyunId)
                        .update("currentTurn", _opponent.value?.name ?: "")
                }
            }
        _pasSayaci.value = 0
        kontrolVeBitirHarfBittiMi(oyunId)
    }

    private fun kontrolVeBitirHarfBittiMi(oyunId: String) {
        val current = _currentPlayer.value ?: return
        val opponent = _opponent.value ?: return

        if (harfHavuzu.isEmpty() && current.letters.isEmpty()) {
            val oppHarfPuani = opponent.letters.sumOf { harfPuanlari[it] ?: 0 }
            val currentScore = current.score + oppHarfPuani
            val opponentScore = opponent.score - oppHarfPuani

            _currentPlayer.value = current.copy(score = currentScore)
            _opponent.value = opponent.copy(score = opponentScore)

            firestore.collection("games").document(oyunId)
                .update("scores", mapOf(
                    current.name to currentScore,
                    opponent.name to opponentScore
                ))

            when {
                currentScore > opponentScore -> bitirOyunu(oyunId, current.name)
                opponentScore > currentScore -> bitirOyunu(oyunId, opponent.name)
                else -> bitirOyunu(oyunId, null)
            }
        }

        if (harfHavuzu.isEmpty() && opponent.letters.isEmpty()) {
            val currentHarfPuani = current.letters.sumOf { harfPuanlari[it] ?: 0 }
            val currentScore = current.score - currentHarfPuani
            val opponentScore = opponent.score + currentHarfPuani

            _currentPlayer.value = current.copy(score = currentScore)
            _opponent.value = opponent.copy(score = opponentScore)

            firestore.collection("games").document(oyunId)
                .update("scores", mapOf(
                    current.name to currentScore,
                    opponent.name to opponentScore
                ))
            when {
                currentScore > opponentScore -> bitirOyunu(oyunId, current.name)
                opponentScore > currentScore -> bitirOyunu(oyunId, opponent.name)
                else -> bitirOyunu(oyunId, null)
            }
        }
    }

    fun ekstraHamleyiKullan(oyunId: String, kullaniciAdi: String, kullan: Boolean) {
        if (kullan) {
            _isMyTurn.value = true
        } else {
            _isMyTurn.value = false
            firestore.collection("games").document(oyunId)
                .update("currentTurn", _opponent.value?.name ?: "")
        }
        // Ekstra hamle hakkı temizlenir
        firestore.collection("games").document(oyunId)
            .update("extraTurn", FieldValue.delete())
        _pasSayaci.value = 0
    }

    private fun olusanTumKelimeler(): List<String> {
        val tahta = _board.value
        val yerlestirilen = _placedLetters.value.map { Pair(it.first, it.second) }

        val kelimeler = mutableSetOf<String>()

        for ((x, y) in yerlestirilen) {
            // 🔠 Yatay kelime
            var sol = x
            while (sol > 0 && tahta[y][sol - 1].letter != null) sol--
            var sag = x
            while (sag < 14 && tahta[y][sag + 1].letter != null) sag++

            if (sag - sol >= 1) {
                val kelime = (sol..sag).mapNotNull { tahta[y][it].letter }.joinToString("").lowercase()
                kelimeler.add(kelime)
            }
            // 🔠 Dikey kelime
            var ust = y
            while (ust > 0 && tahta[ust - 1][x].letter != null) ust--
            var alt = y
            while (alt < 14 && tahta[alt + 1][x].letter != null) alt++

            if (alt - ust >= 1) {
                val kelime = (ust..alt).mapNotNull { tahta[it][x].letter }.joinToString("").lowercase()
                kelimeler.add(kelime)
            }
        }

        return kelimeler.toList()
    }
    fun jokerHarfiYerineKoy(harf: Char, koordinat: Pair<Int, Int>?) {
        koordinat?.let { (x, y) ->
            val oyuncu = _currentPlayer.value ?: return

            // Derin kopya al
            val newLetters = oyuncu.letters.toMutableList()
            if (newLetters.contains('*')) {
                newLetters.remove('*')

                val newOyuncu = oyuncu.copy(letters = newLetters)

                val boardData = _board.value.map { it.toMutableList() }.toMutableList()
                boardData[y][x] = boardData[y][x].copy(letter = harf, isJoker = true)

                _board.value = boardData
                _currentPlayer.value = newOyuncu
            }
        }
    }




    private fun komsuKontroluSaglandiMi(): Boolean {
        val tahta = _board.value
        val yerlestirilen = _placedLetters.value.map { Pair(it.first, it.second) }.toSet()

        // Eğer sadece ilk hamle yapılıyorsa: Ortadaki kutuyu içermeli
        val sadeceYeniHarflerVar = tahta.flatten().all { tile ->
            tile.letter == null || yerlestirilen.contains(Pair(tile.x, tile.y))
        }
        if (sadeceYeniHarflerVar) {
            return yerlestirilen.contains(Pair(7, 7))
        }
        // Normal durum: Tahtadaki en az bir harfe komşu olmalı
        for ((x, y, _) in _placedLetters.value) {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val komsuX = x + dx
                    val komsuY = y + dy
                    if (komsuX in 0..14 && komsuY in 0..14) {
                        val komsu = tahta[komsuY][komsuX]
                        if (komsu.letter != null && Pair(komsuX, komsuY) !in yerlestirilen) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    fun bitirOyunu(oyunId: String, winner: String?) {
        firestore.collection("games").document(oyunId).update(
            mapOf(
                "starter" to "bitti",
                "winner" to winner
            )
        ).addOnSuccessListener {
            val currentUser = _currentPlayer.value?.name ?: return@addOnSuccessListener
            val opponentUser = _opponent.value?.name ?: return@addOnSuccessListener

            val usersToUpdate = listOf(currentUser, opponentUser)

            usersToUpdate.forEach { username ->
                firestore.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener { docs ->
                        val doc = docs.firstOrNull() ?: return@addOnSuccessListener
                        val userId = doc.id
                        val played = (doc.getLong("gamesPlayed") ?: 0L).toInt() + 1
                        val won = (doc.getLong("gamesWon") ?: 0L).toInt() +
                                if (username == winner) 1 else 0

                        firestore.collection("users").document(userId)
                            .update(
                                mapOf(
                                    "gamesPlayed" to played,
                                    "gamesWon" to won,
                                    "winRate" to (if (played == 0) 0.0 else (won.toDouble() / played * 100))
                                )
                            )
                    }
            }
        }
    }

    fun teslimOl(oyunId: String) {
        val rakip = _opponent.value?.name ?: return
        _isMyTurn.value = false
        _placedLetters.value = emptyList()

        bitirOyunu(oyunId, winner = rakip)
    }

    fun pasGec(oyunId: String) {
        val rakip = _opponent.value?.name ?: return

        _isMyTurn.value = false
        _placedLetters.value = emptyList()
        _pasSayaci.value += 1
        // 🔚 Eğer art arda 4 pas olduysa oyun bitir
        if (_pasSayaci.value >= 4) {
            kontrolVeBitir(oyunId)
            return
        }
        firestore.collection("games").document(oyunId)
            .update("currentTurn", rakip)
    }
    fun  kontrolVeBitir(oyunId: String) {
        val myName = _currentPlayer.value?.name ?: return
        val oppName = _opponent.value?.name ?: return
        val myScore = _currentPlayer.value?.score ?: 0
        val oppScore = _opponent.value?.score ?: 0

        when {
            myScore > oppScore -> {
                bitirOyunu(oyunId, myName)
                guncelleKullaniciSkoru(myName, true)
                guncelleKullaniciSkoru(oppName, false)
            }
            oppScore > myScore -> {
                bitirOyunu(oyunId, oppName)
                guncelleKullaniciSkoru(myName, false)
                guncelleKullaniciSkoru(oppName, true)
            }
            else -> {
                bitirOyunu(oyunId, null)
                guncelleKullaniciSkoru(myName, false)
                guncelleKullaniciSkoru(oppName, false)
            }
        }
    }

    fun listenForGameEnd(oyunId: String, onGameEnd: (winner: String?, myScore: Int, opponentScore: Int) -> Unit) {
        firestore.collection("games").document(oyunId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot?.getString("starter") == "bitti") {
                    val winner = snapshot.getString("winner")
                    val scoreMap = snapshot.get("scores") as? Map<*, *>
                    val myScore = (scoreMap?.get(_currentPlayer.value?.name) as? Long)?.toInt() ?: 0
                    val oppScore = (scoreMap?.get(_opponent.value?.name) as? Long)?.toInt() ?: 0
                    onGameEnd(winner, myScore, oppScore)
                }
            }
    }

    fun komsuyaTasima(oyunId: String, fromX: Int, fromY: Int, toX: Int, toY: Int) {
        val boardData = _board.value.map { it.toMutableList() }.toMutableList()
        val harf = boardData[fromY][fromX].letter ?: return
        val hedef = boardData[toY][toX].letter
        val mesafe = Math.abs(fromX - toX) + Math.abs(fromY - toY)

        if (mesafe == 1 && hedef == null) {
            // 🧩 Tahtayı güncelle
            boardData[fromY][fromX] = boardData[fromY][fromX].copy(letter = null)
            boardData[toY][toX] = boardData[toY][toX].copy(letter = harf)
            _board.value = boardData

            // 🔄 Yerel state güncelle
            _placedLetters.value = listOf(Triple(toX, toY, harf))

            // 🔄 Önceki hamleyi Firestore'dan sil
            firestore.collection("games").document(oyunId).collection("hamleler")
                .whereEqualTo("player", _currentPlayer.value?.name)
                .get()
                .addOnSuccessListener { docs ->
                    docs.forEach { it.reference.delete() }

                    // ✔ Yeni konumu hamle olarak Firestore’a kaydet
                    sendMoveToFirestore(oyunId, listOf(Triple(toX, toY, harf)))

                    // ⏭ Sıra rakibe geçsin
                    _isMyTurn.value = false
                    firestore.collection("games").document(oyunId)
                        .update("currentTurn", _opponent.value?.name ?: "")
                    _pasSayaci.value = 0
                }
        } else {
            _hataMesaji.value = "Sadece 1 birim ve boş kutuya taşıyabilirsin!"
        }
        _pasSayaci.value = 0
    }
    private val _hataMesaji = MutableStateFlow<String?>(null)
    val hataMesaji = _hataMesaji.asStateFlow()

    fun temizleHataMesaji() {
        _hataMesaji.value = null
    }
    fun hesaplaKelimePuani(
        tahta: List<List<Tile>>,
        yerlestirilen: List<Triple<Int, Int, Char>>,
        harfPuanlari: Map<Char, Int>
    ): Int {
        var toplam = 0
        var kelimeKatsayi = 1

        for ((x, y, harf) in yerlestirilen) {
            val tile = tahta[y][x]
            val puan = harfPuanlari[harf] ?: 0

            toplam += when (tile.type) {
                TileType.H2 -> puan * 2
                TileType.H3 -> puan * 3
                else -> puan
            }

            kelimeKatsayi *= when (tile.type) {
                TileType.K2 -> 2
                TileType.K3 -> 3
                else -> 1
            }
        }
        return toplam * kelimeKatsayi
    }
    private val _kelimePuani = MutableStateFlow(0)
    val kelimePuani = _kelimePuani.asStateFlow()

    fun setKelimePuani(puan: Int) {
        _kelimePuani.value = puan
    }


    fun guncelleKelimeRenklendirme() {
        val tahta = _board.value.map { it.toMutableList() }.toMutableList()
        // 🔄 Renkleri temizle
        for (y in 0..14) {
            for (x in 0..14) {
                val tile = tahta[y][x]
                if (tile.highlightColor != null) {
                    tahta[y][x] = tile.copy(highlightColor = null)
                }
            }
        }

        val yerlestirilen = _placedLetters.value.map { Pair(it.first, it.second) }
        val kutuDurumlari = mutableMapOf<Pair<Int, Int>, MutableSet<Boolean>>()
        val yeniKelimePuaniKonumlari = mutableMapOf<Pair<Int, Int>, Int>()

        for ((x, y) in yerlestirilen) {
            // ➤ Yatay kelime
            var sol = x
            while (sol > 0 && tahta[y][sol - 1].letter != null) sol--
            var sag = x
            while (sag < 14 && tahta[y][sag + 1].letter != null) sag++

            if (sag - sol >= 1) {
                val kelime = (sol..sag).mapNotNull { tahta[y][it].letter }.joinToString("").lowercase()
                val ilkHarf = kelime.firstOrNull()
                val gecerli = ilkHarf != null && harfeGoreKelimeler[ilkHarf]?.contains(kelime) == true

                for (i in sol..sag) {
                    kutuDurumlari.getOrPut(Pair(i, y)) { mutableSetOf() }.add(gecerli)
                }

                if (gecerli) {
                    val koordinatlar = (sol..sag).map { it to y }
                    val toplamPuan = hesaplaKelimePuani(_board.value, _placedLetters.value, harfPuanlari)

                    yeniKelimePuaniKonumlari[Pair(sag + 1, y + 1)] = toplamPuan
                }
            }
            // ➤ Dikey kelime
            var ust = y
            while (ust > 0 && tahta[ust - 1][x].letter != null) ust--
            var alt = y
            while (alt < 14 && tahta[alt + 1][x].letter != null) alt++

            if (alt - ust >= 1) {
                val kelime = (ust..alt).mapNotNull { tahta[it][x].letter }.joinToString("").lowercase()
                val ilkHarf = kelime.firstOrNull()
                val gecerli = ilkHarf != null && harfeGoreKelimeler[ilkHarf]?.contains(kelime) == true

                for (j in ust..alt) {
                    kutuDurumlari.getOrPut(Pair(x, j)) { mutableSetOf() }.add(gecerli)
                }

                if (gecerli) {
                    val koordinatlar = (ust..alt).map { x to it }
                    val toplamPuan = hesaplaKelimePuani(_board.value, _placedLetters.value, harfPuanlari)

                    yeniKelimePuaniKonumlari[Pair(x + 1, alt + 1)] = toplamPuan
                }
            }
        }
        for ((coord, durumlar) in kutuDurumlari) {
            val (x, y) = coord
            val renk = when {
                durumlar.contains(true) && !durumlar.contains(false) -> "green"
                durumlar.contains(false) && !durumlar.contains(true) -> "red"
                else -> null
            }

            val tile = tahta[y][x]
            tahta[y][x] = tile.copy(highlightColor = renk)
        }

        _board.value = tahta
        kelimePuaniKonumlari = yeniKelimePuaniKonumlari
        val puan = hesaplaKelimePuani(_board.value, _placedLetters.value, harfPuanlari)
        setKelimePuani(puan)
    }
    fun placeLetter(x: Int, y: Int, harf: Char, oyunId: String, isJoker: Boolean = false) {
        if (!_isMyTurn.value) return

        val username = _currentPlayer.value?.name ?: return

        firestore.collection("games").document(oyunId).get()
            .addOnSuccessListener { doc ->
                val zoneLock = doc.get("zoneLock") as? Map<*, *> ?: emptyMap<String, String>()
                val lockSide = zoneLock[username] as? String
                if (lockSide == "left" && x < 7) {
                    _hataMesaji.value = "Bu bölgede harf yerleştiremezsin!"
                    return@addOnSuccessListener
                }

                val frozen = doc.get("frozenLetters") as? Map<*, *> ?: emptyMap<String, List<String>>()
                val donmus = frozen[username] as? List<String> ?: emptyList()
                if (donmus.contains(harf.toString())) {
                    _hataMesaji.value = "$harf harfini bu tur kullanamazsın!"
                    return@addOnSuccessListener
                }

                val boardData = _board.value.map { it.toMutableList() }.toMutableList()
                val tile = boardData[y][x]

                if (tile.letter == null &&
                    (_currentPlayer.value?.letters?.contains(harf) == true || isJoker)
                ) {
                    boardData[y][x] = tile.copy(letter = harf, revealed = true, isJoker = isJoker)
                    _board.value = boardData.toList()

                    if (isJoker) {
                        _currentPlayer.value?.letters?.remove('*')
                    } else {
                        _currentPlayer.value?.letters?.remove(harf)
                    }

                    _placedLetters.value = _placedLetters.value + Triple(x, y, harf)
                    guncelleKelimeRenklendirme()
                }
            }
    }


    fun undoLastLetter() {
        val last = _placedLetters.value.lastOrNull() ?: return
        val boardData = _board.value.map { it.toMutableList() }.toMutableList()
        boardData[last.second][last.first] = boardData[last.second][last.first].copy(letter = null)
        _board.value = boardData
        _currentPlayer.value?.letters?.add(last.third)
        _placedLetters.value = _placedLetters.value.dropLast(1)
        guncelleKelimeRenklendirme()
    }
    private fun drawAdditionalLetters() {
        val current = _currentPlayer.value ?: return
        val needed = 7 - current.letters.size
        val newLetters = drawLetters(needed)
        current.letters.addAll(newLetters)
        _currentPlayer.value = current
    }
    private fun sendMoveToFirestore(oyunId: String, hamle: List<Triple<Int, Int, Char>>) {
        val hamleListesi = hamle.map {
            mapOf("x" to it.first, "y" to it.second, "letter" to it.third.toString())
        }

        val moveData = mapOf(
            "player" to _currentPlayer.value?.name,
            "letters" to hamleListesi,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("games").document(oyunId)
            .collection("hamleler")
            .add(moveData)
    }

    fun listenForMoves(oyunId: String, username: String) {
        firestore.collection("games").document(oyunId)
            .collection("hamleler")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val data = change.document.data
                        val letters = data["letters"] as? List<Map<String, Any>> ?: return@forEach

                        for (l in letters) {
                            val x = (l["x"] as Long).toInt()
                            val y = (l["y"] as Long).toInt()
                            val c = (l["letter"] as String).first()
                            val boardData = _board.value.map { it.toMutableList() }.toMutableList()
                            if (boardData[y][x].letter == null) {
                                boardData[y][x] = boardData[y][x].copy(letter = c)
                                _board.value = boardData
                            }
                        }
                        firestore.collection("games").document(oyunId).get().addOnSuccessListener { doc ->
                            val nextTurn = doc.getString("currentTurn") ?: ""
                            val sure = doc.getLong("sure")?.toInt() ?: 120  // 🔄 burada düzeltildi
                            _remainingTime.value = sure
                            _isMyTurn.value = (nextTurn == username)

                            if (_isMyTurn.value) {
                                // ❄ Dondurulmuş harfleri temizle (sadece 1 tur geçerli olması için)
                                val frozenTurnMap = doc.get("frozenLettersTurn") as? Map<*, *>
                                if (frozenTurnMap?.get(username) == true) {
                                    firestore.collection("games").document(oyunId)
                                        .update(
                                            mapOf(
                                                "frozenLetters.$username" to emptyList<String>(),
                                                "frozenLettersTurn.$username" to false
                                            )
                                        )
                                }
                                startTimer(oyunId)
                            }
                            val scoreMap = doc.get("scores") as? Map<*, *>
                            val myScore = (scoreMap?.get(username) as? Long)?.toInt() ?: 0
                            val oppScore = scoreMap?.filterKeys { it != username }?.values?.firstOrNull() as? Long ?: 0

                            _currentPlayer.value = _currentPlayer.value?.copy(score = myScore)
                            _opponent.value = _opponent.value?.copy(score = oppScore.toInt())
                        }
                    }
                }
            }
    }

    fun getRemainingLettersCount(): Int = harfHavuzu.size

    private var timerJob: Job? = null

    private fun startTimer(oyunId: String) {
        if (!_isMyTurn.value || timerJob?.isActive == true) return

        timerJob = viewModelScope.launch {
            while (_remainingTime.value > 0) {
                delay(1000)
                _remainingTime.value -= 1
            }
            _isMyTurn.value = false
            val rakip = _opponent.value?.name ?: return@launch
            val ben = _currentPlayer.value?.name ?: return@launch

            bitirOyunu(oyunId, winner = rakip)
        }
    }

    fun listenForGameStart(oyunId: String, username: String) {
        firestore.collection("games").document(oyunId)
            .addSnapshotListener { snapshot, _ ->
                val currentTurn = snapshot?.getString("currentTurn") ?: return@addSnapshotListener
                _isMyTurn.value = (currentTurn == username)

                val sure = snapshot.getLong("sure")?.toInt() ?: 120
                _remainingTime.value = sure

                if (_isMyTurn.value) startTimer(oyunId)
            }
    }

    fun kullanOdul(reward: RewardType, oyunId: String, kullaniciAdi: String) {
        val firestore = FirebaseFirestore.getInstance()
        val rakip = _opponent.value ?: return

        val odulMesaji = when (reward) {
            RewardType.BOLGE_YASAGI -> {
                firestore.collection("games").document(oyunId)
                    .update("zoneLock", mapOf(rakip.name to "left"))
                "${rakip.name} için sol taraf yasaklandı."
            }
            RewardType.HARF_YASAGI -> {
                val rakipHarfler = rakip.letters
                if (rakipHarfler.isEmpty()) {
                    _hataMesaji.value = "${rakip.name} için harf yasağı uygulanamadı çünkü harfi yok."
                    return
                }

                val donacakHarfler = rakipHarfler.shuffled().take(minOf(2, rakipHarfler.size)).map { it.toString() }

                firestore.collection("games").document(oyunId)
                    .update(
                        mapOf(
                            "frozenLetters" to mapOf(rakip.name to donacakHarfler),
                            "frozenLettersTurn" to mapOf(rakip.name to true)
                        )
                    )
                "${rakip.name} için bazı harfler donduruldu: ${donacakHarfler.joinToString(",")}"
            }
            RewardType.EKSTRA_HAMLE_JOKERI -> {
                firestore.collection("games").document(oyunId)
                    .update("extraTurn", kullaniciAdi)
                "$kullaniciAdi ekstra hamle hakkı kazandı!"
            }
        }

        // 🔄 Ödülü kullanınca envanterden çıkar
        firestore.collection("users")
            .whereEqualTo("username", kullaniciAdi)
            .get()
            .addOnSuccessListener { docs ->
                val doc = docs.firstOrNull() ?: return@addOnSuccessListener
                val currentAwards = doc.get("awards") as? List<String> ?: return@addOnSuccessListener
                val updated = currentAwards.toMutableList()
                updated.remove(reward.name)
                firestore.collection("users").document(doc.id)
                    .update("awards", updated)
                    .addOnSuccessListener {
                        // 👇 Bildirim göster
                        _hataMesaji.value = odulMesaji
                        // 👇 UI yeniden yüklensin diye çağır
                        yukleKullaniciOdulleri(kullaniciAdi)
                    }
            }
    }

    fun yukleKullaniciOdulleri(username: String) {
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { docs ->
                val doc = docs.firstOrNull() ?: return@addOnSuccessListener
                val rawAwards = doc.get("awards") as? List<String> ?: emptyList()
                val rewardTypes = rawAwards.mapNotNull { RewardType.values().find { r -> r.name == it } }
                _kullaniciOdulleri.value = rewardTypes
            }
    }
    fun guncelleKullaniciSkoru(kullaniciAdi: String, kazanmaDurumu: Boolean) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users")
            .whereEqualTo("username", kullaniciAdi)
            .get()
            .addOnSuccessListener { docs ->
                val doc = docs.firstOrNull() ?: return@addOnSuccessListener
                val docRef = firestore.collection("users").document(doc.id)

                val mevcutOynama = (doc.getLong("gamesPlayed") ?: 0L).toInt()
                val mevcutKazanma = (doc.getLong("gamesWon") ?: 0L).toInt()

                val yeniOynama = mevcutOynama + 1
                val yeniKazanma = if (kazanmaDurumu) mevcutKazanma + 1 else mevcutKazanma
                val winRate = if (yeniOynama == 0) 0.0 else (yeniKazanma.toDouble() / yeniOynama * 100.0)

                docRef.update(
                    mapOf(
                        "gamesPlayed" to yeniOynama,
                        "gamesWon" to yeniKazanma,
                        "winRate" to winRate
                    )
                )
            }
    }
}