package com.hddev.byahero.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hddev.byahero.data.UserRole
import com.hddev.byahero.ui.theme.ByaheroBlack
import com.hddev.byahero.ui.theme.ByaheroCanvas
import com.hddev.byahero.ui.theme.ByaheroDarkGreen
import com.hddev.byahero.ui.theme.ByaheroGlassWhite
import com.hddev.byahero.ui.theme.ByaheroMuted
import com.hddev.byahero.ui.theme.ByaheroWhite

private data class PredefinedCredential(
    val email: String,
    val password: String,
    val role: UserRole
)

private val PREDEFINED_LOGINS = listOf(
    PredefinedCredential("passenger@byahero.app", "password123", UserRole.PASSENGER),
    PredefinedCredential("rider@byahero.app", "password123", UserRole.RIDER),
    PredefinedCredential("alex@byahero.app", "password123", UserRole.BOTH)
)

@Composable
fun AuthGateScreen(onAuthenticated: (UserRole) -> Unit) {
    var isSignUp by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.BOTH) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFEAF3ED), ByaheroCanvas)
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ByaheroGlassWhite)
                .border(1.dp, Color.White, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            Text(
                text = "BYAHERO",
                color = ByaheroBlack,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                text = "by HDDEV",
                color = ByaheroDarkGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        isSignUp = false
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isSignUp) ByaheroDarkGreen else Color(0xFFEBF1ED),
                        contentColor = if (!isSignUp) ByaheroWhite else ByaheroBlack
                    )
                ) {
                    Text("Log In", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        isSignUp = true
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSignUp) ByaheroDarkGreen else Color(0xFFEBF1ED),
                        contentColor = if (isSignUp) ByaheroWhite else ByaheroBlack
                    )
                ) {
                    Text("Sign Up", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (isSignUp) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (isSignUp) {
                Spacer(Modifier.height(12.dp))
                Text("Select Role", color = ByaheroDarkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UserRole.entries.forEach { role ->
                        Button(
                            onClick = { selectedRole = role },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRole == role) ByaheroDarkGreen else Color(0xFFEBF1ED),
                                contentColor = if (selectedRole == role) ByaheroWhite else ByaheroBlack
                            )
                        ) {
                            Text(role.name, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorMessage!!, color = Color(0xFFB91C1C), fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val cleanEmail = email.trim().lowercase()
                    if (isSignUp) {
                        if (fullName.isBlank() || cleanEmail.isBlank() || password.length < 6) {
                            errorMessage = "Enter your full name, valid email, and password (min 6 characters)."
                        } else {
                            onAuthenticated(selectedRole)
                        }
                    } else {
                        val match = PREDEFINED_LOGINS.find {
                            it.email == cleanEmail && it.password == password
                        }
                        if (match != null) {
                            onAuthenticated(match.role)
                        } else {
                            errorMessage = "Incorrect email or password."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ByaheroDarkGreen)
            ) {
                Text(
                    text = if (isSignUp) "Create Account" else "Log In",
                    color = ByaheroWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { onAuthenticated(UserRole.BOTH) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ByaheroWhite,
                    contentColor = ByaheroBlack
                )
            ) {
                Text("Continue with Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Travel safely with BYAHERO",
                color = ByaheroMuted,
                fontSize = 11.sp
            )
        }
    }
}
