package com.hddev.byahero.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.hddev.byahero.data.NearbySpot
import com.hddev.byahero.ui.theme.ByaheroBlack
import com.hddev.byahero.ui.theme.ByaheroBorder
import com.hddev.byahero.ui.theme.ByaheroDarkGreen
import com.hddev.byahero.ui.theme.ByaheroGlassWhite
import com.hddev.byahero.ui.theme.ByaheroWhite
import com.hddev.byahero.ui.theme.PoppinsFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh

private object MapTileCache {
    private val cache = LruCache<String, ImageBitmap>(180)
    private val inFlight = ConcurrentHashMap<String, Boolean>()

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun tryMarkInFlight(key: String): Boolean = inFlight.putIfAbsent(key, true) == null

    fun put(key: String, bmp: ImageBitmap) {
        cache.put(key, bmp)
        inFlight.remove(key)
    }

    fun clearInFlight(key: String) {
        inFlight.remove(key)
    }
}

private suspend fun fetchTileBitmap(z: Int, x: Int, y: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    val urls = listOf(
        "https://tile.openstreetmap.org/$z/$x/$y.png",
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/$z/$y/$x"
    )
    for (urlStr in urls) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) BYAHERO-Navigation/1.0")
                setRequestProperty("Accept", "image/png,image/*")
            }
            if (conn.responseCode == 200) {
                val decoded = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                if (decoded != null) {
                    return@withContext decoded.asImageBitmap()
                }
            }
        } catch (_: Exception) {
        } finally {
            conn?.disconnect()
        }
    }
    null
}

private fun lngToWorldX(lng: Double, scale: Double): Double =
    ((lng + 180.0) / 360.0) * scale

private fun latToWorldY(lat: Double, scale: Double): Double {
    val clamped = lat.coerceIn(-85.0511, 85.0511)
    val s = sin(Math.toRadians(clamped))
    val yNorm = 0.5 - ln((1.0 + s) / (1.0 - s)) / (4.0 * PI)
    return yNorm * scale
}

private fun worldToLatLng(worldX: Double, worldY: Double, scale: Double): LatLng {
    val lng = ((worldX / scale) * 360.0 - 180.0).coerceIn(-180.0, 180.0)
    val n = PI - 2.0 * PI * (worldY / scale)
    val lat = Math.toDegrees(atan(sinh(n))).coerceIn(-85.0511, 85.0511)
    return LatLng(lat, lng)
}

