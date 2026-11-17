package com.hddev.byahero.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.hddev.byahero.data.ActiveMode
import com.hddev.byahero.data.ActiveRide
import com.hddev.byahero.data.NearbySpot
import com.hddev.byahero.data.PlaceSearchHelper
import com.hddev.byahero.data.RideStatus
import com.hddev.byahero.data.UserProfile
import com.hddev.byahero.data.UserRole
import com.hddev.byahero.data.VehicleType
import com.hddev.byahero.nativeengine.ByaheroNativeEngine
import com.hddev.byahero.ui.components.InteractiveOsmMap
import com.hddev.byahero.ui.theme.ByaheroBlack
import com.hddev.byahero.ui.theme.ByaheroBorder
import com.hddev.byahero.ui.theme.ByaheroCanvas
import com.hddev.byahero.ui.theme.ByaheroDarkGreen
import com.hddev.byahero.ui.theme.ByaheroEmerald
import com.hddev.byahero.ui.theme.ByaheroGlassWhite
import com.hddev.byahero.ui.theme.ByaheroMuted
import com.hddev.byahero.ui.theme.ByaheroSubtleGlass
import com.hddev.byahero.ui.theme.ByaheroWhite
import com.hddev.byahero.ui.theme.PoppinsFontFamily
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

private enum class MapPickMode {
    NONE,
    PICKUP,
    DESTINATION
}

