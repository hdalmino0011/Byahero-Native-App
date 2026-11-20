package com.hddev.byahero.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hddev.byahero.R
import com.hddev.byahero.data.ActiveMode
import com.hddev.byahero.data.UserProfile
import com.hddev.byahero.data.UserRole
import com.hddev.byahero.data.VehicleType
import com.hddev.byahero.ui.theme.ByaheroBlack
import com.hddev.byahero.ui.theme.ByaheroBorder
import com.hddev.byahero.ui.theme.ByaheroCanvas
import com.hddev.byahero.ui.theme.ByaheroDarkGreen
import com.hddev.byahero.ui.theme.ByaheroGlassWhite
import com.hddev.byahero.ui.theme.ByaheroMuted
import com.hddev.byahero.ui.theme.ByaheroSubtleGlass
import com.hddev.byahero.ui.theme.ByaheroWhite
import com.hddev.byahero.ui.theme.PoppinsFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class StoredAuthAccount(
    val id: String,
    val fullName: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val vehicleType: VehicleType,
    val plateNumber: String,
    val authProvider: String
) {
    fun toProfile(): UserProfile = UserProfile(
        id = id,
        fullName = fullName,
        email = email,
        authProvider = authProvider,
        role = role,
        activeMode = if (role == UserRole.RIDER) ActiveMode.RIDER else ActiveMode.PASSENGER,
        vehicleType = vehicleType,
        plateNumber = plateNumber.ifBlank { "BYH-774" },
        isOnline = role != UserRole.PASSENGER,
        totalRides = if (role == UserRole.PASSENGER) 4 else 14,
        riderEarningsPhp = if (role == UserRole.PASSENGER) 0.0 else 3840.0,
        riderIncentivesPhp = if (role == UserRole.PASSENGER) 0.0 else 192.0
    )
}

private val DefaultSeedAccounts = listOf(
    StoredAuthAccount(
        id = "acc_passenger_1",
        fullName = "Juan Dela Cruz",
        email = "passenger@byahero.app",
        password = "password123",
        role = UserRole.PASSENGER,
        vehicleType = VehicleType.MOTORCYCLE,
        plateNumber = "",
        authProvider = "member"
    ),
    StoredAuthAccount(
        id = "acc_rider_1",
        fullName = "Carlo Dizon",
        email = "rider@byahero.app",
        password = "password123",
        role = UserRole.RIDER,
        vehicleType = VehicleType.MOTORCYCLE,
        plateNumber = "MTO-8421",
        authProvider = "member"
    ),
    StoredAuthAccount(
        id = "acc_both_1",
        fullName = "Alex Rivera",
        email = "alex@byahero.app",
        password = "password123",
        role = UserRole.BOTH,
        vehicleType = VehicleType.TAXI,
        plateNumber = "BYH-2026",
        authProvider = "google"
    )
)

private const val PREF_AUTH_STORE = "byahero_accounts_v1"
private const val KEY_ACCOUNTS_JSON = "accounts_json"

private fun loadStoredAccounts(context: Context): List<StoredAuthAccount> {
    return try {
        val prefs = context.getSharedPreferences(PREF_AUTH_STORE, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ACCOUNTS_JSON, null) ?: return DefaultSeedAccounts
        val arr = JSONArray(raw)
        val parsed = mutableListOf<StoredAuthAccount>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            parsed.add(
                StoredAuthAccount(
                    id = obj.optString("id", "user_$i"),
                    fullName = obj.optString("fullName", "BYAHERO Member"),
                    email = obj.optString("email", ""),
                    password = obj.optString("password", ""),
                    role = runCatching { UserRole.valueOf(obj.optString("role", "BOTH")) }.getOrDefault(UserRole.BOTH),
                    vehicleType = runCatching { VehicleType.valueOf(obj.optString("vehicleType", "MOTORCYCLE")) }.getOrDefault(VehicleType.MOTORCYCLE),
                    plateNumber = obj.optString("plateNumber", "BYH-2026"),
                    authProvider = obj.optString("authProvider", "member")
                )
            )
        }
        val missingSeeds = DefaultSeedAccounts.filter { seed ->
            parsed.none { it.email.equals(seed.email, ignoreCase = true) }
        }
        parsed + missingSeeds
    } catch (_: Exception) {
        DefaultSeedAccounts
    }
}