@Composable
fun NativeInteractiveMap(
    modifier: Modifier = Modifier,
    userLocation: LatLng,
    pickupLocation: LatLng,
    destinationLocation: LatLng?,
    discoveredSpots: List<NearbySpot>,
    recenterSeq: Int = 0,
    focusTarget: LatLng? = null,
    focusSeq: Int = 0,
    onSpotClick: ((NearbySpot) -> Unit)? = null,
    onMapClick: (LatLng) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val tilePx = with(density) { 240.dp.toPx().toDouble() }

    var viewSize by remember { mutableStateOf(IntSize(1, 1)) }
    var zoom by remember { mutableIntStateOf(15) }
    var centerLat by remember { mutableDoubleStateOf(userLocation.latitude) }
    var centerLng by remember { mutableDoubleStateOf(userLocation.longitude) }
    var tileRevision by remember { mutableIntStateOf(0) }

    LaunchedEffect(recenterSeq) {
        if (recenterSeq > 0) {
            centerLat = userLocation.latitude
            centerLng = userLocation.longitude
            zoom = 15
        }
    }

    LaunchedEffect(focusSeq) {
        if (focusSeq > 0 && focusTarget != null) {
            centerLat = focusTarget.latitude
            centerLng = focusTarget.longitude
            zoom = 15
        }
    }

    LaunchedEffect(userLocation, destinationLocation) {
        if (destinationLocation != null) {
            centerLat = (pickupLocation.latitude + destinationLocation.latitude) / 2.0
            centerLng = (pickupLocation.longitude + destinationLocation.longitude) / 2.0
            zoom = 13
        } else {
            centerLat = userLocation.latitude
            centerLng = userLocation.longitude
        }
    }

    LaunchedEffect(discoveredSpots) {
        if (discoveredSpots.isNotEmpty() && destinationLocation == null) {
            centerLat = discoveredSpots.first().lat
            centerLng = discoveredSpots.first().lng
            zoom = 14
        }
    }

    val scale = tilePx * 2.0.pow(zoom)
    val centerWorldX = lngToWorldX(centerLng, scale)
    val centerWorldY = latToWorldY(centerLat, scale)
    val halfW = viewSize.width / 2.0
    val halfH = viewSize.height / 2.0
    val maxTileIdx = 1 shl zoom

    val minTx = floor((centerWorldX - halfW) / tilePx).toInt()
    val maxTx = floor((centerWorldX + halfW) / tilePx).toInt()
    val minTy = max(0, floor((centerWorldY - halfH) / tilePx).toInt())
    val maxTy = min(maxTileIdx - 1, floor((centerWorldY + halfH) / tilePx).toInt())

    LaunchedEffect(zoom, minTx, maxTx, minTy, maxTy, viewSize) {
        if (viewSize.width <= 1 || viewSize.height <= 1) return@LaunchedEffect
        for (ty in minTy..maxTy) {
            for (txRaw in minTx..maxTx) {
                val tx = ((txRaw % maxTileIdx) + maxTileIdx) % maxTileIdx
                val key = "osm_v2_$zoom/$tx/$ty"
                if (MapTileCache.get(key) == null && MapTileCache.tryMarkInFlight(key)) {
                    scope.launch {
                        val bmp = fetchTileBitmap(zoom, tx, ty)
                        if (bmp != null) {
                            MapTileCache.put(key, bmp)
                            tileRevision++
                        } else {
                            MapTileCache.clearInFlight(key)
                        }
                    }
                }
            }
        }
    }

    fun latLngToScreen(pt: LatLng): Offset {
        val wx = lngToWorldX(pt.longitude, scale)
        val wy = latToWorldY(pt.latitude, scale)
        return Offset(
            x = (wx - centerWorldX + halfW).toFloat(),
            y = (wy - centerWorldY + halfH).toFloat()
        )
    }

    Box(
        modifier = modifier
            .background(Color(0xFFE7EFEA))
            .onSizeChanged { viewSize = it }
            .pointerInput(zoom, scale, halfW, halfH) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val currentWorldX = lngToWorldX(centerLng, scale) - dragAmount.x
                    val currentWorldY = latToWorldY(centerLat, scale) - dragAmount.y
                    val nextCenter = worldToLatLng(currentWorldX, currentWorldY, scale)
                    centerLat = nextCenter.latitude
                    centerLng = nextCenter.longitude
                }
            }
            .pointerInput(zoom, scale, halfW, halfH) {
                detectTapGestures { offset ->
                    val cwx = lngToWorldX(centerLng, scale)
                    val cwy = latToWorldY(centerLat, scale)
                    val wx = cwx + (offset.x - halfW)
                    val wy = cwy + (offset.y - halfH)
                    onMapClick(worldToLatLng(wx, wy, scale))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rev = tileRevision
            @Suppress("UNUSED_EXPRESSION")
            rev

            val gridStep = 64.dp.toPx()
            val offsetX = ((-(centerWorldX % gridStep) + gridStep) % gridStep).toFloat()
            val offsetY = ((-(centerWorldY % gridStep) + gridStep) % gridStep).toFloat()
            var gx = offsetX
            while (gx < size.width) {
                drawLine(Color(0xFFD5E2DA), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                gx += gridStep
            }
            var gy = offsetY
            while (gy < size.height) {
                drawLine(Color(0xFFD5E2DA), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                gy += gridStep
            }

            for (ty in minTy..maxTy) {
                for (txRaw in minTx..maxTx) {
                    val tx = ((txRaw % maxTileIdx) + maxTileIdx) % maxTileIdx
                    val key = "osm_v2_$zoom/$tx/$ty"
                    val bmp = MapTileCache.get(key) ?: continue
                    val left = (txRaw * tilePx - centerWorldX + halfW).roundToInt()
                    val top = (ty * tilePx - centerWorldY + halfH).roundToInt()
                    val right = ((txRaw + 1) * tilePx - centerWorldX + halfW).roundToInt()
                    val bottom = ((ty + 1) * tilePx - centerWorldY + halfH).roundToInt()
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(left, top),
                        dstSize = IntSize(max(1, right - left), max(1, bottom - top))
                    )
                }
            }

            if (destinationLocation != null) {
                val p1 = latLngToScreen(pickupLocation)
                val p2 = latLngToScreen(destinationLocation)
                drawLine(
                    color = ByaheroWhite,
                    start = p1,
                    end = p2,
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = ByaheroDarkGreen,
                    start = p1,
                    end = p2,
                    strokeWidth = 8f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 12f), 0f)
                )
            }

            val gpsPos = latLngToScreen(userLocation)
            drawCircle(
                color = ByaheroDarkGreen.copy(alpha = 0.18f),
                radius = 40f,
                center = gpsPos
            )
            drawCircle(
                color = ByaheroWhite,
                radius = 16f,
                center = gpsPos
            )
            drawCircle(
                color = ByaheroDarkGreen,
                radius = 11f,
                center = gpsPos
            )
        }

        discoveredSpots.take(8).forEach { spot ->
            val pos = latLngToScreen(LatLng(spot.lat, spot.lng))
            if (pos.x > -80f && pos.x < viewSize.width + 80f && pos.y > -40f && pos.y < viewSize.height + 40f) {
                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (pos.x - 42.dp.toPx()).roundToInt(),
                                y = (pos.y - 30.dp.toPx()).roundToInt()
                            )
                        }
                        .clickable {
                            if (onSpotClick != null) {
                                onSpotClick(spot)
                            } else {
                                onMapClick(LatLng(spot.lat, spot.lng))
                            }
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = ByaheroWhite,
                    border = BorderStroke(1.5.dp, ByaheroDarkGreen),
                    shadowElevation = 5.dp
                ) {
                    Text(
                        text = spot.name,
                        fontFamily = PoppinsFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ByaheroDarkGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        val pickupScreen = latLngToScreen(pickupLocation)
        Surface(
            modifier = Modifier.offset {
                IntOffset(
                    x = (pickupScreen.x - 28.dp.toPx()).roundToInt(),
                    y = (pickupScreen.y - 34.dp.toPx()).roundToInt()
                )
            },
            shape = RoundedCornerShape(8.dp),
            color = ByaheroWhite,
            border = BorderStroke(2.dp, ByaheroDarkGreen),
            shadowElevation = 6.dp
        ) {
            Text(
                text = "Pickup",
                fontFamily = PoppinsFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ByaheroBlack,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        if (destinationLocation != null) {
            val destScreen = latLngToScreen(destinationLocation)
            Surface(
                modifier = Modifier.offset {
                    IntOffset(
                        x = (destScreen.x - 32.dp.toPx()).roundToInt(),
                        y = (destScreen.y - 34.dp.toPx()).roundToInt()
                    )
                },
                shape = RoundedCornerShape(8.dp),
                color = ByaheroDarkGreen,
                border = BorderStroke(2.dp, ByaheroWhite),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = "Drop-off",
                    fontFamily = PoppinsFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ByaheroWhite,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = { if (zoom < 18) zoom++ },
                shape = CircleShape,
                color = ByaheroGlassWhite,
                border = BorderStroke(1.dp, ByaheroBorder),
                shadowElevation = 4.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom in",
                        tint = ByaheroBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Surface(
                onClick = { if (zoom > 3) zoom-- },
                shape = CircleShape,
                color = ByaheroGlassWhite,
                border = BorderStroke(1.dp, ByaheroBorder),
                shadowElevation = 4.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom out",
                        tint = ByaheroBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
