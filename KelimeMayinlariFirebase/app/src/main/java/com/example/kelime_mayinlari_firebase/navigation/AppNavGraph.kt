// AppNavGraph.kt
package com.example.kelime_mayinlari_firebase.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.kelime_mayinlari_firebase.ui.theme.*
import com.example.kelime_mayinlari_firebase.viewModel.AuthViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: AuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController = navController, viewModel = viewModel)
        }
        composable("register") {
            RegisterScreen(navController = navController, viewModel = viewModel)
        }
        composable("home") {
            HomeScreen(navController = navController, viewModel = viewModel)
        }
        composable("yeni_oyun") {
            YeniOyunScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        composable(
            route = "map_screen/{oyunId}/{kullaniciAdi}",
            arguments = listOf(
                navArgument("oyunId") { type = NavType.StringType },
                navArgument("kullaniciAdi") { type = NavType.StringType }
            )
        ) {
            val oyunId = it.arguments?.getString("oyunId") ?: ""
            val kullaniciAdi = it.arguments?.getString("kullaniciAdi") ?: ""
            MapScreen(
                oyunId = oyunId,
                kullaniciAdi = kullaniciAdi,
                navController = navController
            )
        }
        composable("aktif_oyunlar") {
            val kullaniciAdi = viewModel.currentUsername.value ?: ""
            AktifOyunlarScreen(navController, kullaniciAdi)
        }
        composable(
            "gameover/{oyunId}/{kullaniciAdi}/{sonuc}/{myScore}/{oppScore}/{kalanHarfSayisi}",
            arguments = listOf(
                navArgument("oyunId") { type = NavType.StringType },
                navArgument("kullaniciAdi") { type = NavType.StringType },
                navArgument("sonuc") { type = NavType.StringType },
                navArgument("myScore") { type = NavType.IntType },
                navArgument("oppScore") { type = NavType.IntType },
                navArgument("kalanHarfSayisi") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val oyunId = backStackEntry.arguments?.getString("oyunId") ?: ""
            val kullaniciAdi = backStackEntry.arguments?.getString("kullaniciAdi") ?: ""
            val sonuc = backStackEntry.arguments?.getString("sonuc") ?: ""
            val myScore = backStackEntry.arguments?.getInt("myScore") ?: 0
            val oppScore = backStackEntry.arguments?.getInt("oppScore") ?: 0
            val kalanHarfSayisi = backStackEntry.arguments?.getInt("kalanHarfSayisi") ?: 0

            GameOverScreen(
                oyunId = oyunId,
                kullaniciAdi = kullaniciAdi,
                sonuc = sonuc,
                myScore = myScore,
                oppScore = oppScore,
                kalanHarfSayisi = kalanHarfSayisi,
                navController = navController
            )
        }

        composable(
            "map_view_only/{oyunId}",
            arguments = listOf(navArgument("oyunId") { type = NavType.StringType })
        ) {
            val oyunId = it.arguments?.getString("oyunId") ?: ""
            MapViewOnlyScreen(oyunId = oyunId, navController = navController)
        }

        composable("biten_oyunlar") {
            val username = viewModel.currentUsername.value ?: ""
            BitenOyunlarScreen(navController, kullaniciAdi = username)
        }








    }
}