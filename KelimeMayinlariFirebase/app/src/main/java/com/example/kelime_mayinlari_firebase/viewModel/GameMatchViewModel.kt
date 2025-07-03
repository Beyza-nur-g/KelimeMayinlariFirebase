
package com.example.kelime_mayinlari_firebase.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GameMatchViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _matchRequest = MutableStateFlow<Pair<String, String>?>(null) // (gameId, opponent)
    val matchRequest: StateFlow<Pair<String, String>?> = _matchRequest

    private val _matchResult = MutableStateFlow<MatchResult?>(null)
    val matchResult: StateFlow<MatchResult?> = _matchResult

    private var currentUser: String? = null
    private var currentRoom: String? = null

    fun startWaitingLoop(
        sureTipi: String,
        kullaniciAdi: String
    ) {
        _isSearching.value = true
        currentUser = kullaniciAdi
        currentRoom = "waiting_$sureTipi"

        val sureInSeconds = when (sureTipi) {
            "2dk" -> 120
            "5dk" -> 300
            "12sa" -> 12 * 60 * 60
            "24sa" -> 24 * 60 * 60
            else -> 120
        }


        viewModelScope.launch {
            // 🔍 Daha önce oluşturulmuş matchId kontrolü (önceki eşleşmelerde duplicate yaratma engeli)
            val mevcutMatch = firestore.collection("match_requests")
                .whereEqualTo("player1", kullaniciAdi)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?: firestore.collection("match_requests")
                    .whereEqualTo("player2", kullaniciAdi)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()

            if (mevcutMatch != null) {
                val rakip = (mevcutMatch.getString("player1") ?: mevcutMatch.getString("player2"))
                    ?.takeIf { it != kullaniciAdi }
                if (rakip != null) {
                    _matchRequest.value = Pair(mevcutMatch.id, rakip)
                    _isSearching.value = false
                    return@launch
                }
            }

            val waitingRef = firestore.collection(currentRoom!!).document(kullaniciAdi)
            try {
                waitingRef.set(mapOf("timestamp" to System.currentTimeMillis()), SetOptions.merge()).await()
            } catch (e: Exception) {
                _isSearching.value = false
                return@launch
            }

            while (_isSearching.value) {
                delay(2000)

                val snapshot = try {
                    firestore.collection(currentRoom!!).get().await()
                } catch (e: Exception) {
                    continue
                }

                val digerOyuncular = snapshot.documents.mapNotNull { it.id }.filter { it != kullaniciAdi }

                if (digerOyuncular.isNotEmpty()) {
                    val rakipAdi = digerOyuncular.first()

                    val matchId = "match_${System.currentTimeMillis()}"
                    val matchData = mapOf(
                        "player1" to rakipAdi,
                        "player2" to kullaniciAdi,
                        "sure" to sureInSeconds,
                        "status" to mapOf(
                            rakipAdi to "pending",
                            kullaniciAdi to "pending"
                        ),
                        "notified" to mapOf(
                            rakipAdi to false,
                            kullaniciAdi to true
                        )

                    )

                    try {
                        firestore.collection("match_requests").document(matchId).set(matchData).await()
                        firestore.collection(currentRoom!!).document(kullaniciAdi).delete()
                        firestore.collection(currentRoom!!).document(rakipAdi).delete()
                        _isSearching.value = false
                        _matchRequest.value = Pair(matchId, rakipAdi)
                        break
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        }
    }
    fun pollIncomingMatches(kullaniciAdi: String) {
        viewModelScope.launch {
            while (true) {
                delay(2000)
                val snapshot = firestore.collection("match_requests")
                    .whereEqualTo("status.$kullaniciAdi", "pending")
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    val matchId = doc.id
                    val players = doc.get("players") as? List<*> ?: listOf(doc.getString("player1"), doc.getString("player2"))
                    val opponent = players.firstOrNull { it != kullaniciAdi }?.toString() ?: continue

                    val notified = doc.get("notified") as? Map<*, *> ?: continue
                    if (notified[kullaniciAdi] == false) {
                        firestore.collection("match_requests").document(matchId)
                            .update("notified.$kullaniciAdi", true)
                            .await()
                        _matchRequest.value = Pair(matchId, opponent)
                        return@launch
                    }
                }
            }
        }
    }
    fun confirmMatch(
        matchId: String,
        kullaniciAdi: String,
        onAllConfirmed: (oyunId: String, rakip: String) -> Unit
    ) {
        viewModelScope.launch {
            val ref = firestore.collection("match_requests").document(matchId)
            ref.update("status.$kullaniciAdi", "confirmed").await()

            val snapshot = ref.get().await()
            val status = snapshot.get("status") as? Map<*, *> ?: return@launch
            val notified = snapshot.get("notified") as? Map<*, *> ?: return@launch

            val player1 = snapshot.getString("player1") ?: return@launch
            val player2 = snapshot.getString("player2") ?: return@launch
            val sure = snapshot.getLong("sure")?.toInt() ?: 120

            if (status.values.all { it == "confirmed" }) {
                val starter = listOf(player1, player2).shuffled().first()
                val currentTurn = starter

                val gameData = mapOf(
                    "players" to listOf(player1, player2),
                    "sure" to sure,
                    "starter" to starter,
                    "currentTurn" to currentTurn,
                    "createdAt" to System.currentTimeMillis(),
                    "matchId" to matchId,
                    "scores" to mapOf(
                        player1 to 0,
                        player2 to 0
                    ),
                    "winner" to null //
                )
                // ✅ matchId = gameDoc.id
                firestore.collection("games").document(matchId).set(gameData).await()

                // 🎯 match_requests silinmez, çünkü diğer oyuncu henüz yönlendirilmedi
                // Sadece yönlendirme bilgisi ayarlanır
                firestore.collection("match_requests").document(matchId)
                    .update("notified.$kullaniciAdi", true)

                val rakip = if (kullaniciAdi == player1) player2 else player1
                _matchResult.value = MatchResult(oyunId = matchId, rakipKullaniciAdi = rakip)
                onAllConfirmed(matchId, rakip)
            }
        }
    }

    fun rejectMatch(matchId: String) {
        viewModelScope.launch {
            firestore.collection("match_requests").document(matchId).delete().await()
        }
    }

    fun stopSearching() {
        _isSearching.value = false
        currentUser?.let { user ->
            currentRoom?.let { room ->
                firestore.collection(room).document(user).delete()
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        stopSearching()
    }
    fun listenForIncomingMatch(kullaniciAdi: String) {
        firestore.collection("match_requests")
            .whereEqualTo("player1", kullaniciAdi)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.forEach { doc ->
                    val status = doc.get("status") as? Map<*, *> ?: return@forEach
                    if ((status[kullaniciAdi] as? String) == "pending") {
                        val rakip = doc.getString("player2") ?: return@forEach
                        _matchRequest.value = Pair(doc.id, rakip)
                    }
                }
            }

        firestore.collection("match_requests")
            .whereEqualTo("player2", kullaniciAdi)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.forEach { doc ->
                    val status = doc.get("status") as? Map<*, *> ?: return@forEach
                    if ((status[kullaniciAdi] as? String) == "pending") {
                        val rakip = doc.getString("player1") ?: return@forEach
                        _matchRequest.value = Pair(doc.id, rakip)
                    }
                }
            }
    }
    fun pollMatchResult(matchId: String, kullaniciAdi: String) {
        viewModelScope.launch {
            while (_matchResult.value == null) {
                delay(2000)

                val doc = firestore.collection("match_requests").document(matchId).get().await()
                val status = doc.get("status") as? Map<*, *> ?: continue
                val notified = doc.get("notified") as? Map<*, *> ?: continue

                if (status.values.all { it == "confirmed" } && notified.values.all { it == true }) {
                    val player1 = doc.getString("player1") ?: continue
                    val player2 = doc.getString("player2") ?: continue
                    val rakip = if (kullaniciAdi == player1) player2 else player1

                    // 🔍 matchId = gameId olduğu için doğrudan alınır
                    val gameSnapshot = firestore.collection("games").document(matchId).get().await()
                    if (gameSnapshot.exists()) {
                        _matchResult.value = MatchResult(matchId, rakip)
                        break
                    }
                }
            }
        }
    }
    fun setMatchResult(oyunId: String, rakipKullaniciAdi: String) {
        _matchResult.value = MatchResult(oyunId, rakipKullaniciAdi)
    }
    fun clearMatchRequest() {
        _matchRequest.value = null
    }
}
data class MatchResult(
    val oyunId: String,
    val rakipKullaniciAdi: String
)
