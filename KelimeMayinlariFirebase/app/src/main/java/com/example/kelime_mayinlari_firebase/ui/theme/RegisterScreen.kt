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
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val username = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
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

    if (isLoggedIn) {
        navController.navigate("home") {
            popUpTo("register") { inclusive = true }
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
            Text("Kayıt Ol", fontSize = 22.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text("Kullanıcı Adı") }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text("E-posta") }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Şifre") }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                viewModel.registerWithValidation(
                    username.value.trim(),
                    email.value.trim(),
                    password.value.trim()
                )
            }) {
                Text("Kayıt Ol")
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = {
                viewModel.clearError()
                navController.popBackStack()
            }) {
                Text("Zaten hesabım var")
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("⚠️ $it", color = Color.Red)
            }
        }
    }
}
