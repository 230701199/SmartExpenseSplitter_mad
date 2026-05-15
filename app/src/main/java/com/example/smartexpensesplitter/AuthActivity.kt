package com.example.smartexpensesplitter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensesplitter.ui.theme.SmartExpenseSplitterTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    // ─── Google Sign-In launcher ──────────────────────────────────────────────
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                // Exchange Google ID token for Firebase credential
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { navigateToMain() }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            SmartExpenseSplitterTheme {
                AuthScreen(
                    onLogin = { email, password -> handleLogin(email, password) },
                    onRegister = { email, password -> handleRegister(email, password) },
                    onResendVerification = { email, password -> resendVerificationEmail(email, password) },
                    onGoogleSignIn = { launchGoogleSignIn() }
                )
            }
        }
    }

    /**
     * Login flow:
     * 1. Sign in with Firebase.
     * 2. If successful, check email verification.
     * 3. If not verified → sign out + show error. Never proceed to main.
     * 4. If verified → navigate to main screen.
     */
    private fun handleLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                if (!user.isEmailVerified) {
                    // Block login — sign back out immediately
                    auth.signOut()
                    // Signal the UI to show the "not verified" state
                    // We do this via a broadcast-style approach using a companion flag
                    AuthState.unverifiedEmail = email
                    AuthState.unverifiedPassword = password
                    AuthState.showVerificationBanner = true
                    // Recreate content to trigger recomposition with updated state
                    setContent {
                        SmartExpenseSplitterTheme {
                            AuthScreen(
                                onLogin = { e, p -> handleLogin(e, p) },
                                onRegister = { e, p -> handleRegister(e, p) },
                                onResendVerification = { e, p -> resendVerificationEmail(e, p) },
                                onGoogleSignIn = { launchGoogleSignIn() }
                            )
                        }
                    }
                } else {
                    // Email verified — proceed normally
                    navigateToMain()
                }
            }
            .addOnFailureListener { e ->
                // Only shows Firebase error (wrong password, no account, etc.)
                // Does NOT auto-create an account
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Register flow:
     * 1. Create account with Firebase.
     * 2. Send verification email.
     * 3. Sign out immediately — user must verify before they can log in.
     * 4. Show success message instructing user to check their email.
     */
    private fun handleRegister(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                // Send verification email
                user.sendEmailVerification()
                    .addOnSuccessListener {
                        // Sign out — user must verify first
                        auth.signOut()
                        Toast.makeText(
                            this,
                            "✅ Verification email sent to $email.\nPlease verify before logging in.",
                            Toast.LENGTH_LONG
                        ).show()
                        // Switch UI back to login mode
                        AuthState.switchToLogin = true
                        setContent {
                            SmartExpenseSplitterTheme {
                                AuthScreen(
                                    onLogin = { e, p -> handleLogin(e, p) },
                                    onRegister = { e, p -> handleRegister(e, p) },
                                    onResendVerification = { e, p -> resendVerificationEmail(e, p) },
                                    onGoogleSignIn = { launchGoogleSignIn() }
                                )
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        // Account created but email sending failed — still sign out
                        auth.signOut()
                        Toast.makeText(
                            this,
                            "Account created but could not send verification email: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Re-authenticate to get a fresh user object, then resend verification email.
     * Needed because Firebase requires recent auth to send verification.
     */
    private fun resendVerificationEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                user.sendEmailVerification()
                    .addOnSuccessListener {
                        auth.signOut()
                        Toast.makeText(
                            this,
                            "Verification email resent to $email.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        auth.signOut()
                        Toast.makeText(this, "Could not resend: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /** Launch the Google One-Tap / Sign-In flow */
    private fun launchGoogleSignIn() {
        val webClientId = try { getString(R.string.default_web_client_id) } catch (e: Exception) { "" }
        if (webClientId.isEmpty()) {
            Toast.makeText(
                this,
                "Google Sign-In not configured. Enable it in Firebase Console.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    private fun navigateToMain() {
        AuthState.reset()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

// ─── Simple shared state object to communicate Activity → Composable ──────────
// (Avoids ViewModel overhead for this simple one-Activity case)
object AuthState {
    var showVerificationBanner: Boolean = false
    var unverifiedEmail: String = ""
    var unverifiedPassword: String = ""
    var switchToLogin: Boolean = false

    fun reset() {
        showVerificationBanner = false
        unverifiedEmail = ""
        unverifiedPassword = ""
        switchToLogin = false
    }
}

// ─── Auth Screen Composable ────────────────────────────────────────────────────

@Composable
fun AuthScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onResendVerification: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit
) {
    // If activity signaled to switch to login (after registration), honour it
    var isLogin by remember { mutableStateOf(if (AuthState.switchToLogin) { AuthState.switchToLogin = false; true } else true) }
    var email    by remember { mutableStateOf(AuthState.unverifiedEmail) }
    var password by remember { mutableStateOf("") }

    // Show the "not verified" banner if login was blocked
    var showNotVerifiedBanner by remember { mutableStateOf(AuthState.showVerificationBanner) }

    // Whenever the user types, clear the banner
    LaunchedEffect(email, password) {
        if (showNotVerifiedBanner && (email != AuthState.unverifiedEmail || password.isNotEmpty())) {
            showNotVerifiedBanner = false
            AuthState.showVerificationBanner = false
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
    )

    Box(
        modifier = Modifier.fillMaxSize().background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Accent bar ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .width(48.dp).height(4.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF22C55E), Color(0xFF38BDF8)))
                        )
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (isLogin) "Welcome Back" else "Create Account",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
                Text("Smart Expense Splitter", fontSize = 13.sp, color = Color(0xFF94A3B8))

                Spacer(Modifier.height(24.dp))

                // ── Email not verified banner ─────────────────────────────────
                AnimatedVisibility(
                    visible = showNotVerifiedBanner,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFFFB800).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "⚠️ Email not verified",
                            color = Color(0xFFFFB800),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Please verify your email before logging in.\nCheck your inbox for the verification link.",
                            color = Color(0xFFFFD966),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                onResendVerification(
                                    AuthState.unverifiedEmail,
                                    AuthState.unverifiedPassword
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB800)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800))
                        ) {
                            Text("Resend Verification Email", fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (showNotVerifiedBanner) Spacer(Modifier.height(16.dp))

                // ── Email field ──────────────────────────────────────────────
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF38BDF8)
                    )
                )

                Spacer(Modifier.height(12.dp))

                // ── Password field ───────────────────────────────────────────
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color(0xFF94A3B8)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF38BDF8)
                    )
                )

                Spacer(Modifier.height(24.dp))

                // ── Primary action button ────────────────────────────────────
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) return@Button
                        // Each mode calls its own dedicated handler — no shared fallback
                        if (isLogin) onLogin(email, password)
                        else         onRegister(email, password)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLogin) Color(0xFF22C55E) else Color(0xFF38BDF8)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = email.isNotBlank() && password.isNotBlank()
                ) {
                    Text(
                        text = if (isLogin) "Login" else "Register",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                }

                // ── Register note ────────────────────────────────────────────
                AnimatedVisibility(visible = !isLogin) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "A verification email will be sent.\nYou must verify before logging in.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── OR divider ───────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF475569))
                    Text("  OR  ", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF475569))
                }

                Spacer(Modifier.height(16.dp))

                // ── Google Sign-In button ────────────────────────────────────
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("🔵  Sign in with Google", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(16.dp))

                // ── Toggle login / register ──────────────────────────────────
                TextButton(onClick = {
                    isLogin = !isLogin
                    showNotVerifiedBanner = false
                    AuthState.showVerificationBanner = false
                }) {
                    Text(
                        text = if (isLogin) "Don't have an account? Register"
                               else         "Already have an account? Login",
                        color = Color(0xFF38BDF8),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
