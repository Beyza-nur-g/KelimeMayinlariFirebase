package com.example.kelime_mayinlari_firebase.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.kelime_mayinlari_firebase.viewModel.AuthViewModel
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val error by viewModel.error.collectAsState()
    val info by viewModel.infoMessage.collectAsState()

    LaunchedEffect(info) {
        info?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Giriş Yap", fontSize = 22.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text("Kullanıcı Adı") }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Şifre") }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                viewModel.loginWithUsername(
                    username.value.trim(),
                    password.value.trim()
                )
            }) {
                Text("Giriş Yap")
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = {
                viewModel.clearError()
                navController.navigate("register")
            }) {
                Text("Hesabın yok mu? Kayıt ol")
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("⚠️ $it", color = Color.Red)
            }
        }
    }
}