private fun saveStoredAccounts(context: Context, accounts: List<StoredAuthAccount>) {
    try {
        val arr = JSONArray()
        accounts.forEach { acc ->
            arr.put(
                JSONObject().apply {
                    put("id", acc.id)
                    put("fullName", acc.fullName)
                    put("email", acc.email)
                    put("password", acc.password)
                    put("role", acc.role.name)
                    put("vehicleType", acc.vehicleType.name)
                    put("plateNumber", acc.plateNumber)
                    put("authProvider", acc.authProvider)
                }
            )
        }
        context.getSharedPreferences(PREF_AUTH_STORE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCOUNTS_JSON, arr.toString())
            .apply()
    } catch (_: Exception) {
        // ignore persistence errors
    }
}

@Composable
fun AuthGateScreen(onAuthenticated: (UserProfile) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("login") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.BOTH) }
    var selectedVehicle by remember { mutableStateOf(VehicleType.MOTORCYCLE) }
    var plateNumber by remember { mutableStateOf("BYH-2026") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var submittingKind by remember { mutableStateOf<String?>(null) }

    fun completeAuthentication(profile: UserProfile, kind: String) {
        if (submittingKind != null) return
        submittingKind = kind
        scope.launch {
            delay(260)
            submittingKind = null
            onAuthenticated(profile)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE5F0E9),
                        ByaheroCanvas,
                        Color(0xFFEAF3ED)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = ByaheroGlassWhite,
                border = BorderStroke(1.dp, Color.White),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header matching web design
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_byahero_logo),
                                contentDescription = "BYAHERO Logo",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_byahero_wordmark_dark),
                                    contentDescription = "BYAHERO",
                                    modifier = Modifier
                                        .width(138.dp)
                                        .height(22.dp)
                                )
                                Text(
                                    text = "by HDDEV",
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ByaheroDarkGreen
                                )
                            }
                        }

                        Text(
                            text = if (mode == "login") "Sign In" else "Register",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ByaheroMuted
                        )
                    }

                    // Segmented Switcher (Log In / Sign Up)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ByaheroSubtleGlass)
                            .border(1.dp, ByaheroBorder, RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("login" to "Log In", "signup" to "Sign Up").forEach { (key, title) ->
                            val isSelected = mode == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ByaheroDarkGreen else Color.Transparent)
                                    .clickable {
                                        mode = key
                                        errorMsg = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ByaheroWhite else ByaheroMuted
                                )
                            }
                        }
                    }

                    // Inputs
                    AnimatedVisibility(
                        visible = mode == "signup",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        ByaheroAuthInput(
                            label = "Full Name",
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = "Juan Dela Cruz",
                            leadingIcon = Icons.Default.Person
                        )
                    }

                    ByaheroAuthInput(
                        label = "Email Address",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "you@example.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    ByaheroAuthInput(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Enter password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = showPassword,
                        onTogglePassword = { showPassword = !showPassword }
                    )

                    AnimatedVisibility(
                        visible = mode == "signup",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Select Role",
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ByaheroDarkGreen
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        UserRole.PASSENGER to "Passenger",
                                        UserRole.RIDER to "Rider",
                                        UserRole.BOTH to "Both"
                                    ).forEach { (role, label) ->
                                        val active = selectedRole == role
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (active) ByaheroDarkGreen else ByaheroWhite.copy(alpha = 0.85f))
                                                .border(
                                                    1.dp,
                                                    if (active) ByaheroDarkGreen else ByaheroBorder,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedRole = role },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (active) ByaheroWhite else ByaheroBlack
                                            )
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = selectedRole == UserRole.RIDER || selectedRole == UserRole.BOTH,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(ByaheroSubtleGlass)
                                        .border(1.dp, ByaheroBorder, RoundedCornerShape(16.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            Triple(VehicleType.MOTORCYCLE, "Motorcycle", Icons.Default.TwoWheeler),
                                            Triple(VehicleType.TAXI, "Taxi", Icons.Default.DirectionsCar)
                                        ).forEach { (type, label, icon) ->
                                            val active = selectedVehicle == type
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (active) ByaheroDarkGreen else ByaheroWhite)
                                                    .border(
                                                        1.dp,
                                                        if (active) ByaheroDarkGreen else ByaheroBorder,
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { selectedVehicle = type },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (active) ByaheroWhite else ByaheroBlack,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = label,
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (active) ByaheroWhite else ByaheroBlack
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ByaheroWhite)
                                            .border(1.dp, ByaheroBorder, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (plateNumber.isEmpty()) {
                                            Text(
                                                text = "Plate Number (e.g. BYH-2026)",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 12.sp,
                                                color = ByaheroMuted
                                            )
                                        }
                                        BasicTextField(
                                            value = plateNumber,
                                            onValueChange = { plateNumber = it.uppercase() },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = ByaheroBlack
                                            ),
                                            cursorBrush = SolidColor(ByaheroDarkGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = errorMsg != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        errorMsg?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = msg,
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    Button(
                        enabled = submittingKind == null,
                        onClick = {
                            errorMsg = null
                            val cleanEmail = email.trim().lowercase()
                            val accounts = loadStoredAccounts(context)
                            if (mode == "login") {
                                if (cleanEmail.isBlank() || password.isBlank()) {
                                    errorMsg = "Please enter your email address and password."
                                    return@Button
                                }
                                val match = accounts.firstOrNull {
                                    it.email.equals(cleanEmail, ignoreCase = true) && it.password == password
                                }
                                if (match == null) {
                                    errorMsg = "Incorrect email or password. Please check your credentials or sign up."
                                    return@Button
                                }
                                completeAuthentication(match.toProfile(), "form")
                            } else {
                                val cleanName = fullName.trim()
                                if (cleanName.isBlank() || !cleanEmail.contains("@") || password.length < 6) {
                                    errorMsg = "Enter your full name, valid email, and a password of at least 6 characters."
                                    return@Button
                                }
                                if (accounts.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
                                    errorMsg = "An account with this email already exists. Please log in."
                                    return@Button
                                }
                                val created = StoredAuthAccount(
                                    id = "user_${System.currentTimeMillis()}",
                                    fullName = cleanName,
                                    email = cleanEmail,
                                    password = password,
                                    role = selectedRole,
                                    vehicleType = selectedVehicle,
                                    plateNumber = if (selectedRole == UserRole.PASSENGER) "" else plateNumber.trim().ifBlank { "BYH-2026" },
                                    authProvider = "member"
                                )
                                saveStoredAccounts(context, listOf(created) + accounts)
                                completeAuthentication(created.toProfile(), "form")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ByaheroDarkGreen,
                            contentColor = ByaheroWhite
                        )
                    ) {
                        if (submittingKind == "form") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ByaheroWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (mode == "login") "Signing In..." else "Creating Account...",
                                fontFamily = PoppinsFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = if (mode == "login") "Log In" else "Create Account",
                                fontFamily = PoppinsFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ByaheroBorder)
                        Text(
                            text = "or",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ByaheroMuted
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ByaheroBorder)
                    }

                    OutlinedButton(
                        enabled = submittingKind == null,
                        onClick = {
                            errorMsg = null
                            val accounts = loadStoredAccounts(context)
                            val googleAcc = accounts.firstOrNull {
                                it.email.equals("alex@byahero.app", ignoreCase = true)
                            } ?: DefaultSeedAccounts[2]
                            completeAuthentication(googleAcc.toProfile(), "google")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ByaheroBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ByaheroWhite,
                            contentColor = ByaheroBlack
                        )
                    ) {
                        if (submittingKind == "google") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ByaheroDarkGreen
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Connecting Google...",
                                fontFamily = PoppinsFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ByaheroBlack
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_g),
                                contentDescription = "Google",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                fontFamily = PoppinsFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ByaheroBlack
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ByaheroAuthInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ByaheroMuted
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ByaheroSubtleGlass)
                .border(1.dp, ByaheroBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = ByaheroDarkGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = PoppinsFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFF86948C)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = PoppinsFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = ByaheroBlack
                    ),
                    cursorBrush = SolidColor(ByaheroDarkGreen),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (isPassword && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword && onTogglePassword != null) {
                IconButton(
                    onClick = onTogglePassword,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = ByaheroMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
