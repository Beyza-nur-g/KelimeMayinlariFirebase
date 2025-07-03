package com.example.kelime_mayinlari_firebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.kelime_mayinlari_firebase.navigation.AppNavGraph
import com.example.kelime_mayinlari_firebase.viewModel.AuthViewModel
import com.example.kelime_mayinlari_firebase.ui.theme.KelimeMayinlariFirebaseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KelimeMayinlariFirebaseTheme {
                val navController = rememberNavController()
                val viewModel: AuthViewModel = viewModel()
                AppNavGraph(navController = navController, viewModel = viewModel)
            }
        }
    }
}
