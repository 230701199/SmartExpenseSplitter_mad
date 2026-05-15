package com.example.smartexpensesplitter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartexpensesplitter.ui.theme.SmartExpenseSplitterTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartExpenseSplitterTheme {
                PremiumSplash {
                    // ✅ Check if user is already logged in
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        // Already logged in → go straight to MainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        // Not logged in → go to AuthActivity
                        startActivity(Intent(this, AuthActivity::class.java))
                    }
                    finish()
                }
            }
        }
    }
}

@Composable
fun PremiumSplash(onFinish: () -> Unit) {

    // ✨ Fade animation
    var start by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(900),
        label = ""
    )

    // ⏳ Delay + navigate
    LaunchedEffect(Unit) {
        start = true
        delay(1800)
        onFinish()
    }

    // 🎨 Premium gradient (dark → soft accent)
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // deep navy
            Color(0xFF1E293B), // slate
            Color(0xFF334155)  // lighter slate
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {

            // 🟣 Subtle accent line (premium touch)
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF22C55E), // green
                                Color(0xFF38BDF8)  // cyan
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Smart Expense Splitter",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Manage shared expenses effortlessly",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5F5)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFF38BDF8),
                strokeWidth = 2.dp
            )
        }
    }
}
