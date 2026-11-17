package com.hddev.byahero.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.hddev.byahero.data.ActiveMode
import com.hddev.byahero.data.NearbySpot
import com.hddev.byahero.data.UserRole
import com.hddev.byahero.data.VehicleType
import com.hddev.byahero.nativeengine.ByaheroNativeEngine
import com.hddev.byahero.ui.theme.ByaheroBlack
import com.hddev.byahero.ui.theme.ByaheroCanvas
import com.hddev.byahero.ui.theme.ByaheroDarkGreen
import com.hddev.byahero.ui.theme.ByaheroGlassWhite
import com.hddev.byahero.ui.theme.ByaheroMuted
import com.hddev.byahero.ui.theme.ByaheroWhite
import java.util.Calendar

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var userRole by remember { mutableStateOf(UserRole.BOTH) }
    var activeMode by remember { mutableStateOf(ActiveMode.PASSENGER) }
    var vehicleType by remember { mutableStateOf(VehicleType.MOTORCYCLE) }

    var userLatLng by remember { mutableStateOf(LatLng(14.5995, 120.9842)) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var pickupLabel by remember { mutableStateOf("Current GPS Location") }
    var destinationLabel by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var discoveredSpots by remember { mutableStateOf<List<NearbySpot>>(emptyList()) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission =
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    userLatLng = LatLng(loc.latitude, loc.longitude)
                }
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLatLng, 14f)
    }

    val isSaturday = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
    }

    val distanceKm = remember(userLatLng, destinationLatLng) {
        val dest = destinationLatLng
        if (dest == null) 0.0
        else ByaheroNativeEngine.calculateHaversineKm(
            userLatLng.latitude,
            userLatLng.longitude,
            dest.latitude,
            dest.longitude
        ) * 1.25
    }

    val fareQuote = remember(distanceKm, vehicleType, isSaturday) {
        if (distanceKm <= 0.0) null
        else ByaheroNativeEngine.computeFareQuote(distanceKm, vehicleType, isSaturday)
    }

    fun searchPlacesViaSdk(query: String, category: String) {
        if (!Places.isInitialized()) return
        val placesClient = Places.createClient(context)
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.RATING
        )
        val request = SearchByTextRequest.builder(query, fields)
            .setLocationBias(CircularBounds.newInstance(userLatLng, 15000.0))
            .setMaxResultCount(10)
            .build()

        placesClient.searchByText(request).addOnSuccessListener { response ->
            discoveredSpots = response.places.mapNotNull { place ->
                val loc = place.location ?: return@mapNotNull null
                val dist = ByaheroNativeEngine.calculateHaversineKm(
                    userLatLng.latitude,
                    userLatLng.longitude,
                    loc.latitude,
                    loc.longitude
                )
                NearbySpot(
                    id = place.id ?: place.displayName.orEmpty(),
                    name = place.displayName ?: "Location",
                    address = place.formattedAddress ?: "",
                    lat = loc.latitude,
                    lng = loc.longitude,
                    rating = place.rating,
                    category = category,
                    distanceKm = dist
                )
            }
        }
    }

    Scaffold(
        containerColor = ByaheroCanvas,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ByaheroDarkGreen)
            ) {
                NavigationBar(containerColor = Color.Transparent) {
                    val navItems = listOf(
                        "Ride" to Icons.Default.LocationOn,
                        "Explore" to Icons.Default.Explore,
                        "Rider" to Icons.Default.TwoWheeler,
                        "Account" to Icons.Default.Person
                    )
                    navItems.forEachIndexed { index, pair ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(pair.second, contentDescription = pair.first) },
                            label = { Text(pair.first, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ByaheroWhite,
                                selectedTextColor = ByaheroWhite,
                                unselectedIconColor = ByaheroWhite.copy(alpha = 0.65f),
                                unselectedTextColor = ByaheroWhite.copy(alpha = 0.65f),
                                indicatorColor = ByaheroWhite.copy(alpha = 0.18f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Interactive Google Map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    onMapClick = { latLng ->
                        destinationLatLng = latLng
                        destinationLabel = "Pinned (${String.format("%.4f", latLng.latitude)}, ${String.format("%.4f", latLng.longitude)})"
                    }
                ) {
                    Marker(
                        state = MarkerState(position = userLatLng),
                        title = "Pickup: $pickupLabel"
                    )
                    destinationLatLng?.let { dest ->
                        Marker(
                            state = MarkerState(position = dest),
                            title = "Destination: $destinationLabel"
                        )
                    }
                    discoveredSpots.forEach { spot ->
                        Marker(
                            state = MarkerState(position = LatLng(spot.lat, spot.lng)),
                            title = spot.name,
                            snippet = spot.address
                        )
                    }
                }

                // Floating Frosted White Glass Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ByaheroGlassWhite)
                        .border(1.dp, Color.White, RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ByaheroDarkGreen)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search your favourite place, food, resort", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                searchPlacesViaSdk(searchQuery, "Search")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ByaheroDarkGreen)
                    ) {
                        Text("Find", color = ByaheroWhite, fontSize = 12.sp)
                    }
                }
            }

            // Bottom Frosted Glass Sheet Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ByaheroGlassWhite)
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        Text(
                            text = if (isSaturday) "Saturday 10% Discount Active" else "BYAHERO Ride · Sat 10% Off + 5% Rider Bonus",
                            color = ByaheroDarkGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Pickup: $pickupLabel", color = ByaheroBlack, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (destinationLabel.isBlank()) "Tap map or Explore to set destination" else "Drop-off: $destinationLabel",
                            color = ByaheroMuted,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vehicleType = VehicleType.MOTORCYCLE },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (vehicleType == VehicleType.MOTORCYCLE) ByaheroDarkGreen else Color(0xFFEBF1ED),
                                    contentColor = if (vehicleType == VehicleType.MOTORCYCLE) ByaheroWhite else ByaheroBlack
                                )
                            ) {
                                Icon(Icons.Default.TwoWheeler, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Moto")
                            }
                            Button(
                                onClick = { vehicleType = VehicleType.TAXI },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (vehicleType == VehicleType.TAXI) ByaheroDarkGreen else Color(0xFFEBF1ED),
                                    contentColor = if (vehicleType == VehicleType.TAXI) ByaheroWhite else ByaheroBlack
                                )
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Taxi")
                            }
                        }
                        if (fareQuote != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Fare: PHP ${fareQuote.finalFare.toInt()}  |  Rider Bonus: +PHP ${String.format("%.2f", fareQuote.riderIncentive)}",
                                color = ByaheroBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    1 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    searchPlacesViaSdk(
                                        "McDonalds Jollibee KFC Burger King fast food",
                                        "Fast Food"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ByaheroDarkGreen)
                            ) {
                                Icon(Icons.Default.Fastfood, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Fast Food", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { searchPlacesViaSdk("beach resort swimming resort", "Resort & Beach") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ByaheroDarkGreen)
                            ) {
                                Icon(Icons.Default.Water, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Resorts", fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.height(140.dp)) {
                            items(discoveredSpots) { spot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            destinationLatLng = LatLng(spot.lat, spot.lng)
                                            destinationLabel = spot.name
                                            selectedTab = 0
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(spot.name, color = ByaheroBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(spot.address, color = ByaheroMuted, fontSize = 11.sp, maxLines = 1)
                                    }
                                    Text(
                                        text = "${String.format("%.1f", spot.distanceKm)} km",
                                        color = ByaheroDarkGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        Text(
                            text = "Rider Dispatch Mode (+5% Extra Incentive)",
                            color = ByaheroBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Active Role: ${userRole.name} · Current Mode: ${activeMode.name}",
                            color = ByaheroDarkGreen,
                            fontSize = 12.sp
                        )
                    }

                    else -> {
                        Text("BYAHERO Account & Role Switcher", color = ByaheroBlack, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UserRole.entries.forEach { role ->
                                Button(
                                    onClick = {
                                        userRole = role
                                        activeMode = if (role == UserRole.RIDER) ActiveMode.RIDER else ActiveMode.PASSENGER
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (userRole == role) ByaheroDarkGreen else Color(0xFFEBF1ED),
                                        contentColor = if (userRole == role) ByaheroWhite else ByaheroBlack
                                    )
                                ) {
                                    Text(role.name, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
