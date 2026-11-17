package com.hddev.byahero.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.model.LatLng
import com.hddev.byahero.data.NearbySpot
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveOsmMap(
    modifier: Modifier = Modifier,
    userLocation: LatLng,
    pickupLocation: LatLng,
    destinationLocation: LatLng?,
    discoveredSpots: List<NearbySpot>,
    onMapClick: (LatLng) -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageLoaded by remember { mutableStateOf(false) }

    val initialHtml = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #E9EFEA; font-family: sans-serif; }
            .pin-label {
              background: #0F522B;
              color: #FFFFFF;
              border: 2px solid #FFFFFF;
              border-radius: 10px;
              padding: 3px 8px;
              font-size: 11px;
              font-weight: 700;
              white-space: nowrap;
              box-shadow: 0 3px 10px rgba(0,0,0,0.25);
            }
            .pin-pickup {
              background: #FFFFFF;
              color: #101713;
              border: 2px solid #0F522B;
            }
            .pin-spot {
              background: #FFFFFF;
              color: #0F522B;
              border: 1.5px solid #0F522B;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = L.map('map', { zoomControl: false }).setView([${userLocation.latitude}, ${userLocation.longitude}], 15);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 19,
              attribution: 'OpenStreetMap'
            }).addTo(map);

            var layerGroup = L.layerGroup().addTo(map);

            map.on('click', function(e) {
              if (window.AndroidMapBridge) {
                window.AndroidMapBridge.onMapTapped(e.latlng.lat, e.latlng.lng);
              }
            });

            window.updateMapState = function(payloadJson) {
              var data = JSON.parse(payloadJson);
              layerGroup.clearLayers();

              L.circleMarker([data.userLat, data.userLng], {
                radius: 8,
                color: '#FFFFFF',
                weight: 3,
                fillColor: '#0F522B',
                fillOpacity: 1
              }).addTo(layerGroup);

              L.marker([data.pickupLat, data.pickupLng], {
                icon: L.divIcon({
                  className: '',
                  html: '<div class="pin-label pin-pickup">Pickup</div>'
                })
              }).addTo(layerGroup);

              var bounds = [[data.pickupLat, data.pickupLng]];

              if (data.hasDest) {
                L.marker([data.destLat, data.destLng], {
                  icon: L.divIcon({
                    className: '',
                    html: '<div class="pin-label">Drop-off</div>'
                  })
                }).addTo(layerGroup);
                L.polyline([
                  [data.pickupLat, data.pickupLng],
                  [data.destLat, data.destLng]
                ], { color: '#0F522B', weight: 5, opacity: 0.9 }).addTo(layerGroup);
                bounds.push([data.destLat, data.destLng]);
              }

              if (data.spots && data.spots.length > 0) {
                data.spots.forEach(function(s) {
                  var m = L.marker([s.lat, s.lng], {
                    icon: L.divIcon({
                      className: '',
                      html: '<div class="pin-label pin-spot">' + s.name.replace(/</g, '') + '</div>'
                    })
                  }).addTo(layerGroup);
                  m.on('click', function() {
                    if (window.AndroidMapBridge) {
                      window.AndroidMapBridge.onMapTapped(s.lat, s.lng);
                    }
                  });
                  bounds.push([s.lat, s.lng]);
                });
              }

              if (bounds.length > 1) {
                map.fitBounds(bounds, { padding: [70, 50], maxZoom: 16 });
              } else {
                map.panTo([data.userLat, data.userLng]);
              }
            };
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(pageLoaded, userLocation, pickupLocation, destinationLocation, discoveredSpots) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!pageLoaded) return@LaunchedEffect
        val spotsArr = JSONArray()
        discoveredSpots.take(10).forEach { spot ->
            spotsArr.put(
                JSONObject().apply {
                    put("name", spot.name)
                    put("lat", spot.lat)
                    put("lng", spot.lng)
                }
            )
        }
        val payload = JSONObject().apply {
            put("userLat", userLocation.latitude)
            put("userLng", userLocation.longitude)
            put("pickupLat", pickupLocation.latitude)
            put("pickupLng", pickupLocation.longitude)
            put("hasDest", destinationLocation != null)
            put("destLat", destinationLocation?.latitude ?: 0.0)
            put("destLng", destinationLocation?.longitude ?: 0.0)
            put("spots", spotsArr)
        }.toString()

        val escaped = JSONObject.quote(payload)
        wv.evaluateJavascript("window.updateMapState($escaped);", null)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 BYAHERO/1.0"
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onMapTapped(lat: Double, lng: Double) {
                            post { onMapClick(LatLng(lat, lng)) }
                        }
                    },
                    "AndroidMapBridge"
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageLoaded = true
                    }
                }
                loadDataWithBaseURL("https://byahero.local/", initialHtml, "text/html", "UTF-8", null)
                webViewRef = this
            }
        }
    )
}
