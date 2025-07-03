package com.example.kelime_mayinlari_firebase.viewModel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage = _infoMessage.asStateFlow()

    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername = _currentUsername.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun clearInfoMessage() {
        _infoMessage.value = null
    }

    fun registerWithValidation(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _error.value = "Tüm alanlar zorunludur"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _error.value = "Geçerli bir e-posta adresi giriniz"
            return
        }

        if (!isPasswordStrong(password)) {
            _error.value = "Şifre en az 8 karakter, büyük/küçük harf ve rakam içermelidir"
            return
        }

        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    _error.value = "Bu kullanıcı adı zaten alınmış"
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val uid = authResult.user?.uid ?: return@addOnSuccessListener

                            // Başlangıç değerleri
                            val userMap = hashMapOf(
                                "username" to username,
                                "email" to email,
                                "gamesPlayed" to 0,
                                "gamesWon" to 0,
                                "winRate" to 0.0,
                                "awards" to listOf<String>() // Başlangıçta boş ödül listesi
                            )

                            firestore.collection("users").document(uid).set(userMap)
                                .addOnSuccessListener {
                                    _currentUsername.value = username
                                    _isLoggedIn.value = true
                                    _infoMessage.value = "Kayıt başarılı!"
                                }
                        }
                        .addOnFailureListener {
                            _error.value = it.localizedMessage
                        }
                }
            }
            .addOnFailureListener {
                _error.value = "Kullanıcı adı kontrolü başarısız: ${it.localizedMessage}"
            }
    }

    fun loginWithUsername(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _error.value = "Kullanıcı adı ve şifre gerekli"
            return
        }

        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    _error.value = "Bu kullanıcı adına ait kayıt bulunamadı"
                } else {
                    val email = result.documents.first().getString("email") ?: ""
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            _currentUsername.value = username
                            _isLoggedIn.value = true
                            _infoMessage.value = "Hoş geldin, $username!"
                        }
                        .addOnFailureListener {
                            _error.value = "Kullanıcı adı veya şifre yanlış"
                        }
                }
            }
            .addOnFailureListener {
                _error.value = "Veritabanı hatası: ${it.localizedMessage}"
            }
    }

    private fun isPasswordStrong(password: String): Boolean {
        val lengthOK = password.length >= 8
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return lengthOK && hasUpper && hasLower && hasDigit
    }
    private val _gamesPlayed = MutableStateFlow(0)
    val gamesPlayed = _gamesPlayed.asStateFlow()

    private val _gamesWon = MutableStateFlow(0)
    val gamesWon = _gamesWon.asStateFlow()

    fun loadUserStats(username: String) {
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { docs ->
                val doc = docs.firstOrNull() ?: return@addOnSuccessListener
                _gamesPlayed.value = (doc.getLong("gamesPlayed") ?: 0L).toInt()
                _gamesWon.value = (doc.getLong("gamesWon") ?: 0L).toInt()
            }
    }

}
