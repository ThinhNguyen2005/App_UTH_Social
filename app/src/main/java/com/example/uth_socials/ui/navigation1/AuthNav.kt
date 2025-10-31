package com.example.uth_socials.ui.navigation1


import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.uth_socials.ui.component.navigation.AppNavGraph
import com.example.uth_socials.ui.viewmodel.AuthViewModel

@Composable
fun AuthNav(
    viewModel: AuthViewModel,
    launcher: ActivityResultLauncher<Intent>
) {
    val navController = rememberNavController()
    // Delegate to unified app nav graph
    AppNavGraph(
        navController = navController,
        authViewModel = viewModel,
        launcher = launcher
    )
}
