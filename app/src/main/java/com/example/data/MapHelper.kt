package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlin.math.*

data class Landmark(
    val name: String,
    val lat: Double,
    val lng: Double,
    val description: String,
    val iconName: String
)

data class Street(
    val name: String,
    val points: List<Pair<Double, Double>>, // Lat Lng coordinates
    val trafficLevel: TrafficLevel // Green, Yellow, Red
)

enum class TrafficLevel(val label: String, val speedKmh: Int, val multiplier: Double) {
    LOW("Free-flow (Green)", 80, 1.0),
    MEDIUM("Moderate (Yellow)", 40, 1.3),
    HIGH("Heavy Jam (Red)", 12, 2.0)
}

object MapHelper {
    // Metro bounding coordinates
    const val CENTER_LAT = 55.7539
    const val CENTER_LNG = 37.6208
    const val LAT_SPAN = 0.15
    const val LNG_SPAN = 0.22

    val landmarks = listOf(
        Landmark("Red Square & Kremlin", 55.7539, 37.6208, "Historic city center and landmarks.", "castle"),
        Landmark("Moscow City Financial", 55.7483, 37.5385, "Skyscrapers and modern businesses.", "business"),
        Landmark("Gorky Central Park", 55.7294, 37.6015, "Beautiful park and recreation area.", "park"),
        Landmark("Belorussky Train Station", 55.7762, 37.5812, "Major transport and express trains hub.", "train"),
        Landmark("MSU Sparrow Hills", 55.7031, 37.5312, "Scenic view of the city near university.", "school"),
        Landmark("Luzhniki Olympic Stadium", 55.7158, 37.5536, "Sports complex and running trails.", "sports"),
        Landmark("Tretyakov Gallery", 55.7414, 37.6208, "Famous Russian national art gallery.", "palette"),
        Landmark("VDNKh Exhibition", 55.8263, 37.6376, "Historic pavilions and space displays.", "rocket")
    )

    val streets = listOf(
        Street(
            "Tverskaya Radial Street",
            listOf(55.7539 to 37.6208, 55.7595 to 37.6111, 55.7681 to 37.5994, 55.7762 to 37.5812),
            TrafficLevel.MEDIUM
        ),
        Street(
            "Garden Circular Ring",
            listOf(55.7681 to 37.5994, 55.7719 to 37.6370, 55.7414 to 37.6430, 55.7294 to 37.6015, 55.7483 to 37.5815, 55.7681 to 37.5994),
            TrafficLevel.HIGH
        ),
        Street(
            "Kutuzovsky Avenue",
            listOf(55.7483 to 37.5815, 55.7450 to 37.5500, 55.7483 to 37.5385),
            TrafficLevel.LOW
        ),
        Street(
            "Leningradsky Expressway",
            listOf(55.7762 to 37.5812, 55.7950 to 37.5400, 55.8150 to 37.5100),
            TrafficLevel.MEDIUM
        ),
        Street(
            "Moskva River Drive",
            listOf(55.7031 to 37.5312, 55.7158 to 37.5536, 55.7294 to 37.6015, 55.7414 to 37.6208, 55.7539 to 37.6450),
            TrafficLevel.LOW
        )
    )

    // Haversine formula for distance in km
    fun getDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // Estimate minutes of travel
    fun estimateTravelTimeMinutes(distanceKm: Double, level: TrafficLevel): Int {
        val baseSpeed = level.speedKmh
        val rawTimeHours = distanceKm / baseSpeed
        val minutes = (rawTimeHours * 60).roundToInt()
        return max(3, minutes)
    }

    // Calculate dynamic fare
    fun calculateFare(distanceKm: Double, carType: String, trafficLevel: TrafficLevel): Double {
        val baseRate = when (carType) {
            "Economy" -> 120.0
            "Comfort" -> 220.0
            "Business" -> 450.0
            else -> 150.0
        }
        val perKmRate = when (carType) {
            "Economy" -> 15.0
            "Comfort" -> 25.0
            "Business" -> 50.0
            else -> 20.0
        }
        val trafficSurcharge = trafficLevel.multiplier
        val price = (baseRate + (distanceKm * perKmRate)) * trafficSurcharge
        return (price / 10).roundToInt() * 10.0 // Round to nearest 10 for clean look
    }

    // Generate simulated road routing points to make car animation look beautiful (following street grids)
    fun generateRoutePoints(startLat: Double, startLng: Double, endLat: Double, endLng: Double): List<Pair<Double, Double>> {
        val route = mutableListOf<Pair<Double, Double>>()
        route.add(startLat to startLng)

        // Find standard node intersections to make routing realistic instead of straight lines
        // Let's connect them via intermediate corner or street nodes to represent real street grid
        val midLat = startLat
        val midLng = endLng

        // Interpolate first leg (horizontal-ish or vertical-ish)
        val steps = 15
        for (i in 1..steps) {
            val fraction = i.toDouble() / steps
            val lat = startLat + (midLat - startLat) * fraction
            val lng = startLng + (midLng - startLng) * fraction
            route.add(lat to lng)
        }

        // Interpolate second leg
        for (i in 1..steps) {
            val fraction = i.toDouble() / steps
            val lat = midLat + (endLat - midLat) * fraction
            val lng = midLng + (endLng - midLng) * fraction
            route.add(lat to lng)
        }

        route.add(endLat to endLng)
        return route.distinct()
    }

    // Get closest landmark name for coordinates
    fun getClosestLabel(lat: Double, lng: Double): String {
        var minDistance = Double.MAX_VALUE
        var closest = "Custom Pin Landmark"
        for (landmark in landmarks) {
            val d = getDistanceKm(lat, lng, landmark.lat, landmark.lng)
            if (d < minDistance) {
                minDistance = d
                closest = landmark.name
            }
        }
        return if (minDistance < 1.2) {
            closest
        } else {
            String.format("Street point (%.4f, %.4f)", lat, lng)
        }
    }

    // Launch Yandex Navigator or Yandex Maps App via Intent
    fun launchYandexMaps(context: Context, pickupLat: Double, pickupLng: Double, dropoffLat: Double, dropoffLng: Double) {
        val geoUriString = "yandexmaps://maps.yandex.ru/?rtext=$pickupLat,$pickupLng~$dropoffLat,$dropoffLng&rtt=auto"
        val fallbackWebUri = "https://yandex.ru/maps/?rtext=$pickupLat,$pickupLng~$dropoffLat,$dropoffLng&rtt=auto"
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUriString))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to Web Browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackWebUri))
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not open map navigation: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launch single target Navigator point
    fun launchYandexNavigator(context: Context, targetLat: Double, targetLng: Double) {
        val naviUriString = "yandexnavi://build_route_on_map?lat_to=$targetLat&lon_to=$targetLng"
        val fallbackWebUri = "https://yandex.ru/maps/?rtext=~$targetLat,$targetLng&rtt=auto"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(naviUriString))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser mapping
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackWebUri))
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not launch Yandex Navigator: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
