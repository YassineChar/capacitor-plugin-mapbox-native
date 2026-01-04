package com.whisperspots.plugins

import android.annotation.SuppressLint
import android.graphics.*
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.getcapacitor.*
import com.getcapacitor.annotation.CapacitorPlugin
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import kotlin.math.pow

@CapacitorPlugin(name = "WhisperSpotsMapboxNative")
class MapboxNativePlugin : Plugin() {
    
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var circleAnnotationManager: CircleAnnotationManager? = null
    private var statusBarGradientView: View? = null
    
    private val whisperAnnotations = mutableMapOf<String, PointAnnotation>()
    private val clusterAnnotations = mutableListOf<ClusterAnnotationData>()
    private val cachedWhisperIds = mutableSetOf<String>()
    
    private var mockUserAnnotation: PointAnnotation? = null
    private var isMockModeActive = false
    
    private var currentUserRadius: Double? = null
    private var autoSyncEnabled = false
    
    private var mapTopOffset: Float = 0f
    private var mapHeightOffset: Float = 0f
    
    private var moreWhispersTranslation: String = "more"
    private var clusteringThreshold: Double = 50.0
    private var lastClusteredZoomLevel: Double = 0.0
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    @PluginMethod
    fun echo(call: PluginCall) {
        val value = call.getString("value") ?: ""
        call.resolve(JSObject().put("value", value))
    }
    