@SuppressLint("MissingPermission")
@Composable
fun MainScreen(
    profile: UserProfile,
    onUpdateProfile: (UserProfile) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val useGoogleMaps = remember { PlaceSearchHelper.hasValidMapsKey }

    var selectedTab by remember(profile.id) {
        mutableStateOf(if (profile.activeMode == ActiveMode.RIDER) 2 else 0)
    }

    var userLocation by remember { mutableStateOf(LatLng(14.5995, 120.9842)) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission =
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var pickupLabel by remember { mutableStateOf("Current GPS Location") }
    var pickupLatLng by remember { mutableStateOf(userLocation) }
    var destLabel by remember { mutableStateOf("") }
    var destLatLng by remember { mutableStateOf<LatLng?>(null) }
    var mapPickMode by remember { mutableStateOf(MapPickMode.NONE) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val live = LatLng(loc.latitude, loc.longitude)
                    userLocation = live
                    if (pickupLabel == "Current GPS Location") {
                        pickupLatLng = live
                    }
                }
            }
        }
    }

    var selectedVehicle by remember { mutableStateOf(VehicleType.MOTORCYCLE) }
    val actualSaturday = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
    }
    var saturdayPreview by remember { mutableStateOf(actualSaturday) }
    val isSaturdayPromo = actualSaturday || saturdayPreview

    var searchQuery by remember { mutableStateOf("") }
    var discoveredSpots by remember { mutableStateOf<List<NearbySpot>>(emptyList()) }
    var exploreTitle by remember { mutableStateOf("Explore Nearby") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var activeRide by remember { mutableStateOf<ActiveRide?>(null) }

    val seedRequests = remember(isSaturdayPromo) {
        listOf(
            ActiveRide(
                id = "req_101",
                passengerName = "Marco Santos",
                vehicleType = VehicleType.MOTORCYCLE,
                pickupLabel = "Ayala Triangle Gardens",
                pickupLat = 14.5568,
                pickupLng = 121.0235,
                destinationLabel = "SM Mall of Asia",
                destLat = 14.5351,
                destLng = 120.9822,
                distanceKm = 6.4,
                durationMins = 19,
                fare = ByaheroNativeEngine.computeFareQuote(6.4, VehicleType.MOTORCYCLE, isSaturdayPromo),
                status = RideStatus.SEARCHING
            ),
            ActiveRide(
                id = "req_102",
                passengerName = "Claire Mendoza",
                vehicleType = VehicleType.TAXI,
                pickupLabel = "Bonifacio High Street",
                pickupLat = 14.5509,
                pickupLng = 121.0503,
                destinationLabel = "NAIA Terminal 3",
                destLat = 14.5194,
                destLng = 121.0155,
                distanceKm = 8.2,
                durationMins = 24,
                fare = ByaheroNativeEngine.computeFareQuote(8.2, VehicleType.TAXI, isSaturdayPromo),
                status = RideStatus.SEARCHING
            )
        )
    }
    var incomingRequests by remember { mutableStateOf(seedRequests) }
    var activeRiderJob by remember { mutableStateOf<ActiveRide?>(null) }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 14f)
    }

    LaunchedEffect(userLocation) {
        if (destLatLng == null) {
            cameraState.position = CameraPosition.fromLatLngZoom(userLocation, 14.5f)
        }
    }

    val routeDistanceKm = remember(pickupLatLng, destLatLng) {
        destLatLng?.let { dest ->
            val raw = ByaheroNativeEngine.calculateHaversineKm(
                pickupLatLng.latitude,
                pickupLatLng.longitude,
                dest.latitude,
                dest.longitude
            )
            max(0.8, raw * 1.25)
        } ?: 0.0
    }

    val durationMins = remember(routeDistanceKm, selectedVehicle) {
        if (routeDistanceKm <= 0.0) 0
        else {
            val speed = if (selectedVehicle == VehicleType.MOTORCYCLE) 28.0 else 22.0
            max(4, ((routeDistanceKm / speed) * 60.0).roundToInt())
        }
    }

    val fareQuote = remember(routeDistanceKm, selectedVehicle, isSaturdayPromo) {
        if (routeDistanceKm > 0.0) {
            ByaheroNativeEngine.computeFareQuote(routeDistanceKm, selectedVehicle, isSaturdayPromo)
        } else null
    }

    fun runSearch(category: String, query: String, label: String, openExplore: Boolean = false) {
        scope.launch {
            isSearching = true
            searchError = null
            exploreTitle = label
            if (openExplore) selectedTab = 1
            val results = PlaceSearchHelper.searchPlaces(context, query, category, userLocation)
            discoveredSpots = results
            if (results.isEmpty()) {
                searchError = "No nearby results found for $label. Try another query."
            } else {
                cameraState.position = CameraPosition.fromLatLngZoom(
                    LatLng(results.first().lat, results.first().lng),
                    14.5f
                )
            }
            isSearching = false
        }
    }

    fun handleMapTap(coords: LatLng) {
        when (mapPickMode) {
            MapPickMode.PICKUP -> {
                pickupLatLng = coords
                pickupLabel = String.format("Pinned (%.4f, %.4f)", coords.latitude, coords.longitude)
                mapPickMode = MapPickMode.NONE
            }
            MapPickMode.DESTINATION, MapPickMode.NONE -> {
                destLatLng = coords
                destLabel = String.format("Pinned (%.4f, %.4f)", coords.latitude, coords.longitude)
                mapPickMode = MapPickMode.NONE
            }
        }
    }

    Scaffold(
        containerColor = ByaheroCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = ByaheroDarkGreen,
                contentColor = ByaheroWhite,
                tonalElevation = 8.dp
            ) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ByaheroWhite,
                    selectedTextColor = ByaheroWhite,
                    unselectedIconColor = ByaheroWhite.copy(alpha = 0.68f),
                    unselectedTextColor = ByaheroWhite.copy(alpha = 0.68f),
                    indicatorColor = ByaheroWhite.copy(alpha = 0.2f)
                )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Navigation, contentDescription = "Ride") },
                    label = { Text("Ride", fontFamily = PoppinsFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                    label = { Text("Explore", fontFamily = PoppinsFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.TwoWheeler, contentDescription = "Rider") },
                    label = { Text("Rider", fontFamily = PoppinsFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                    label = { Text("Account", fontFamily = PoppinsFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = navColors
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Map Workspace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (selectedTab == 0 || selectedTab == 1) 0.48f else 0.30f)
            ) {
                if (useGoogleMaps) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState,
                        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = false,
                            zoomControlsEnabled = false,
                            mapToolbarEnabled = false
                        ),
                        onMapClick = { handleMapTap(it) }
                    ) {
                        Marker(
                            state = MarkerState(position = pickupLatLng),
                            title = "Pickup: $pickupLabel"
                        )
                        destLatLng?.let { d ->
                            Marker(
                                state = MarkerState(position = d),
                                title = "Drop-off: $destLabel"
                            )
                            Polyline(
                                points = listOf(pickupLatLng, d),
                                color = ByaheroDarkGreen,
                                width = 10f
                            )
                        }
                        discoveredSpots.forEach { spot ->
                            Marker(
                                state = MarkerState(position = LatLng(spot.lat, spot.lng)),
                                title = spot.name,
                                snippet = String.format("%.2f km away", spot.distanceKm),
                                onClick = {
                                    destLatLng = LatLng(spot.lat, spot.lng)
                                    destLabel = spot.name
                                    false
                                }
                            )
                        }
                    }
                } else {
                    InteractiveOsmMap(
                        modifier = Modifier.fillMaxSize(),
                        userLocation = userLocation,
                        pickupLocation = pickupLatLng,
                        destinationLocation = destLatLng,
                        discoveredSpots = discoveredSpots,
                        onMapClick = { handleMapTap(it) }
                    )
                }

                // Floating Frosted Search Bar + Chips safely below system status bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ByaheroGlassWhite,
                        border = BorderStroke(1.dp, Color.White),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = ByaheroDarkGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search any location, McDonald's, resort...",
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 12.sp,
                                        color = ByaheroMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 13.sp,
                                        color = ByaheroBlack
                                    ),
                                    cursorBrush = SolidColor(ByaheroDarkGreen),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = ByaheroMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ByaheroDarkGreen)
                                    .clickable {
                                        val q = searchQuery.trim()
                                        if (q.isNotEmpty()) {
                                            runSearch("search", q, "Results for \"$q\"", openExplore = true)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Find",
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ByaheroWhite
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapQuickChip(
                            label = "Fast Food",
                            icon = Icons.Default.Fastfood,
                            onClick = {
                                runSearch(
                                    "fast_food",
                                    "Jollibee McDonalds KFC Burger King",
                                    "Nearby Fast Food",
                                    openExplore = true
                                )
                            }
                        )
                        MapQuickChip(
                            label = "Resorts & Beaches",
                            icon = Icons.Default.BeachAccess,
                            onClick = {
                                runSearch(
                                    "resort_beach",
                                    "beach resort swimming resort",
                                    "Nearest Resorts & Beaches",
                                    openExplore = true
                                )
                            }
                        )
                        Surface(
                            onClick = {
                                if (hasLocationPermission) {
                                    val fused = LocationServices.getFusedLocationProviderClient(context)
                                    fused.lastLocation.addOnSuccessListener { loc ->
                                        if (loc != null) {
                                            val next = LatLng(loc.latitude, loc.longitude)
                                            userLocation = next
                                            pickupLatLng = next
                                            pickupLabel = "Current GPS Location"
                                            cameraState.position = CameraPosition.fromLatLngZoom(next, 15f)
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = ByaheroDarkGreen,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "My GPS",
                                    tint = ByaheroWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "My GPS",
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ByaheroWhite
                                )
                            }
                        }
                    }

                    if (mapPickMode != MapPickMode.NONE) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ByaheroDarkGreen,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (mapPickMode == MapPickMode.PICKUP) {
                                        "Tap map to set Pickup"
                                    } else {
                                        "Tap map to set Destination"
                                    },
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ByaheroWhite
                                )
                                Text(
                                    text = "Cancel",
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA7F3D0),
                                    modifier = Modifier.clickable { mapPickMode = MapPickMode.NONE }
                                )
                            }
                        }
                    }
                }
            }

            // Frosted White Sheet Content
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (selectedTab == 0 || selectedTab == 1) 0.52f else 0.70f),
                color = ByaheroCanvas,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Saturday 10% + 5% Banner
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ByaheroDarkGreen
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isSaturdayPromo) "Saturday Perks Active" else "Saturday Travel Perks",
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ByaheroWhite
                                        )
                                        Text(
                                            text = "10% passenger discount + 5% rider bonus",
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 11.sp,
                                            color = Color(0xFFD1FAE5)
                                        )
                                    }
                                    if (!actualSaturday) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (saturdayPreview) ByaheroWhite else ByaheroWhite.copy(alpha = 0.2f))
                                                .clickable { saturdayPreview = !saturdayPreview }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (saturdayPreview) "Sat On" else "Preview Sat",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (saturdayPreview) ByaheroDarkGreen else ByaheroWhite
                                            )
                                        }
                                    }
                                }
                            }

                            if (activeRide != null) {
                                ActiveRideTrackerCard(
                                    ride = activeRide!!,
                                    onAdvance = {
                                        activeRide = when (activeRide!!.status) {
                                            RideStatus.SEARCHING -> activeRide!!.copy(
                                                status = RideStatus.ACCEPTED,
                                                riderName = if (activeRide!!.vehicleType == VehicleType.MOTORCYCLE) "Kuya Carlo D." else "Kuya Ramon V.",
                                                riderPlate = if (activeRide!!.vehicleType == VehicleType.MOTORCYCLE) "MTO-8421" else "TXI-3902"
                                            )
                                            RideStatus.ACCEPTED -> activeRide!!.copy(status = RideStatus.ARRIVING)
                                            RideStatus.ARRIVING -> activeRide!!.copy(status = RideStatus.IN_PROGRESS)
                                            RideStatus.IN_PROGRESS -> activeRide!!.copy(status = RideStatus.COMPLETED)
                                            RideStatus.COMPLETED -> null
                                        }
                                    },
                                    onCancel = { activeRide = null }
                                )
                            } else {
                                // Stops Box
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ByaheroWhite,
                                    border = BorderStroke(1.dp, ByaheroBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(ByaheroDarkGreen)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "PICKUP",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ByaheroMuted
                                                )
                                                Text(
                                                    text = pickupLabel,
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ByaheroBlack,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            SmallChipButton("GPS") {
                                                pickupLatLng = userLocation
                                                pickupLabel = "Current GPS Location"
                                            }
                                            SmallChipButton("Map") {
                                                mapPickMode = MapPickMode.PICKUP
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = ByaheroDarkGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "DESTINATION",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ByaheroMuted
                                                )
                                                Text(
                                                    text = destLabel.ifBlank { "Tap map or Explore to set destination" },
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (destLabel.isBlank()) ByaheroMuted else ByaheroBlack,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (destLatLng != null) {
                                                IconButton(
                                                    onClick = {
                                                        val tmpLat = pickupLatLng
                                                        val tmpLabel = pickupLabel
                                                        pickupLatLng = destLatLng!!
                                                        pickupLabel = destLabel
                                                        destLatLng = tmpLat
                                                        destLabel = tmpLabel
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SwapVert,
                                                        contentDescription = "Swap",
                                                        tint = ByaheroDarkGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            SmallChipButton("Map") {
                                                mapPickMode = MapPickMode.DESTINATION
                                            }
                                        }
                                    }
                                }

                                // Vehicle Cards
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    VehicleChoiceCard(
                                        modifier = Modifier.weight(1f),
                                        title = "BYAHERO Moto",
                                        subtitle = "P40 base · Fast commute",
                                        icon = Icons.Default.TwoWheeler,
                                        selected = selectedVehicle == VehicleType.MOTORCYCLE,
                                        onClick = { selectedVehicle = VehicleType.MOTORCYCLE }
                                    )
                                    VehicleChoiceCard(
                                        modifier = Modifier.weight(1f),
                                        title = "BYAHERO Cab",
                                        subtitle = "P60 base · AC Taxi",
                                        icon = Icons.Default.DirectionsCar,
                                        selected = selectedVehicle == VehicleType.TAXI,
                                        onClick = { selectedVehicle = VehicleType.TAXI }
                                    )
                                }

                                if (fareQuote != null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = ByaheroWhite,
                                        border = BorderStroke(1.dp, ByaheroBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = String.format("%.2f km · ~%d min", routeDistanceKm, durationMins),
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ByaheroBlack
                                                )
                                                Text(
                                                    text = String.format("+P%.2f rider incentive", fareQuote.riderIncentive),
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 11.sp,
                                                    color = ByaheroDarkGreen
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = String.format("P%.2f", fareQuote.finalFare),
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = ByaheroDarkGreen
                                                )
                                                if (fareQuote.passengerDiscount > 0) {
                                                    Text(
                                                        text = String.format("Saved P%.2f (10%%)", fareQuote.passengerDiscount),
                                                        fontFamily = PoppinsFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ByaheroEmerald
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            activeRide = ActiveRide(
                                                id = "ride_${System.currentTimeMillis()}",
                                                passengerName = profile.fullName,
                                                vehicleType = selectedVehicle,
                                                pickupLabel = pickupLabel,
                                                pickupLat = pickupLatLng.latitude,
                                                pickupLng = pickupLatLng.longitude,
                                                destinationLabel = destLabel,
                                                destLat = destLatLng!!.latitude,
                                                destLng = destLatLng!!.longitude,
                                                distanceKm = routeDistanceKm,
                                                durationMins = durationMins,
                                                fare = fareQuote,
                                                status = RideStatus.SEARCHING
                                            )
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
                                        Text(
                                            text = if (selectedVehicle == VehicleType.MOTORCYCLE) "Book BYAHERO Moto" else "Book BYAHERO Taxi",
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ExploreCategoryButton(
                                    modifier = Modifier.weight(1f),
                                    title = "Fast Food Nearby",
                                    subtitle = "McDonald's, Jollibee, KFC",
                                    icon = Icons.Default.Fastfood
                                ) {
                                    runSearch(
                                        "fast_food",
                                        "Jollibee McDonalds KFC Burger King",
                                        "Nearby Fast Food"
                                    )
                                }
                                ExploreCategoryButton(
                                    modifier = Modifier.weight(1f),
                                    title = "Resorts & Beaches",
                                    subtitle = "Coastal & pool spots",
                                    icon = Icons.Default.BeachAccess
                                ) {
                                    runSearch(
                                        "resort_beach",
                                        "beach resort swimming resort",
                                        "Nearest Resorts & Beaches"
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ByaheroDarkGreen
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = exploreTitle,
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ByaheroWhite
                                    )
                                    Text(
                                        text = if (isSearching) "Searching..." else "${discoveredSpots.size} found",
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 11.sp,
                                        color = Color(0xFFD1FAE5)
                                    )
                                }
                            }

                            if (isSearching) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = ByaheroDarkGreen)
                                }
                            } else if (searchError != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                ) {
                                    Text(
                                        text = searchError!!,
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 12.sp,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else if (discoveredSpots.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ByaheroWhite,
                                    border = BorderStroke(1.dp, ByaheroBorder)
                                ) {
                                    Text(
                                        text = "Tap Fast Food Nearby or Resorts & Beaches above, or search any location in the map bar.",
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 12.sp,
                                        color = ByaheroMuted,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                discoveredSpots.forEach { spot ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = ByaheroWhite,
                                        border = BorderStroke(1.dp, ByaheroBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = spot.name,
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ByaheroBlack,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = spot.address.ifBlank { "Location on map" },
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 11.sp,
                                                    color = ByaheroMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = String.format("%.2f km away", spot.distanceKm),
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ByaheroDarkGreen
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    destLatLng = LatLng(spot.lat, spot.lng)
                                                    destLabel = spot.name
                                                    selectedTab = 0
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = ByaheroDarkGreen,
                                                    contentColor = ByaheroWhite
                                                )
                                            ) {
                                                Text(
                                                    text = "Ride Here",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            if (profile.role == UserRole.PASSENGER) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = ByaheroWhite,
                                    border = BorderStroke(1.dp, ByaheroBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Become a BYAHERO Rider",
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ByaheroBlack
                                        )
                                        Text(
                                            text = "Turn on Rider Mode alongside your passenger account and earn +5% extra incentive on every completed trip.",
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 12.sp,
                                            color = ByaheroMuted
                                        )
                                        Button(
                                            onClick = {
                                                onUpdateProfile(
                                                    profile.copy(
                                                        role = UserRole.BOTH,
                                                        activeMode = ActiveMode.RIDER,
                                                        isOnline = true
                                                    )
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ByaheroDarkGreen,
                                                contentColor = ByaheroWhite
                                            )
                                        ) {
                                            Text(
                                                text = "Enable Passenger & Rider Dual Role",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ByaheroWhite,
                                    border = BorderStroke(1.dp, ByaheroBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${profile.vehicleType.name.lowercase().replaceFirstChar { it.uppercase() }} Partner · ${profile.plateNumber}",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ByaheroBlack
                                            )
                                            Text(
                                                text = if (profile.isOnline) "Online · Accepting requests" else "Offline",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 11.sp,
                                                color = ByaheroDarkGreen
                                            )
                                        }
                                        SmallChipButton(if (profile.isOnline) "Go Offline" else "Go Online") {
                                            onUpdateProfile(profile.copy(isOnline = !profile.isOnline))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = ByaheroWhite,
                                        border = BorderStroke(1.dp, ByaheroBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Total Payouts",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 11.sp,
                                                color = ByaheroMuted
                                            )
                                            Text(
                                                text = String.format("P%.2f", profile.riderEarningsPhp),
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ByaheroBlack
                                            )
                                            Text(
                                                text = "${profile.totalRides} completed",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 11.sp,
                                                color = ByaheroMuted
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = ByaheroDarkGreen
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "+5% Rider Bonus",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 11.sp,
                                                color = Color(0xFFD1FAE5)
                                            )
                                            Text(
                                                text = String.format("+P%.2f", profile.riderIncentivesPhp),
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ByaheroWhite
                                            )
                                            Text(
                                                text = "Extra incentives",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 11.sp,
                                                color = Color(0xFFA7F3D0)
                                            )
                                        }
                                    }
                                }

                                if (activeRiderJob != null) {
                                    val job = activeRiderJob!!
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = ByaheroWhite,
                                        border = BorderStroke(1.5.dp, ByaheroDarkGreen)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Active Job · ${job.passengerName}",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ByaheroDarkGreen
                                            )
                                            Text(
                                                text = "${job.pickupLabel} -> ${job.destinationLabel}",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 12.sp,
                                                color = ByaheroBlack
                                            )
                                            Text(
                                                text = String.format(
                                                    "Payout: P%.2f (+P%.2f bonus)",
                                                    job.fare.finalFare + job.fare.riderIncentive,
                                                    job.fare.riderIncentive
                                                ),
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ByaheroDarkGreen
                                            )
                                            Button(
                                                onClick = {
                                                    val payout = job.fare.finalFare + job.fare.riderIncentive
                                                    onUpdateProfile(
                                                        profile.copy(
                                                            totalRides = profile.totalRides + 1,
                                                            riderEarningsPhp = profile.riderEarningsPhp + payout,
                                                            riderIncentivesPhp = profile.riderIncentivesPhp + job.fare.riderIncentive
                                                        )
                                                    )
                                                    activeRiderJob = null
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = ByaheroDarkGreen,
                                                    contentColor = ByaheroWhite
                                                )
                                            ) {
                                                Text(
                                                    text = "Complete & Claim +5% Bonus",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Nearby Ride Requests",
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ByaheroBlack
                                    )
                                    incomingRequests.forEach { req ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = ByaheroWhite,
                                            border = BorderStroke(1.dp, ByaheroBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = req.passengerName,
                                                            fontFamily = PoppinsFontFamily,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ByaheroBlack
                                                        )
                                                        Text(
                                                            text = "${req.vehicleType.name.lowercase().replaceFirstChar { it.uppercase() }} · ${String.format("%.1f km", req.distanceKm)}",
                                                            fontFamily = PoppinsFontFamily,
                                                            fontSize = 11.sp,
                                                            color = ByaheroMuted
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = String.format("P%.2f", req.fare.finalFare + req.fare.riderIncentive),
                                                            fontFamily = PoppinsFontFamily,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = ByaheroDarkGreen
                                                        )
                                                        Text(
                                                            text = String.format("+P%.2f (+5%%)", req.fare.riderIncentive),
                                                            fontFamily = PoppinsFontFamily,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = ByaheroEmerald
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${req.pickupLabel} -> ${req.destinationLabel}",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 12.sp,
                                                    color = ByaheroMuted
                                                )
                                                Button(
                                                    onClick = {
                                                        incomingRequests = incomingRequests.filter { it.id != req.id }
                                                        activeRiderJob = req.copy(
                                                            status = RideStatus.ACCEPTED,
                                                            riderName = profile.fullName,
                                                            riderPlate = profile.plateNumber
                                                        )
                                                        pickupLatLng = LatLng(req.pickupLat, req.pickupLng)
                                                        pickupLabel = req.pickupLabel
                                                        destLatLng = LatLng(req.destLat, req.destLng)
                                                        destLabel = req.destinationLabel
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(42.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = ByaheroDarkGreen,
                                                        contentColor = ByaheroWhite
                                                    )
                                                ) {
                                                    Text(
                                                        text = "Accept Ride",
                                                        fontFamily = PoppinsFontFamily,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Profile Hero Card
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = ByaheroDarkGreen
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = profile.fullName,
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ByaheroWhite
                                    )
                                    Text(
                                        text = profile.email,
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 12.sp,
                                        color = Color(0xFFD1FAE5)
                                    )
                                    Text(
                                        text = if (profile.authProvider == "google") "Google Account Connected" else "Registered BYAHERO Member",
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
                            }

                            // Role & Active Mode Switcher
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ByaheroWhite,
                                border = BorderStroke(1.dp, ByaheroBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Account Role",
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
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
                                            val active = profile.role == role
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (active) ByaheroDarkGreen else ByaheroSubtleGlass)
                                                    .clickable {
                                                        val nextMode = when (role) {
                                                            UserRole.RIDER -> ActiveMode.RIDER
                                                            UserRole.PASSENGER -> ActiveMode.PASSENGER
                                                            UserRole.BOTH -> profile.activeMode
                                                        }
                                                        onUpdateProfile(
                                                            profile.copy(
                                                                role = role,
                                                                activeMode = nextMode,
                                                                isOnline = role != UserRole.PASSENGER
                                                            )
                                                        )
                                                    },
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

                                    if (profile.role == UserRole.BOTH) {
                                        Text(
                                            text = "Active Mode",
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ByaheroMuted
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                ActiveMode.PASSENGER to "Passenger Mode",
                                                ActiveMode.RIDER to "Rider Mode"
                                            ).forEach { (modeItem, title) ->
                                                val active = profile.activeMode == modeItem
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(40.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (active) ByaheroDarkGreen else ByaheroSubtleGlass)
                                                        .clickable {
                                                            onUpdateProfile(profile.copy(activeMode = modeItem))
                                                            selectedTab = if (modeItem == ActiveMode.RIDER) 2 else 0
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = title,
                                                        fontFamily = PoppinsFontFamily,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (active) ByaheroWhite else ByaheroBlack
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Perks & Summary
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ByaheroWhite,
                                border = BorderStroke(1.dp, ByaheroBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AccountInfoRow("Saturday Discount", "10% Off All Rides")
                                    AccountInfoRow("Rider Bonus", "+5% Extra Incentive")
                                    AccountInfoRow(
                                        "Registered Vehicle",
                                        "${profile.vehicleType.name.lowercase().replaceFirstChar { it.uppercase() }} · ${profile.plateNumber}"
                                    )
                                }
                            }

                            // Prominent Log Out Button returning to AuthGateScreen
                            OutlinedButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFFEF2F2),
                                    contentColor = Color(0xFFB91C1C)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Log Out",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Log Out",
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapQuickChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = ByaheroGlassWhite,
        border = BorderStroke(1.dp, Color.White),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ByaheroDarkGreen,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontFamily = PoppinsFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ByaheroBlack
            )
        }
    }
}

@Composable
private fun SmallChipButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ByaheroSubtleGlass)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = ByaheroDarkGreen
        )
    }
}

@Composable
private fun VehicleChoiceCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ByaheroDarkGreen else ByaheroWhite,
        border = BorderStroke(1.dp, if (selected) ByaheroDarkGreen else ByaheroBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ByaheroWhite else ByaheroDarkGreen,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) ByaheroWhite else ByaheroBlack
            )
            Text(
                text = subtitle,
                fontFamily = PoppinsFontFamily,
                fontSize = 10.sp,
                color = if (selected) Color(0xFFD1FAE5) else ByaheroMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExploreCategoryButton(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = ByaheroWhite,
        border = BorderStroke(1.dp, ByaheroBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ByaheroDarkGreen,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ByaheroBlack
            )
            Text(
                text = subtitle,
                fontFamily = PoppinsFontFamily,
                fontSize = 10.sp,
                color = ByaheroMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActiveRideTrackerCard(
    ride: ActiveRide,
    onAdvance: () -> Unit,
    onCancel: () -> Unit
) {
    val statusHeadline = when (ride.status) {
        RideStatus.SEARCHING -> "Finding Nearby BYAHERO Rider..."
        RideStatus.ACCEPTED -> "Rider Matched · ${ride.riderName ?: "BYAHERO Partner"}"
        RideStatus.ARRIVING -> "${ride.riderName ?: "Rider"} is arriving"
        RideStatus.IN_PROGRESS -> "On Trip to ${ride.destinationLabel}"
        RideStatus.COMPLETED -> "Trip Completed"
    }
    val actionLabel = when (ride.status) {
        RideStatus.SEARCHING -> "Match Rider Now"
        RideStatus.ACCEPTED -> "Simulate Arrival"
        RideStatus.ARRIVING -> "Start Trip"
        RideStatus.IN_PROGRESS -> "Complete Ride"
        RideStatus.COMPLETED -> "Book Another Ride"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ByaheroWhite,
        border = BorderStroke(1.5.dp, ByaheroDarkGreen)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = statusHeadline,
                fontFamily = PoppinsFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ByaheroDarkGreen
            )
            Text(
                text = "${ride.pickupLabel} -> ${ride.destinationLabel}",
                fontFamily = PoppinsFontFamily,
                fontSize = 12.sp,
                color = ByaheroBlack
            )
            if (ride.riderPlate != null) {
                Text(
                    text = "Plate: ${ride.riderPlate}",
                    fontFamily = PoppinsFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ByaheroMuted
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAdvance,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ByaheroDarkGreen,
                        contentColor = ByaheroWhite
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontFamily = PoppinsFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (ride.status != RideStatus.COMPLETED) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ByaheroBorder)
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 12.sp,
                            color = ByaheroMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontSize = 12.sp,
            color = ByaheroMuted
        )
        Text(
            text = value,
            fontFamily = PoppinsFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ByaheroDarkGreen
        )
    }
}
