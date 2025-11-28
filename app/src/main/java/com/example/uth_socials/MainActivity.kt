package com.example.uth_socials

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.ui.component.navigation.AppNavGraph
import com.example.uth_socials.ui.theme.UTH_SocialsTheme
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.example.uth_socials.ui.viewmodel.AuthViewModelFactory
import com.example.uth_socials.ui.viewmodel.BanStatusViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import androidx.annotation.RequiresApi


class MainActivity : ComponentActivity() {
    private lateinit var banStatusViewModel: BanStatusViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, gso)    

        val userRepository = UserRepository()

        val authViewModelFactory = AuthViewModelFactory(auth, userRepository, googleClient)
        val viewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]

        banStatusViewModel = ViewModelProvider(this)[BanStatusViewModel::class.java]

        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.handleGoogleResult(result.data)
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val channel = NotificationChannel(
            "default_channel",
            "Thông báo chung",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)


        FirebaseMessaging.getInstance().subscribeToTopic("allUsers")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Subscribed to all_users topic")
                } else {
                    println("Failed to subscribe")
                }
            }

        setContent {
            UTH_SocialsTheme {
                AppNavGraph(
                    viewModel = viewModel,
                    launcher = launcher
                )

            }
        }
    }

}