    @PluginMethod
    fun initMapbox(call: PluginCall) {
        android.util.Log.i("MapboxNativePlugin", "🗺️ initMapbox() CALLED - topOffset: ${call.getDouble("topOffset")}, heightOffset: ${call.getDouble("heightOffset")}")
        bridge.activity.runOnUiThread {
            if (mapView != null) {
                android.util.Log.w("MapboxNativePlugin", "⚠️ Map already initialized, returning success")
                call.resolve(JSObject().put("status", "success"))
                return@runOnUiThread
            }
            
            mapTopOffset = (call.getDouble("topOffset") ?: 0.0).toFloat()
            mapHeightOffset = (call.getDouble("heightOffset") ?: 0.0).toFloat()
            
            android.util.Log.i("MapboxNativePlugin", "📐 Using offsets - top: $mapTopOffset, height: $mapHeightOffset")
            
            try {
                val context = bridge.context
                val activity = bridge.activity
                
                android.util.Log.i("MapboxNativePlugin", "🏗️ Creating MapView...")
                
                val styleUri = call.getString("styleUri") ?: run {
                    val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    when (nightModeFlags) {
                        android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                            android.util.Log.i("MapboxNativePlugin", "🌙 Dark mode detected - using night style")
                            "mapbox://styles/zpalah/cmfwv2ibl000701r3398e20ps"
                        }
                        android.content.res.Configuration.UI_MODE_NIGHT_NO -> {
                            android.util.Log.i("MapboxNativePlugin", "☀️ Light mode detected - using day style")
                            "mapbox://styles/zpalah/cmfwvtnd200dx01sbb5y043o7"
                        }
                        else -> {
                            android.util.Log.i("MapboxNativePlugin", "🌓 Default mode - using night style")
                            "mapbox://styles/zpalah/cmfwv2ibl000701r3398e20ps"
                        }
                    }
                }
                
                val mapView = MapView(context, MapInitOptions(
                    context = context,
                    styleUri = styleUri
                ))
                
                this.mapView = mapView
                
                val rootView = activity.window.decorView.findViewById<FrameLayout>(android.R.id.content)
                val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                
                mapView.layoutParams = layoutParams
                mapView.visibility = View.GONE
                
                android.util.Log.i("MapboxNativePlugin", "➕ Adding MapView to rootView (visibility: GONE)")
                
                rootView.addView(mapView, 0)
                
                android.util.Log.i("MapboxNativePlugin", "🎨 Creating status bar gradient overlay...")
                createStatusBarGradient(activity, rootView)
                
                val webView = bridge.webView
                webView?.setBackgroundColor(Color.TRANSPARENT)
                webView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                android.util.Log.i("MapboxNativePlugin", "🎨 WebView set to TRANSPARENT")
                
                this.mapboxMap = mapView.mapboxMap
                
                mapboxMap?.loadStyle(styleUri) { style ->
                    val annotationApi = mapView.annotations
                    pointAnnotationManager = annotationApi.createPointAnnotationManager()
                    circleAnnotationManager = annotationApi.createCircleAnnotationManager()
                    
                    mapView.location.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }
                    
                    val centerLat = call.getDouble("centerLat") ?: 0.0
                    val centerLon = call.getDouble("centerLon") ?: 0.0
                    val zoom = call.getDouble("zoom") ?: 14.0
                    
                    if (centerLat != 0.0 && centerLon != 0.0) {
                        val cameraOptions = CameraOptions.Builder()
                            .center(Point.fromLngLat(centerLon, centerLat))
                            .zoom(zoom)
                            .build()
                        mapboxMap?.setCamera(cameraOptions)
                        android.util.Log.i("MapboxNativePlugin", "📍 Centered on provided coords: $centerLat, $centerLon")
                    } else {
                        android.util.Log.w("MapboxNativePlugin", "⚠️ No coords provided, using Mapbox default")
                    }
                    
                    setupMapListeners()
                    
                    android.util.Log.i("MapboxNativePlugin", "✅ Style loaded, map fully ready")
                    
                    call.resolve(JSObject().put("status", "success"))
                }
                
            } catch (e: Exception) {
                call.reject("Failed to initialize map: ${e.message}", e)
            }
        }
    }
    
    @PluginMethod
    fun showMapbox(call: PluginCall) {
        android.util.Log.i("MapboxNativePlugin", "👁️ showMapbox() CALLED")
        bridge.activity.runOnUiThread {
            if (mapView == null) {
                android.util.Log.e("MapboxNativePlugin", "❌ Map is NULL, cannot show")
                call.reject("Map is not initialized. Please call initMapbox first.")
                return@runOnUiThread
            }
            
            android.util.Log.i("MapboxNativePlugin", "✅ Setting mapView visibility to VISIBLE")
            mapView?.visibility = View.VISIBLE
            
            android.util.Log.i("MapboxNativePlugin", "🌟 Showing status bar gradient")
            statusBarGradientView?.visibility = View.VISIBLE
            
            android.util.Log.i("MapboxNativePlugin", "🔼 Bringing MapView to front (z-index fix)")
            mapView?.bringToFront()
            
            bridge.webView?.invalidate()
            
            call.resolve(JSObject().put("status", "success"))
        }
    }
    
    @PluginMethod
    fun hideMapbox(call: PluginCall) {
        bridge.activity.runOnUiThread {
            if (mapView == null) {
                call.reject("Map is not initialized. Please call initMapbox first.")
                return@runOnUiThread
            }
            
            mapView?.visibility = View.GONE
            statusBarGradientView?.visibility = View.GONE
            call.resolve(JSObject().put("status", "success"))
        }
    }
    
    @PluginMethod
    fun closeMapbox(call: PluginCall) {
        bridge.activity.runOnUiThread {
            mapView?.onDestroy()
            
            val rootView = bridge.activity.window.decorView.findViewById<FrameLayout>(android.R.id.content)
            mapView?.let { rootView.removeView(it) }
            
            mapView = null
            mapboxMap = null
            pointAnnotationManager = null
            circleAnnotationManager = null
            
            coroutineScope.cancel()
            
            call.resolve(JSObject().put("status", "success"))
        }
    }
    
    @PluginMethod
    fun isMapboxVisible(call: PluginCall) {
        bridge.activity.runOnUiThread {
            val status = when {
                mapView == null -> 0
                mapView?.visibility == View.GONE -> 1
                else -> 2
            }
            call.resolve(JSObject().put("status", status))
        }
    }
    
    @PluginMethod
    fun setValuesMapbox(call: PluginCall) {
        bridge.activity.runOnUiThread {
            val dataPoints = call.getArray("dataPoints")
            
            if (dataPoints == null) {
                call.reject("DataPoints are required")
                return@runOnUiThread
            }
            
            if (mapView == null || pointAnnotationManager == null) {
                call.reject("Map is not initialized. Please call initMapbox first.")
                return@runOnUiThread
            }
            
            call.getString("moreWhispersTranslation")?.let {
                moreWhispersTranslation = it
            }
            
            call.getDouble("clusteringThreshold")?.let {
                clusteringThreshold = it
            }
            
            val incomingWhisperIds = mutableSetOf<String>()
            for (i in 0 until dataPoints.length()) {
                try {
                    val point = dataPoints.getJSONObject(i)
                    val whisperId = if (point.has("whisperId")) point.getString("whisperId") else null
                    whisperId?.let { incomingWhisperIds.add(it) }
                } catch (e: Exception) {
                    continue
                }
            }
            
            if (incomingWhisperIds == cachedWhisperIds && incomingWhisperIds.isNotEmpty()) {
                val incomingDataMap = mutableMapOf<String, org.json.JSONObject>()
                for (i in 0 until dataPoints.length()) {
                    try {
                        val point = dataPoints.getJSONObject(i)
                        val whisperId = if (point.has("whisperId")) point.getString("whisperId") else null
                        whisperId?.let { id ->
                            incomingDataMap[id] = point
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
                
                whisperAnnotations.forEach { (id, annotation) ->
                    incomingDataMap[id]?.let { data ->
                        val opacity = if (data.has("opacity")) data.getDouble("opacity").toFloat() else 1.0f
                        annotation.iconOpacity = opacity.toDouble()
                        pointAnnotationManager?.update(annotation)
                    }
                }
                
                call.resolve(JSObject().put("status", "cached"))
                return@runOnUiThread
            }
            
            cachedWhisperIds.clear()
            cachedWhisperIds.addAll(incomingWhisperIds)
            
            pointAnnotationManager?.deleteAll()
            whisperAnnotations.clear()
            clusterAnnotations.clear()
            
            val whisperAnnotationsList = mutableListOf<WhisperAnnotationData>()
            val existingIds = mutableSetOf<String>()
            
            for (i in 0 until dataPoints.length()) {
                val point = try {
                    dataPoints.getJSONObject(i)
                } catch (e: Exception) {
                    continue
                }
                
                if (!point.has("latitude") || !point.has("longitude") || !point.has("label") || !point.has("whisperId")) continue
                
                val lat = point.getDouble("latitude")
                val lon = point.getDouble("longitude")
                val label = point.getString("label")
                val whisperId = point.getString("whisperId")
                
                if (existingIds.contains(whisperId)) continue
                existingIds.add(whisperId)
                
                val annotationData = WhisperAnnotationData(
                    whisperId = whisperId,
                    latitude = lat,
                    longitude = lon,
                    label = label,
                    iconUrl = if (point.has("iconUrl")) point.getString("iconUrl") else null,
                    initials = if (point.has("initials")) point.getString("initials") else null,
                    avatarColor = if (point.has("avatarColor")) point.getString("avatarColor") else null,
                    expiryColor = if (point.has("expiryColor")) point.getString("expiryColor") else null,
                    markerSize = if (point.has("markerSize")) point.getDouble("markerSize").toFloat() else 60f,
                    opacity = if (point.has("opacity")) point.getDouble("opacity").toFloat() else 1.0f,
                    isClickable = if (point.has("isClickable")) point.getBoolean("isClickable") else true
                )
                
                whisperAnnotationsList.add(annotationData)
            }
            
            val clusteredAnnotations = clusterNearbyWhispers(whisperAnnotationsList)
            
            clusteredAnnotations.forEach { data ->
                if (data.isCluster) {
                    createClusterAnnotation(data)
                } else {
                    createSingleAnnotation(data.whispers.first())
                }
            }
            
            call.resolve(JSObject().put("status", "success"))
        }
    }
    
    @PluginMethod
    fun setCenterPoint(call: PluginCall) {
        bridge.activity.runOnUiThread {
            if (mapboxMap == null) {
                call.reject("Map is not initialized")
                return@runOnUiThread
            }
            
            val latitude = call.getDouble("latitude")
            val longitude = call.getDouble("longitude")
            val animated = call.getBoolean("animated") ?: true
            
            if (latitude == null || longitude == null) {
                call.reject("latitude and longitude are required")
                return@runOnUiThread
            }
            
            val cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(longitude, latitude))
                .build()
            
            mapboxMap?.setCamera(cameraOptions)
            
            call.resolve(JSObject().put("status", "success"))
        }
    }
    
    @PluginMethod
    fun setCenterAndZoom(call: PluginCall) {
        bridge.activity.runOnUiThread {
            if (mapboxMap == null) {
                call.reject("Map is not initialized")
                return@runOnUiThread
            }
            
            val latitude = call.getDouble("latitude")
            val longitude = call.getDouble("longitude")
            val zoom = call.getDouble("zoom")
            val animated = call.getBoolean("animated") ?: true
            
            if (latitude == null || longitude == null || zoom == null) {
                call.reject("latitude, longitude, and zoom are required")
                return@runOnUiThread
            }
            
            val cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(longitude, latitude))
                .zoom(zoom)
                .build()
            
            mapboxMap?.setCamera(cameraOptions)
            
            call.resolve(JSObject().put("status", "success"))
        }
    }
    
    @PluginMethod
    fun setZoomLevel(call: PluginCall) {
        bridge.activity.runOnUiThread {
            if (mapboxMap == null) {
                call.reject("Map is not initialized")
                return@runOnUiThread
            }
            
            val zoom = call.getDouble("zoom")
            val animated = call.getBoolean("animated") ?: true
            
            if (zoom == null) {
                call.reject("Zoom level is required")
                return@runOnUiThread
            }
            
            val currentCenter = mapboxMap?.cameraState?.center ?: return@runOnUiThread
            
            val cameraOptions = CameraOptions.Builder()
                .center(currentCenter)
                .zoom(zoom)
                .build()
            
            mapboxMap?.setCamera(cameraOptions)
            
            call.resolve(JSObject().put("status", "success"))
        }
    }
    
    @PluginMethod
    fun addCircle(call: PluginCall) {
        bridge.activity.runOnUiThread {
            if (circleAnnotationManager == null) {
                call.reject("Map is not initialized")
                return@runOnUiThread
            }
            
            val lat = call.getDouble("latitude")
            val lon = call.getDouble("longitude")
            val radius = call.getDouble("radius")
            
            if (lat == null || lon == null || radius == null) {
                call.reject("latitude, longitude, and radius are required")
                return@runOnUiThread
            }
            
            circleAnnotationManager?.deleteAll()
            
            val circleAnnotationOptions = CircleAnnotationOptions()
                .withPoint(Point.fromLngLat(lon, lat))
                .withCircleRadius(radiusMetersToPixels(radius))
                .withCircleColor("#00E5FF")
                .withCircleOpacity(0.3)
                .withCircleStrokeColor("#00E5FF")
                .withCircleStrokeWidth(2.0)
            
            circleAnnotationManager?.create(circleAnnotationOptions)
            
            currentUserRadius = radius
            autoSyncEnabled = true
            
            call.resolve(JSObject().put("status", "success").put("circleId", "user-circle"))
        }
    }
    
    @PluginMethod
    fun removeCircle(call: PluginCall) {
        bridge.activity.runOnUiThread {
            circleAnnotationManager?.deleteAll()
            
            autoSyncEnabled = false
            currentUserRadius = null
            
            call.resolve(JSObject().put("status", "success"))
        }
    }
    
    @PluginMethod
    fun clearMarkers(call: PluginCall) {
        bridge.activity.runOnUiThread {
            pointAnnotationManager?.deleteAll()
            whisperAnnotations.clear()
            clusterAnnotations.clear()
            cachedWhisperIds.clear()
            
            call.resolve(JSObject().put("status", "success"))
        }
    }
    
    @PluginMethod
    fun setMockUserLocation(call: PluginCall) {
        bridge.activity.runOnUiThread {
            val lat = call.getDouble("latitude")
            val lon = call.getDouble("longitude")
            
            if (lat == null || lon == null) {
                call.reject("latitude and longitude are required")
                return@runOnUiThread
            }
            
            if (lat == 0.0 && lon == 0.0) {
                mockUserAnnotation?.let { pointAnnotationManager?.delete(it) }
                mockUserAnnotation = null
                isMockModeActive = false
                
                mapView?.location?.updateSettings {
                    enabled = true
                }
                
                call.resolve(JSObject().put("status", "mock_disabled"))
                return@runOnUiThread
            }
            
            isMockModeActive = true
            
            mapView?.location?.updateSettings {
                enabled = false
            }
            
            mockUserAnnotation?.let { pointAnnotationManager?.delete(it) }
            
            val bitmap = createUserLocationBitmap()
            
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(lon, lat))
                .withIconImage(bitmap)
            
            mockUserAnnotation = pointAnnotationManager?.create(pointAnnotationOptions)
            
            call.resolve(JSObject().put("status", "success"))
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun setupMapListeners() {
        mapView?.mapboxMap?.addOnMapClickListener(OnMapClickListener { point ->
            mapView?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            
            val clickedPoint = mapboxMap?.pixelForCoordinate(point) ?: return@OnMapClickListener true
            
            pointAnnotationManager?.annotations?.forEach { annotation ->
                val annotationPoint = mapboxMap?.pixelForCoordinate(annotation.point) ?: return@forEach
                
                val distance = kotlin.math.sqrt(
                    (clickedPoint.x - annotationPoint.x).toDouble().pow(2) + 
                    (clickedPoint.y - annotationPoint.y).toDouble().pow(2)
                )
                
                if (distance < 30.0) {
                    val data = annotation.getData()?.asJsonObject
                    if (data != null) {
                        if (data.has("isCluster") && data.get("isCluster").asBoolean) {
                            val count = data.get("count").asInt
                            notifyListeners("onClusterTap", JSObject().apply {
                                put("count", count)
                                put("latitude", annotation.point.latitude())
                                put("longitude", annotation.point.longitude())
                            })
                            return@OnMapClickListener true
                        }
                        
                        if (data.has("whisperId")) {
                            val whisperId = data.get("whisperId").asString
                            val isClickable = if (data.has("isClickable")) data.get("isClickable").asBoolean else true
                            
                            if (isClickable && whisperId != null) {
                                notifyListeners("onWhisperTap", JSObject().apply {
                                    put("whisperId", whisperId)
                                })
                                return@OnMapClickListener true
                            }
                        }
                    }
                }
            }
            
            true
        })
        
        var lastZoom = mapboxMap?.cameraState?.zoom ?: 0.0
        mapboxMap?.addOnCameraChangeListener {
            val currentZoom = mapboxMap?.cameraState?.zoom ?: 0.0
            val zoomDelta = kotlin.math.abs(currentZoom - lastClusteredZoomLevel)
            
            if (zoomDelta >= 1.0 && lastClusteredZoomLevel > 0.0) {
                reclusterWhispers()
            }
            
            lastZoom = currentZoom
        }
    }
    
    private fun clusterNearbyWhispers(whispers: List<WhisperAnnotationData>): List<ClusterAnnotationData> {
        if (whispers.isEmpty()) return emptyList()
        
        val clustered = mutableListOf<ClusterAnnotationData>()
        val processed = mutableSetOf<String>()
        
        whispers.forEach { whisper ->
            if (processed.contains(whisper.whisperId)) return@forEach
            
            val nearbyWhispers = whispers.filter { other ->
                !processed.contains(other.whisperId) && 
                calculateDistance(whisper.latitude, whisper.longitude, other.latitude, other.longitude) <= clusteringThreshold
            }
            
            if (nearbyWhispers.size > 1) {
                val centerLat = nearbyWhispers.map { it.latitude }.average()
                val centerLon = nearbyWhispers.map { it.longitude }.average()
                
                clustered.add(ClusterAnnotationData(
                    isCluster = true,
                    whispers = nearbyWhispers,
                    latitude = centerLat,
                    longitude = centerLon,
                    count = nearbyWhispers.size
                ))
                
                nearbyWhispers.forEach { processed.add(it.whisperId) }
            } else {
                clustered.add(ClusterAnnotationData(
                    isCluster = false,
                    whispers = listOf(whisper),
                    latitude = whisper.latitude,
                    longitude = whisper.longitude,
                    count = 1
                ))
                processed.add(whisper.whisperId)
            }
        }
        
        lastClusteredZoomLevel = mapboxMap?.cameraState?.zoom ?: 0.0
        
        return clustered
    }
    
    private fun reclusterWhispers() {
        val allWhispers = whisperAnnotations.values.mapNotNull { annotation ->
            val id = annotation.getData()?.asJsonObject?.get("whisperId")?.asString ?: return@mapNotNull null
            
            WhisperAnnotationData(
                whisperId = id,
                latitude = annotation.point.latitude(),
                longitude = annotation.point.longitude(),
                label = "",
                markerSize = 60f,
                opacity = annotation.iconOpacity?.toFloat() ?: 1f,
                isClickable = true
            )
        }
        
        pointAnnotationManager?.deleteAll()
        whisperAnnotations.clear()
        
        val clustered = clusterNearbyWhispers(allWhispers)
        
        clustered.forEach { data ->
            if (data.isCluster) {
                createClusterAnnotation(data)
            } else {
                createSingleAnnotation(data.whispers.first())
            }
        }
    }
    
    private fun createSingleAnnotation(data: WhisperAnnotationData) {
        coroutineScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                generateCircularMarkerBitmap(
                    profileImageUrl = data.iconUrl,
                    initials = data.initials,
                    avatarColor = data.avatarColor,
                    expiryColor = data.expiryColor,
                    size = data.markerSize
                )
            }
            
            withContext(Dispatchers.Main) {
                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(data.longitude, data.latitude))
                    .withIconImage(bitmap)
                    .withIconOpacity(data.opacity.toDouble())
                    .withData(com.google.gson.JsonObject().apply {
                        addProperty("whisperId", data.whisperId)
                        addProperty("isClickable", data.isClickable)
                    })
                
                val annotation = pointAnnotationManager?.create(pointAnnotationOptions)
                annotation?.let { whisperAnnotations[data.whisperId] = it }
            }
        }
    }
    
    private fun createClusterAnnotation(data: ClusterAnnotationData) {
        coroutineScope.launch {
            val mainWhisper = data.whispers.first()
            val moreCount = data.count - 1
            val moreText = "+$moreCount $moreWhispersTranslation"
            
            val bitmap = withContext(Dispatchers.IO) {
                generateClusterMarkerBitmap(
                    profileImageUrl = mainWhisper.iconUrl,
                    initials = mainWhisper.initials,
                    avatarColor = mainWhisper.avatarColor,
                    expiryColor = mainWhisper.expiryColor,
                    size = mainWhisper.markerSize,
                    moreText = moreText
                )
            }
            
            withContext(Dispatchers.Main) {
                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(data.longitude, data.latitude))
                    .withIconImage(bitmap)
                    .withData(com.google.gson.JsonObject().apply {
                        addProperty("isCluster", true)
                        addProperty("count", data.count)
                    })
                
                pointAnnotationManager?.create(pointAnnotationOptions)
            }
        }
    }
    
    private fun generateCircularMarkerBitmap(
        profileImageUrl: String?,
        initials: String?,
        avatarColor: String?,
        expiryColor: String?,
        size: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val borderColor = getBorderColorFromExpiry(expiryColor)
        val bgColor = parseHexColor(avatarColor) ?: Color.parseColor("#9CA3AF")
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val centerX = size / 2
        val centerY = size / 2
        val radius = (size - 6f) / 2
        
        val circlePath = Path()
        circlePath.addCircle(centerX, centerY, radius, Path.Direction.CW)
        canvas.clipPath(circlePath)
        
        if (!profileImageUrl.isNullOrEmpty()) {
            try {
                val url = URL(profileImageUrl)
                val profileBitmap = BitmapFactory.decodeStream(url.openStream())
                
                val scaledBitmap = Bitmap.createScaledBitmap(
                    profileBitmap,
                    (radius * 2).toInt(),
                    (radius * 2).toInt(),
                    true
                )
                
                canvas.drawBitmap(scaledBitmap, centerX - radius, centerY - radius, paint)
            } catch (e: Exception) {
                drawBackgroundAndContent(canvas, paint, bgColor, initials, size, centerX, centerY, radius)
            }
        } else {
            drawBackgroundAndContent(canvas, paint, bgColor, initials, size, centerX, centerY, radius)
        }
        
        canvas.restore()
        canvas.save()
        
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        return bitmap
    }
    
    private fun drawBackgroundAndContent(
        canvas: Canvas,
        paint: Paint,
        bgColor: Int,
        initials: String?,
        size: Float,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        paint.style = Paint.Style.FILL
        paint.color = bgColor
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        if (!initials.isNullOrEmpty() && initials != "?") {
            paint.color = Color.WHITE
            paint.textSize = size * 0.4f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            
            val textBounds = Rect()
            paint.getTextBounds(initials, 0, initials.length, textBounds)
            val textY = centerY + (textBounds.height() / 2)
            
            canvas.drawText(initials, centerX, textY, paint)
        } else {
            paint.color = Color.WHITE
            
            val headRadius = radius * 0.15f
            val headCenterY = centerY - (radius * 0.3f)
            canvas.drawCircle(centerX, headCenterY, headRadius, paint)
            
            val bodyRadius = radius * 0.25f
            val bodyCenterY = centerY + (radius * 0.5f)
            canvas.drawCircle(centerX, bodyCenterY, bodyRadius, paint)
        }
    }
    
    private fun generateClusterMarkerBitmap(
        profileImageUrl: String?,
        initials: String?,
        avatarColor: String?,
        expiryColor: String?,
        size: Float,
        moreText: String
    ): Bitmap {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f * 2
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val textBounds = Rect()
        textPaint.getTextBounds(moreText, 0, moreText.length, textBounds)
        
        val textSpacing = 4f * 2
        val totalHeight = size + textSpacing + textBounds.height() + 12f
        val totalWidth = maxOf(size, textBounds.width() + 20f)
        
        val bitmap = Bitmap.createBitmap(totalWidth.toInt(), totalHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val avatarBitmap = generateCircularMarkerBitmap(profileImageUrl, initials, avatarColor, expiryColor, size)
        val avatarX = (totalWidth - size) / 2
        canvas.drawBitmap(avatarBitmap, avatarX, 0f, null)
        
        val textY = size + textSpacing + textBounds.height()
        val textX = (totalWidth - textBounds.width()) / 2
        
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.strokeWidth = 4f
        textPaint.color = Color.BLACK
        canvas.drawText(moreText, textX, textY, textPaint)
        
        textPaint.style = Paint.Style.FILL
        textPaint.color = Color.WHITE
        canvas.drawText(moreText, textX, textY, textPaint)
        
        return bitmap
    }
    
    private fun createUserLocationBitmap(): Bitmap {
        val size = 44
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val blueColor = Color.parseColor("#007AFF")
        paint.color = Color.argb(64, 0, 122, 255)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        paint.color = blueColor
        canvas.drawCircle(size / 2f, size / 2f, 8f, paint)
        
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(size / 2f, size / 2f, 8f, paint)
        
        return bitmap
    }
    
    private fun getBorderColorFromExpiry(expiryColor: String?): Int {
        return when (expiryColor?.lowercase()) {
            "red" -> Color.parseColor("#F44336")
            "yellow" -> Color.parseColor("#FFEB3B")
            "green" -> Color.parseColor("#4CAF50")
            else -> Color.parseColor("#2196F3")
        }
    }
    
    private fun parseHexColor(hex: String?): Int? {
        if (hex.isNullOrEmpty()) return null
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }
    
    private fun radiusMetersToPixels(meters: Double): Double {
        val metersPerPixel = 156543.03392 * Math.cos(0.0 * Math.PI / 180) / 2.0.pow(mapboxMap?.cameraState?.zoom ?: 14.0)
        return meters / metersPerPixel
    }
    
    data class WhisperAnnotationData(
        val whisperId: String,
        val latitude: Double,
        val longitude: Double,
        val label: String,
        val iconUrl: String? = null,
        val initials: String? = null,
        val avatarColor: String? = null,
        val expiryColor: String? = null,
        val markerSize: Float = 60f,
        val opacity: Float = 1f,
        val isClickable: Boolean = true
    )
    
    data class ClusterAnnotationData(
        val isCluster: Boolean,
        val whispers: List<WhisperAnnotationData>,
        val latitude: Double,
        val longitude: Double,
        val count: Int
    )
    
    private fun createStatusBarGradient(activity: android.app.Activity, rootView: FrameLayout) {
        val statusBarHeight = getStatusBarHeight(activity)
        val gradientView = View(activity)
        
        val gradientDrawable = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb(153, 0, 0, 0),
                Color.argb(0, 0, 0, 0)
            )
        )
        
        gradientView.background = gradientDrawable
        gradientView.elevation = 10f
        
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            statusBarHeight
        )
        gradientView.layoutParams = layoutParams
        gradientView.visibility = View.GONE
        
        rootView.addView(gradientView)
        this.statusBarGradientView = gradientView
        
        android.util.Log.i("MapboxNativePlugin", "✅ Status bar gradient created (height: ${statusBarHeight}px)")
    }
    
    private fun getStatusBarHeight(activity: android.app.Activity): Int {
        var result = 0
        val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = activity.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
    
    override fun handleOnDestroy() {
        super.handleOnDestroy()
        coroutineScope.cancel()
    }
}
