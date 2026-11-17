package com.hddev.byahero.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.hddev.byahero.R
import com.hddev.byahero.data.ActiveMode
import com.hddev.byahero.data.ActiveRide
import com.hddev.byahero.data.NearbySpot
import com.hddev.byahero.data.OrderHistoryStore
import com.hddev.byahero.data.PlaceSearchHelper
import com.hddev.byahero.data.RideStatus
import com.hddev.byahero.data.UserProfile
import com.hddev.byahero.data.UserRole
import com.hddev.byahero.data.VehicleType
import com.hddev.byahero.nativeengine.ByaheroNativeEngine
import com.hddev.byahero.ui.components.NativeInteractiveMap
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

private enum class MapPickMode {
    NONE,
    PICKUP,
    DESTINATION
}

private enum class FocusedStopField {
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
        mutableStateOf(if (profile.activeMode == ActiveMode.RIDER) 3 else 0)
    }
    var fullMapExpanded by remember { mutableStateOf(false) }

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

    var pickupLabel by remember { mutableStateOf("Locating pickup place...") }
    var pickupLatLng by remember { mutableStateOf(userLocation) }
    var userEditedPickup by remember { mutableStateOf(false) }
    var destLabel by remember { mutableStateOf("") }
    var destLatLng by remember { mutableStateOf<LatLng?>(null) }
    var mapPickMode by remember { mutableStateOf(MapPickMode.NONE) }
    var recenterSeq by remember { mutableStateOf(0) }
    var focusTarget by remember { mutableStateOf<LatLng?>(null) }
    var focusSeq by remember { mutableStateOf(0) }
    var selectedMapSpot by remember { mutableStateOf<NearbySpot?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    var orderHistory by remember(profile.id) {
        mutableStateOf(OrderHistoryStore.load(context, profile.fullName))
    }
    var historyFilter by remember { mutableStateOf("all") }

    LaunchedEffect(locationError) {
        if (locationError != null) {
            delay(4200)
            locationError = null
        }
    }

    fun isSameStop(
        lat1: Double,
        lng1: Double,
        label1: String,
        lat2: Double,
        lng2: Double,
        label2: String
    ): Boolean {
        val distKm = ByaheroNativeEngine.calculateHaversineKm(lat1, lng1, lat2, lng2)
        if (distKm < 0.05) return true
        val n1 = label1.trim().lowercase()
        val n2 = label2.trim().lowercase()
        val p1 = n1.isEmpty() || n1.startsWith("locating") || n1.startsWith("resolving")
        val p2 = n2.isEmpty() || n2.startsWith("locating") || n2.startsWith("resolving")
        return !p1 && !p2 && n1 == n2 && distKm < 0.25
    }

    fun trySetPickup(coords: LatLng, label: String): Boolean {
        val d = destLatLng
        if (d != null && isSameStop(coords.latitude, coords.longitude, label, d.latitude, d.longitude, destLabel)) {
            locationError = "Pickup and destination cannot be the same location."
            return false
        }
        locationError = null
        userEditedPickup = true
        pickupLatLng = coords
        pickupLabel = label
        return true
    }

    fun trySetDestination(coords: LatLng, label: String): Boolean {
        if (isSameStop(coords.latitude, coords.longitude, label, pickupLatLng.latitude, pickupLatLng.longitude, pickupLabel)) {
            locationError = "Destination cannot be the same as your pickup location."
            return false
        }
        locationError = null
        destLatLng = coords
        destLabel = label
        return true
    }

    fun recordOrderInHistory(entry: ActiveRide) {
        val updated = listOf(entry) + orderHistory.filter { it.id != entry.id }
        orderHistory = updated
        OrderHistoryStore.save(context, updated)
    }

    // Direct Stop Search State inside Booking Card
    var focusedStopField by remember { mutableStateOf(FocusedStopField.NONE) }
    var pickupInputQuery by remember { mutableStateOf("") }
    var destInputQuery by remember { mutableStateOf("") }
    var inlineStopResults by remember { mutableStateOf<List<NearbySpot>>(emptyList()) }
    var isSearchingStop by remember { mutableStateOf(false) }

    // Resolving real GPS place name on startup / location grant
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val live = LatLng(loc.latitude, loc.longitude)
                    userLocation = live
                    if (!userEditedPickup) {
                        pickupLatLng = live
                        recenterSeq++
                        scope.launch {
                            val (resolvedName, _) = PlaceSearchHelper.reverseGeocode(context, live)
                            if (!userEditedPickup) {
                                pickupLabel = resolvedName
                            }
                        }
                    }
                } else {
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { fresh ->
                            if (fresh != null) {
                                val live = LatLng(fresh.latitude, fresh.longitude)
                                userLocation = live
                                if (!userEditedPickup) {
                                    pickupLatLng = live
                                    recenterSeq++
                                    scope.launch {
                                        val (resolvedName, _) = PlaceSearchHelper.reverseGeocode(context, live)
                                        if (!userEditedPickup) {
                                            pickupLabel = resolvedName
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        } else {
            val (fallbackName, _) = PlaceSearchHelper.reverseGeocode(context, userLocation)
            if (!userEditedPickup && pickupLabel.startsWith("Locating")) {
                pickupLabel = fallbackName
            }
        }
    }

    // Debounced live search when typing inside Pickup or Destination field
    val activeStopSearchQuery = when (focusedStopField) {
        FocusedStopField.PICKUP -> pickupInputQuery
        FocusedStopField.DESTINATION -> destInputQuery
        FocusedStopField.NONE -> ""
    }
    LaunchedEffect(focusedStopField, activeStopSearchQuery) {
        val q = activeStopSearchQuery.trim()
        if (focusedStopField == FocusedStopField.NONE || q.length < 2) {
            inlineStopResults = emptyList()
            isSearchingStop = false
            return@LaunchedEffect
        }
        isSearchingStop = true
        delay(420)
        val found = PlaceSearchHelper.searchPlaces(context, q, "search", userLocation)
        inlineStopResults = found
        isSearchingStop = false
    }

    var selectedVehicle by remember { mutableStateOf(VehicleType.MOTORCYCLE) }
    val actualSaturday = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
    }
    var saturdayPreview by remember { mutableStateOf(actualSaturday) }
    val isSaturdayPromo = actualSaturday || saturdayPreview

    // Top Map Search Bar + Quick Picker
    var mapSearchQuery by remember { mutableStateOf("") }
    var mapSearchDropdownResults by remember { mutableStateOf<List<NearbySpot>>(emptyList()) }
    var showMapSearchDropdown by remember { mutableStateOf(false) }
    var discoveredSpots by remember { mutableStateOf<List<NearbySpot>>(emptyList()) }
    var exploreTitle by remember { mutableStateOf("Explore Nearby") }
    var isSearchingExplore by remember { mutableStateOf(false) }
    var exploreError by remember { mutableStateOf<String?>(null) }

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
        position = CameraPosition.fromLatLngZoom(userLocation, 14.5f)
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

    fun runExploreCategory(category: String, query: String, label: String, openExplore: Boolean) {
        scope.launch {
            isSearchingExplore = true
            exploreError = null
            exploreTitle = label
            showMapSearchDropdown = false
            if (openExplore) {
                selectedTab = 1
                fullMapExpanded = false
            }
            val results = PlaceSearchHelper.searchPlaces(context, query, category, userLocation)
            discoveredSpots = results
            if (results.isEmpty()) {
                exploreError = "No nearby results found for $label."
            } else {
                cameraState.position = CameraPosition.fromLatLngZoom(
                    LatLng(results.first().lat, results.first().lng),
                    14.5f
                )
            }
            isSearchingExplore = false
        }
    }

    fun runTopMapSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        scope.launch {
            isSearchingExplore = true
            val found = PlaceSearchHelper.searchPlaces(context, clean, "search", userLocation)
            mapSearchDropdownResults = found
            discoveredSpots = found
            showMapSearchDropdown = true
            if (found.isNotEmpty()) {
                cameraState.position = CameraPosition.fromLatLngZoom(
                    LatLng(found.first().lat, found.first().lng),
                    14.5f
                )
            }
            isSearchingExplore = false
        }
    }

    fun handleMapTap(coords: LatLng) {
        val targetMode = mapPickMode
        mapPickMode = MapPickMode.NONE
        showMapSearchDropdown = false
        val targetIsPickup = targetMode == MapPickMode.PICKUP
        val prevPickupLatLng = pickupLatLng
        val prevPickupLabel = pickupLabel
        val prevDestLatLng = destLatLng
        val prevDestLabel = destLabel

        selectedMapSpot = null
        if (targetIsPickup) {
            userEditedPickup = true
            pickupLatLng = coords
            pickupLabel = "Resolving place name..."
        } else {
            destLatLng = coords
            destLabel = "Resolving place name..."
        }

        scope.launch {
            val (resolvedName, _) = PlaceSearchHelper.reverseGeocode(context, coords)
            if (targetIsPickup) {
                val curDest = prevDestLatLng
                if (curDest != null && isSameStop(coords.latitude, coords.longitude, resolvedName, curDest.latitude, curDest.longitude, prevDestLabel)) {
                    pickupLatLng = prevPickupLatLng
                    pickupLabel = prevPickupLabel
                    locationError = "Pickup and destination cannot be the same location."
                    return@launch
                }
                locationError = null
                pickupLabel = resolvedName
            } else {
                if (isSameStop(coords.latitude, coords.longitude, resolvedName, prevPickupLatLng.latitude, prevPickupLatLng.longitude, prevPickupLabel)) {
                    destLatLng = prevDestLatLng
                    destLabel = prevDestLabel
                    locationError = "Destination cannot be the same as your pickup location."
                    return@launch
                }
                locationError = null
                destLabel = resolvedName
            }
            fullMapExpanded = false
        }
    }

    Scaffold(
        containerColor = ByaheroCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Brand Top Header matching Web Preview
            Surface(
                color = ByaheroGlassWhite,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(52.dp)
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_byahero_logo),
                            contentDescription = "BYAHERO",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "BYAHERO",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = ByaheroBlack
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (profile.role == UserRole.BOTH) {
                            Surface(
                                onClick = {
                                    val next = if (profile.activeMode == ActiveMode.PASSENGER) ActiveMode.RIDER else ActiveMode.PASSENGER
                                    onUpdateProfile(profile.copy(activeMode = next))
                                    selectedTab = if (next == ActiveMode.RIDER) 3 else 0
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = ByaheroDarkGreen
                            ) {
                                Text(
                                    text = if (profile.activeMode == ActiveMode.PASSENGER) "Passenger" else "Rider Mode",
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ByaheroWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = {
                                selectedTab = 4
                                fullMapExpanded = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = ByaheroWhite,
                            border = BorderStroke(1.dp, ByaheroBorder)
                        ) {
                            Text(
                                text = profile.fullName.substringBefore(" ").ifBlank { "Account" },
                                fontFamily = PoppinsFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ByaheroBlack,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = ByaheroDarkGreen,
                contentColor = ByaheroWhite,
                tonalElevation = 8.dp
            ) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ByaheroWhite,
                    selectedTextColor = ByaheroWhite,
                    unselectedIconColor = ByaheroWhite.copy(alpha = 0.7f),
                    unselectedTextColor = ByaheroWhite.copy(alpha = 0.7f),
                    indicatorColor = ByaheroWhite.copy(alpha = 0.2f)
                )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; fullMapExpanded = false },
                    icon = { Icon(Icons.Default.Navigation, contentDescription = "Ride") },
                    label = { Text("Ride", fontFamily = PoppinsFontFamily, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; fullMapExpanded = false },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                    label = { Text("Explore", fontFamily = PoppinsFontFamily, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; fullMapExpanded = false },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History", fontFamily = PoppinsFontFamily, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; fullMapExpanded = false },
                    icon = { Icon(Icons.Default.TwoWheeler, contentDescription = "Rider") },
                    label = { Text("Rider", fontFamily = PoppinsFontFamily, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4; fullMapExpanded = false },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                    label = { Text("Account", fontFamily = PoppinsFontFamily, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) },
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
            // Live Map Viewport
            val mapWeight = when {
                fullMapExpanded -> 1f
                selectedTab == 0 || selectedTab == 1 -> 0.42f
                else -> 0.26f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(mapWeight)
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
                    NativeInteractiveMap(
                        modifier = Modifier.fillMaxSize(),
                        userLocation = userLocation,
                        pickupLocation = pickupLatLng,
                        destinationLocation = destLatLng,
                        discoveredSpots = discoveredSpots,
                        recenterSeq = recenterSeq,
                        focusTarget = focusTarget,
                        focusSeq = focusSeq,
                        onSpotClick = { spot ->
                            selectedMapSpot = spot
                            focusTarget = LatLng(spot.lat, spot.lng)
                            focusSeq++
                        },
                        onMapClick = { handleMapTap(it) }
                    )
                }

                // Floating Frosted Search Bar & Place Picker Dropdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                if (mapSearchQuery.isEmpty()) {
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
                                    value = mapSearchQuery,
                                    onValueChange = { mapSearchQuery = it },
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
                            if (mapSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        mapSearchQuery = ""
                                        showMapSearchDropdown = false
                                    },
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
                                    .clickable { runTopMapSearch(mapSearchQuery) }
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

                    // Search Results Picker: Set any searched location as Pickup or Destination
                    if (showMapSearchDropdown) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ByaheroWhite,
                            border = BorderStroke(1.dp, ByaheroBorder),
                            shadowElevation = 10.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSearchingExplore) "Searching places..." else "${mapSearchDropdownResults.size} places found",
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ByaheroDarkGreen
                                    )
                                    Text(
                                        text = "Close",
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ByaheroMuted,
                                        modifier = Modifier.clickable { showMapSearchDropdown = false }
                                    )
                                }
                                HorizontalDivider(color = ByaheroBorder)

                                if (mapSearchDropdownResults.isEmpty() && !isSearchingExplore) {
                                    Text(
                                        text = "No matching places found. Try a broader place name.",
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 12.sp,
                                        color = ByaheroMuted,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }

                                mapSearchDropdownResults.forEach { spot ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
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
                                            text = "${spot.address.ifBlank { "Map location" }} · ${String.format("%.1f km", spot.distanceKm)}",
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 11.sp,
                                            color = ByaheroMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    if (trySetPickup(LatLng(spot.lat, spot.lng), spot.name)) {
                                                        focusTarget = LatLng(spot.lat, spot.lng)
                                                        focusSeq++
                                                        showMapSearchDropdown = false
                                                        selectedTab = 0
                                                        fullMapExpanded = false
                                                    } else {
                                                        showMapSearchDropdown = false
                                                        selectedTab = 0
                                                        fullMapExpanded = false
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp),
                                                shape = RoundedCornerShape(9.dp),
                                                border = BorderStroke(1.dp, ByaheroDarkGreen)
                                            ) {
                                                Text(
                                                    text = "Set Pickup",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ByaheroDarkGreen
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    if (trySetDestination(LatLng(spot.lat, spot.lng), spot.name)) {
                                                        focusTarget = LatLng(spot.lat, spot.lng)
                                                        focusSeq++
                                                        showMapSearchDropdown = false
                                                        selectedTab = 0
                                                        fullMapExpanded = false
                                                    } else {
                                                        showMapSearchDropdown = false
                                                        selectedTab = 0
                                                        fullMapExpanded = false
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp),
                                                shape = RoundedCornerShape(9.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = ByaheroDarkGreen,
                                                    contentColor = ByaheroWhite
                                                )
                                            ) {
                                                Text(
                                                    text = "Set Dest",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = ByaheroBorder)
                                }
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
                            label = "Fast Food Nearby",
                            icon = Icons.Default.Fastfood,
                            onClick = {
                                runExploreCategory(
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
                                runExploreCategory(
                                    "resort_beach",
                                    "beach resort swimming resort",
                                    "Nearest Resorts & Beaches",
                                    openExplore = true
                                )
                            }
                        )
                        Surface(
                            onClick = {
                                if (!hasLocationPermission) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    val fused = LocationServices.getFusedLocationProviderClient(context)
                                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                        .addOnSuccessListener { loc ->
                                            if (loc != null) {
                                                val next = LatLng(loc.latitude, loc.longitude)
                                                userLocation = next
                                                cameraState.position = CameraPosition.fromLatLngZoom(next, 15f)
                                                recenterSeq++
                                                val prevLatLng = pickupLatLng
                                                val prevLabel = pickupLabel
                                                pickupLatLng = next
                                                pickupLabel = "Locating current address..."
                                                scope.launch {
                                                    val (name, _) = PlaceSearchHelper.reverseGeocode(context, next)
                                                    if (!trySetPickup(next, name)) {
                                                        pickupLatLng = prevLatLng
                                                        pickupLabel = prevLabel
                                                    }
                                                }
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
                                        "Tap anywhere on the map to set Pickup"
                                    } else {
                                        "Tap anywhere on the map to set Destination"
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
                    selectedMapSpot?.let { activeSpot ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ByaheroWhite,
                            border = BorderStroke(1.dp, ByaheroDarkGreen),
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = activeSpot.name,
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ByaheroBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Dismiss",
                                        fontFamily = PoppinsFontFamily,
                                        fontSize = 11.sp,
                                        color = ByaheroMuted,
                                        modifier = Modifier.clickable { selectedMapSpot = null }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            trySetPickup(LatLng(activeSpot.lat, activeSpot.lng), activeSpot.name)
                                            selectedMapSpot = null
                                            selectedTab = 0
                                            fullMapExpanded = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp),
                                        shape = RoundedCornerShape(9.dp),
                                        border = BorderStroke(1.dp, ByaheroDarkGreen)
                                    ) {
                                        Text(
                                            text = "Set Pickup",
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ByaheroDarkGreen
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            trySetDestination(LatLng(activeSpot.lat, activeSpot.lng), activeSpot.name)
                                            selectedMapSpot = null
                                            selectedTab = 0
                                            fullMapExpanded = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp),
                                        shape = RoundedCornerShape(9.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ByaheroDarkGreen,
                                            contentColor = ByaheroWhite
                                        )
                                    ) {
                                        Text(
                                            text = "Set Dest",
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    locationError?.let { errMsg ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDC2626),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = ByaheroWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = errMsg,
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ByaheroWhite
                                )
                            }
                        }
                    }
                }
            }

            // Sheet Handle Bar matching Web Preview ("Book Motorcycle or Taxi" | "Full Map")
            Surface(
                color = ByaheroWhite,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedTab) {
                            0 -> "Book Motorcycle or Taxi"
                            1 -> "Fast Food & Resorts Nearby"
                            2 -> "Order History"
                            3 -> "Rider Dispatch (+5% Bonus)"
                            else -> "Account & Role"
                        },
                        fontFamily = PoppinsFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ByaheroDarkGreen
                    )
                    Surface(
                        onClick = { fullMapExpanded = !fullMapExpanded },
                        shape = RoundedCornerShape(8.dp),
                        color = ByaheroSubtleGlass
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (fullMapExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = ByaheroDarkGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (fullMapExpanded) "Show Panel" else "Full Map",
                                fontFamily = PoppinsFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ByaheroDarkGreen
                            )
                        }
                    }
                }
            }

            // Sheet Content
            if (!fullMapExpanded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (selectedTab == 0 || selectedTab == 1) 0.58f else 0.74f),
                    color = ByaheroCanvas
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // Saturday 10% + 5% Banner matching Web Preview
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ByaheroDarkGreen
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(ByaheroWhite.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Percent,
                                                    contentDescription = null,
                                                    tint = ByaheroWhite,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = if (isSaturdayPromo) "Saturday 10% Discount Active" else "Every Saturday: 10% Off + 5% Rider Bonus",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ByaheroWhite,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (actualSaturday) "Applied automatically today" else "Riders earn +5% extra on every ride",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFD1FAE5),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (!actualSaturday) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (saturdayPreview) ByaheroWhite else ByaheroWhite.copy(alpha = 0.18f))
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
                                            val current = activeRide!!
                                            val nextRide = when (current.status) {
                                                RideStatus.SEARCHING -> current.copy(
                                                    status = RideStatus.ACCEPTED,
                                                    riderName = if (current.vehicleType == VehicleType.MOTORCYCLE) "Kuya Carlo D." else "Kuya Ramon V.",
                                                    riderPlate = if (current.vehicleType == VehicleType.MOTORCYCLE) "MTO-8421" else "TXI-3902"
                                                )
                                                RideStatus.ACCEPTED -> current.copy(status = RideStatus.ARRIVING)
                                                RideStatus.ARRIVING -> current.copy(status = RideStatus.IN_PROGRESS)
                                                RideStatus.IN_PROGRESS -> {
                                                    val done = current.copy(status = RideStatus.COMPLETED)
                                                    recordOrderInHistory(done)
                                                    done
                                                }
                                                RideStatus.COMPLETED -> null
                                                RideStatus.CANCELLED -> null
                                            }
                                            activeRide = nextRide
                                        },
                                        onCancel = {
                                            val current = activeRide
                                            if (current != null && current.status != RideStatus.COMPLETED) {
                                                recordOrderInHistory(current.copy(status = RideStatus.CANCELLED))
                                            }
                                            activeRide = null
                                        }
                                    )
                                } else {
                                    // Searchable Pickup & Destination Cards + Floating Swap Circle Button
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            // Pickup Input Card
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = ByaheroWhite,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (focusedStopField == FocusedStopField.PICKUP) ByaheroDarkGreen else ByaheroBorder
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(ByaheroBlack)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Pickup Location",
                                                            fontFamily = PoppinsFontFamily,
                                                            fontSize = 10.sp,
                                                            color = ByaheroMuted
                                                        )
                                                        Box {
                                                            if (focusedStopField == FocusedStopField.PICKUP) {
                                                                if (pickupInputQuery.isEmpty()) {
                                                                    Text(
                                                                        text = pickupLabel,
                                                                        fontFamily = PoppinsFontFamily,
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = ByaheroMuted,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            } else if (pickupLabel.isEmpty()) {
                                                                Text(
                                                                    text = "Search pickup place",
                                                                    fontFamily = PoppinsFontFamily,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = ByaheroMuted
                                                                )
                                                            }
                                                            BasicTextField(
                                                                value = if (focusedStopField == FocusedStopField.PICKUP) pickupInputQuery else pickupLabel,
                                                                onValueChange = {
                                                                    focusedStopField = FocusedStopField.PICKUP
                                                                    pickupInputQuery = it
                                                                },
                                                                singleLine = true,
                                                                textStyle = TextStyle(
                                                                    fontFamily = PoppinsFontFamily,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = ByaheroBlack
                                                                ),
                                                                cursorBrush = SolidColor(ByaheroDarkGreen),
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .onFocusChanged { fs ->
                                                                        if (fs.isFocused) {
                                                                            focusedStopField = FocusedStopField.PICKUP
                                                                            pickupInputQuery = ""
                                                                        }
                                                                    }
                                                            )
                                                        }
                                                    }
                                                    if (focusedStopField == FocusedStopField.PICKUP && pickupInputQuery.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = { pickupInputQuery = "" },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Clear pickup",
                                                                tint = ByaheroMuted,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    } else {
                                                        IconButton(
                                                            onClick = {
                                                                focusedStopField = FocusedStopField.NONE
                                                                val prevLatLng = pickupLatLng
                                                                val prevLabel = pickupLabel
                                                                pickupLatLng = userLocation
                                                                pickupLabel = "Locating current address..."
                                                                scope.launch {
                                                                    val (resolved, _) = PlaceSearchHelper.reverseGeocode(context, userLocation)
                                                                    if (!trySetPickup(userLocation, resolved)) {
                                                                        pickupLatLng = prevLatLng
                                                                        pickupLabel = prevLabel
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.size(30.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.MyLocation,
                                                                contentDescription = "Use GPS",
                                                                tint = ByaheroDarkGreen,
                                                                modifier = Modifier.size(17.dp)
                                                            )
                                                        }
                                                    }
                                                    SmallChipButton("Map") {
                                                        focusedStopField = FocusedStopField.NONE
                                                        mapPickMode = MapPickMode.PICKUP
                                                        fullMapExpanded = true
                                                    }
                                                }
                                            }

                                            // Destination Input Card
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = ByaheroWhite,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (focusedStopField == FocusedStopField.DESTINATION) ByaheroDarkGreen else ByaheroBorder
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(ByaheroDarkGreen)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Destination",
                                                            fontFamily = PoppinsFontFamily,
                                                            fontSize = 10.sp,
                                                            color = ByaheroMuted
                                                        )
                                                        Box {
                                                            val showingQuery = focusedStopField == FocusedStopField.DESTINATION
                                                            val valText = if (showingQuery) destInputQuery else destLabel
                                                            if (valText.isEmpty()) {
                                                                Text(
                                                                    text = if (showingQuery && destLabel.isNotBlank()) destLabel else "Where are you heading?",
                                                                    fontFamily = PoppinsFontFamily,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = ByaheroMuted,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                            BasicTextField(
                                                                value = valText,
                                                                onValueChange = {
                                                                    focusedStopField = FocusedStopField.DESTINATION
                                                                    destInputQuery = it
                                                                },
                                                                singleLine = true,
                                                                textStyle = TextStyle(
                                                                    fontFamily = PoppinsFontFamily,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = ByaheroBlack
                                                                ),
                                                                cursorBrush = SolidColor(ByaheroDarkGreen),
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .onFocusChanged { fs ->
                                                                        if (fs.isFocused) {
                                                                            focusedStopField = FocusedStopField.DESTINATION
                                                                            destInputQuery = ""
                                                                        }
                                                                    }
                                                            )
                                                        }
                                                    }
                                                    if (focusedStopField == FocusedStopField.DESTINATION && destInputQuery.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = { destInputQuery = "" },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Clear destination",
                                                                tint = ByaheroMuted,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                    SmallChipButton("Map") {
                                                        focusedStopField = FocusedStopField.NONE
                                                        mapPickMode = MapPickMode.DESTINATION
                                                        fullMapExpanded = true
                                                    }
                                                }
                                            }
                                        }

                                        // Floating Circular Swap Button matching Web Screenshot
                                        if (destLatLng != null) {
                                            Surface(
                                                onClick = {
                                                    userEditedPickup = true
                                                    val tmpLat = pickupLatLng
                                                    val tmpLabel = pickupLabel
                                                    pickupLatLng = destLatLng!!
                                                    pickupLabel = destLabel
                                                    destLatLng = tmpLat
                                                    destLabel = tmpLabel
                                                    focusedStopField = FocusedStopField.NONE
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .offset(x = (-54).dp)
                                                    .size(32.dp),
                                                shape = CircleShape,
                                                color = ByaheroWhite,
                                                border = BorderStroke(1.dp, ByaheroBorder),
                                                shadowElevation = 4.dp
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.SwapVert,
                                                        contentDescription = "Swap Pickup & Destination",
                                                        tint = ByaheroDarkGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Inline Search Suggestions Dropdown when typing inside Pickup or Destination
                                    if (focusedStopField != FocusedStopField.NONE && (inlineStopResults.isNotEmpty() || isSearchingStop)) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = ByaheroWhite,
                                            border = BorderStroke(1.dp, ByaheroBorder),
                                            shadowElevation = 6.dp
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                if (isSearchingStop && inlineStopResults.isEmpty()) {
                                                    Text(
                                                        text = "Searching places...",
                                                        fontFamily = PoppinsFontFamily,
                                                        fontSize = 12.sp,
                                                        color = ByaheroMuted,
                                                        modifier = Modifier.padding(12.dp)
                                                    )
                                                }
                                                inlineStopResults.take(5).forEach { spot ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                val ok = if (focusedStopField == FocusedStopField.PICKUP) {
                                                                    trySetPickup(LatLng(spot.lat, spot.lng), spot.name).also {
                                                                        if (it) pickupInputQuery = ""
                                                                    }
                                                                } else {
                                                                    trySetDestination(LatLng(spot.lat, spot.lng), spot.name).also {
                                                                        if (it) destInputQuery = ""
                                                                    }
                                                                }
                                                                if (ok) {
                                                                    focusTarget = LatLng(spot.lat, spot.lng)
                                                                    focusSeq++
                                                                }
                                                                focusedStopField = FocusedStopField.NONE
                                                                inlineStopResults = emptyList()
                                                            }
                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOn,
                                                            contentDescription = null,
                                                            tint = ByaheroDarkGreen,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = spot.name,
                                                                fontFamily = PoppinsFontFamily,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ByaheroBlack,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = spot.address.ifBlank { String.format("%.1f km away", spot.distanceKm) },
                                                                fontFamily = PoppinsFontFamily,
                                                                fontSize = 10.sp,
                                                                color = ByaheroMuted,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    HorizontalDivider(color = ByaheroBorder)
                                                }
                                            }
                                        }
                                    }

                                    val stopsAreIdentical = destLatLng?.let { d ->
                                        isSameStop(
                                            pickupLatLng.latitude,
                                            pickupLatLng.longitude,
                                            pickupLabel,
                                            d.latitude,
                                            d.longitude,
                                            destLabel
                                        )
                                    } ?: false

                                    if (locationError != null || stopsAreIdentical) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFFEF2F2),
                                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = Color(0xFFB91C1C),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = locationError ?: "Pickup and destination cannot be the same location.",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFB91C1C)
                                                )
                                            }
                                        }
                                    }

                                    // Vector Motorcycle & Taxi Selection Cards matching Web Preview
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        VehicleChoiceCard(
                                            modifier = Modifier.weight(1f),
                                            title = "BYAHERO Moto",
                                            subtitle = "Fast traffic-beat ride",
                                            baseLabel = "Base P40",
                                            drawableRes = if (selectedVehicle == VehicleType.MOTORCYCLE) R.drawable.ic_byahero_moto else R.drawable.ic_moto_dark,
                                            selected = selectedVehicle == VehicleType.MOTORCYCLE,
                                            onClick = { selectedVehicle = VehicleType.MOTORCYCLE }
                                        )
                                        VehicleChoiceCard(
                                            modifier = Modifier.weight(1f),
                                            title = "BYAHERO Cab",
                                            subtitle = "Air-conditioned 4-seater",
                                            baseLabel = "Base P60",
                                            drawableRes = if (selectedVehicle == VehicleType.TAXI) R.drawable.ic_taxi_light else R.drawable.ic_taxi_dark,
                                            selected = selectedVehicle == VehicleType.TAXI,
                                            onClick = { selectedVehicle = VehicleType.TAXI }
                                        )
                                    }

                                    val stopsMatchNow = destLatLng?.let { d ->
                                        isSameStop(
                                            pickupLatLng.latitude,
                                            pickupLatLng.longitude,
                                            pickupLabel,
                                            d.latitude,
                                            d.longitude,
                                            destLabel
                                        )
                                    } ?: false

                                    if (fareQuote != null && !stopsMatchNow) {
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
                                                        text = String.format("%.2f km · %d min ETA", routeDistanceKm, durationMins),
                                                        fontFamily = PoppinsFontFamily,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ByaheroBlack
                                                    )
                                                    Text(
                                                        text = String.format("+P%.2f rider bonus", fareQuote.riderIncentive),
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
                                                            text = String.format("-10%% Sat (Saved P%.2f)", fareQuote.passengerDiscount),
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
                                                val d = destLatLng ?: return@Button
                                                if (isSameStop(pickupLatLng.latitude, pickupLatLng.longitude, pickupLabel, d.latitude, d.longitude, destLabel)) {
                                                    locationError = "Pickup and destination cannot be the same location."
                                                    return@Button
                                                }
                                                activeRide = ActiveRide(
                                                    id = "ride_${System.currentTimeMillis()}",
                                                    passengerName = profile.fullName,
                                                    vehicleType = selectedVehicle,
                                                    pickupLabel = pickupLabel,
                                                    pickupLat = pickupLatLng.latitude,
                                                    pickupLng = pickupLatLng.longitude,
                                                    destinationLabel = destLabel,
                                                    destLat = d.latitude,
                                                    destLng = d.longitude,
                                                    distanceKm = routeDistanceKm,
                                                    durationMins = durationMins,
                                                    fare = fareQuote,
                                                    status = RideStatus.SEARCHING,
                                                    createdAtLabel = OrderHistoryStore.currentTimeLabel(),
                                                    roleContext = "passenger"
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
                                                text = String.format(
                                                    "Book %s · P%.2f",
                                                    if (selectedVehicle == VehicleType.MOTORCYCLE) "Moto" else "Taxi",
                                                    fareQuote.finalFare
                                                ),
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
                                        runExploreCategory(
                                            "fast_food",
                                            "Jollibee McDonalds KFC Burger King",
                                            "Nearby Fast Food",
                                            openExplore = false
                                        )
                                    }
                                    ExploreCategoryButton(
                                        modifier = Modifier.weight(1f),
                                        title = "Resorts & Beaches",
                                        subtitle = "Coastal & pool spots",
                                        icon = Icons.Default.BeachAccess
                                    ) {
                                        runExploreCategory(
                                            "resort_beach",
                                            "beach resort swimming resort",
                                            "Nearest Resorts & Beaches",
                                            openExplore = false
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
                                            text = if (isSearchingExplore) "Searching..." else "${discoveredSpots.size} found",
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 11.sp,
                                            color = Color(0xFFD1FAE5)
                                        )
                                    }
                                }

                                if (isSearchingExplore) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = ByaheroDarkGreen)
                                    }
                                } else if (exploreError != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFFFBEB),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                    ) {
                                        Text(
                                            text = exploreError!!,
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
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
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
                                                    text = "${spot.address.ifBlank { "Location on map" }} · ${String.format("%.2f km", spot.distanceKm)}",
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 11.sp,
                                                    color = ByaheroMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    SmallChipButton("Map") {
                                                        val spotPt = LatLng(spot.lat, spot.lng)
                                                        cameraState.position = CameraPosition.fromLatLngZoom(
                                                            spotPt,
                                                            15f
                                                        )
                                                        focusTarget = spotPt
                                                        focusSeq++
                                                        selectedMapSpot = spot
                                                        fullMapExpanded = true
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            trySetPickup(LatLng(spot.lat, spot.lng), spot.name)
                                                            selectedTab = 0
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(36.dp),
                                                        shape = RoundedCornerShape(10.dp),
                                                        border = BorderStroke(1.dp, ByaheroDarkGreen)
                                                    ) {
                                                        Text(
                                                            text = "Set Pickup",
                                                            fontFamily = PoppinsFontFamily,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ByaheroDarkGreen
                                                        )
                                                    }
                                                    Button(
                                                        onClick = {
                                                            trySetDestination(LatLng(spot.lat, spot.lng), spot.name)
                                                            selectedTab = 0
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(36.dp),
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
                            }

                            2 -> {
                                val completedOrders = orderHistory.filter { it.status == RideStatus.COMPLETED }
                                val totalPassengerSpend = completedOrders
                                    .filter { it.roleContext != "rider" }
                                    .sumOf { it.fare.finalFare }
                                val filteredHistory = when (historyFilter) {
                                    "completed" -> orderHistory.filter { it.status == RideStatus.COMPLETED }
                                    "cancelled" -> orderHistory.filter { it.status == RideStatus.CANCELLED }
                                    else -> orderHistory
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = ByaheroDarkGreen
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Completed Orders",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 11.sp,
                                                color = Color(0xFFD1FAE5)
                                            )
                                            Text(
                                                text = "${completedOrders.size}",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ByaheroWhite
                                            )
                                            Text(
                                                text = "${orderHistory.size} total logged",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 10.sp,
                                                color = Color(0xFFA7F3D0)
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = ByaheroWhite,
                                        border = BorderStroke(1.dp, ByaheroBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Ride Spend",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 11.sp,
                                                color = ByaheroMuted
                                            )
                                            Text(
                                                text = String.format("P%.2f", totalPassengerSpend),
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ByaheroBlack
                                            )
                                            Text(
                                                text = "Passenger completed trips",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 10.sp,
                                                color = ByaheroDarkGreen,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ByaheroSubtleGlass,
                                    border = BorderStroke(1.dp, ByaheroBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            "all" to "All",
                                            "completed" to "Completed",
                                            "cancelled" to "Cancelled"
                                        ).forEach { (key, title) ->
                                            val active = historyFilter == key
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (active) ByaheroDarkGreen else Color.Transparent)
                                                    .clickable { historyFilter = key },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = title,
                                                    fontFamily = PoppinsFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (active) ByaheroWhite else ByaheroMuted
                                                )
                                            }
                                        }
                                    }
                                }

                                if (filteredHistory.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = ByaheroWhite,
                                        border = BorderStroke(1.dp, ByaheroBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "No orders in this view",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ByaheroBlack
                                            )
                                            Text(
                                                text = "Completed and cancelled rides appear here automatically.",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 11.sp,
                                                color = ByaheroMuted
                                            )
                                        }
                                    }
                                } else {
                                    filteredHistory.forEach { item ->
                                        val isCompleted = item.status == RideStatus.COMPLETED
                                        val isRiderTrip = item.roleContext == "rider"
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = ByaheroWhite,
                                            border = BorderStroke(1.dp, ByaheroBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(13.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Image(
                                                            painter = painterResource(
                                                                id = if (item.vehicleType == VehicleType.MOTORCYCLE) {
                                                                    R.drawable.ic_moto_dark
                                                                } else {
                                                                    R.drawable.ic_taxi_dark
                                                                }
                                                            ),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(width = 36.dp, height = 22.dp)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = (if (item.vehicleType == VehicleType.MOTORCYCLE) "BYAHERO Moto" else "BYAHERO Cab") +
                                                                    (if (isRiderTrip) " · Rider Trip" else ""),
                                                                fontFamily = PoppinsFontFamily,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ByaheroBlack
                                                            )
                                                            Text(
                                                                text = item.createdAtLabel,
                                                                fontFamily = PoppinsFontFamily,
                                                                fontSize = 10.sp,
                                                                color = ByaheroMuted
                                                            )
                                                        }
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = if (isRiderTrip) {
                                                                String.format("+P%.2f", item.fare.finalFare + item.fare.riderIncentive)
                                                            } else {
                                                                String.format("P%.2f", item.fare.finalFare)
                                                            },
                                                            fontFamily = PoppinsFontFamily,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = ByaheroBlack
                                                        )
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                                contentDescription = null,
                                                                tint = if (isCompleted) ByaheroDarkGreen else Color(0xFFB91C1C),
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Text(
                                                                text = if (isCompleted) "Completed" else "Cancelled",
                                                                fontFamily = PoppinsFontFamily,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isCompleted) ByaheroDarkGreen else Color(0xFFB91C1C)
                                                            )
                                                        }
                                                    }
                                                }

                                                HorizontalDivider(color = ByaheroBorder)

                                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    Text(
                                                        text = "From: ${item.pickupLabel}",
                                                        fontFamily = PoppinsFontFamily,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ByaheroBlack,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "To: ${item.destinationLabel}",
                                                        fontFamily = PoppinsFontFamily,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ByaheroDarkGreen,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = String.format(
                                                            "%.1f km · %d min%s",
                                                            item.distanceKm,
                                                            item.durationMins,
                                                            item.riderName?.let { " · $it" } ?: ""
                                                        ),
                                                        fontFamily = PoppinsFontFamily,
                                                        fontSize = 10.sp,
                                                        color = ByaheroMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (!isRiderTrip) {
                                                        Surface(
                                                            onClick = {
                                                                if (isSameStop(
                                                                        item.pickupLat,
                                                                        item.pickupLng,
                                                                        item.pickupLabel,
                                                                        item.destLat,
                                                                        item.destLng,
                                                                        item.destinationLabel
                                                                    )
                                                                ) {
                                                                    locationError = "Pickup and destination cannot be the same location."
                                                                } else {
                                                                    userEditedPickup = true
                                                                    pickupLatLng = LatLng(item.pickupLat, item.pickupLng)
                                                                    pickupLabel = item.pickupLabel
                                                                    destLatLng = LatLng(item.destLat, item.destLng)
                                                                    destLabel = item.destinationLabel
                                                                    selectedVehicle = item.vehicleType
                                                                    activeRide = null
                                                                    locationError = null
                                                                    selectedTab = 0
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = ByaheroSubtleGlass
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Refresh,
                                                                    contentDescription = null,
                                                                    tint = ByaheroDarkGreen,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Text(
                                                                    text = "Book Again",
                                                                    fontFamily = PoppinsFontFamily,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = ByaheroDarkGreen
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
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
                                                        recordOrderInHistory(
                                                            job.copy(
                                                                status = RideStatus.COMPLETED,
                                                                createdAtLabel = OrderHistoryStore.currentTimeLabel(),
                                                                roleContext = "rider"
                                                            )
                                                        )
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

                            4 -> {
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
                                                                selectedTab = if (modeItem == ActiveMode.RIDER) 3 else 0
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
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontSize = 11.sp,
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
    baseLabel: String,
    drawableRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) ByaheroDarkGreen else ByaheroWhite,
        border = BorderStroke(1.dp, if (selected) ByaheroDarkGreen else ByaheroBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = title,
                    modifier = Modifier.size(width = 62.dp, height = 34.dp)
                )
                Text(
                    text = baseLabel,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color(0xFFA7F3D0) else ByaheroDarkGreen
                )
            }
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
        RideStatus.CANCELLED -> "Trip Cancelled"
    }
    val actionLabel = when (ride.status) {
        RideStatus.SEARCHING -> "Match Rider Now"
        RideStatus.ACCEPTED -> "Simulate Arrival"
        RideStatus.ARRIVING -> "Start Trip"
        RideStatus.IN_PROGRESS -> "Complete Ride"
        RideStatus.COMPLETED -> "Book Another Ride"
        RideStatus.CANCELLED -> "Book Another Ride"
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
