package com.example.uth_socials

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.uth_socials.ui.component.navigation.AuthNav
import com.example.uth_socials.ui.screen.OnboardingScreen
import com.example.uth_socials.ui.screen.home.HomeScreen
import com.example.uth_socials.ui.theme.UTH_SocialsTheme
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, gso)
        val viewModel = AuthViewModel(auth, googleClient)

        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.handleGoogleResult(this, result.data)
        }

        setContent {
            UTH_SocialsTheme {
                AuthNav(viewModel = viewModel, launcher = launcher)
            }
        }
    }
}