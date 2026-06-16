package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlin.math.*

data class Landmark(
    val nameEn: String,
    val nameHy: String,
    val nameRu: String,
    val lat: Double,
    val lng: Double,
    val descriptionEn: String,
    val descriptionHy: String,
    val descriptionRu: String,
    val iconName: String
)

data class Street(
    val name: String,
    val points: List<Pair<Double, Double>>, // Lat Lng coordinates
    val trafficLevel: TrafficLevel // Green, Yellow, Red
)

data class StreetInfo(
    val en: String,
    val hy: String,
    val ru: String,
    val lat: Double,
    val lng: Double
)

data class SearchResult(
    val en: String,
    val hy: String,
    val ru: String,
    val lat: Double,
    val lng: Double
) {
    fun getLabel(lang: String): String = when (lang) {
        "hy" -> hy
        "ru" -> ru
        else -> en
    }
}

enum class TrafficLevel(val label: String, val speedKmh: Int, val multiplier: Double) {
    LOW("Free-flow (Green)", 80, 1.0),
    MEDIUM("Moderate (Yellow)", 40, 1.3),
    HIGH("Heavy Jam (Red)", 12, 2.0)
}

object MapHelper {
    // Metro bounding coordinates
    const val CENTER_LAT = 40.1777
    const val CENTER_LNG = 44.5126
    const val LAT_SPAN = 0.08
    const val LNG_SPAN = 0.12

    val landmarks = listOf(
        Landmark("Republic Square", "Հանրապետության Հրապարակ", "Площадь Республики", 40.1777, 44.5126, "Central town square in Yerevan with fountains.", "Կենտրոնական հրապարակ Երևանում՝ երգող շատրվաններով։", "Центральная городская площадь в Ереване с фонтанами.", "castle"),
        Landmark("Cascade Complex", "Կասկադ Համալիր", "Каскад", 40.1911, 44.5193, "Giant stairway with art exhibitions and viewpoint.", "Հսկայական աստիճանավանդակ՝ արվեստի ցուցահանդեսներով և դիտակետով։", "Гигантская лестница с художественными выставками и смотровой площадкой.", "business"),
        Landmark("Zvartnots Airport", "Զվարթնոց Օդանավակայան", "Аэропорт Звартноц", 40.1583, 44.3965, "International airport serving Armenia's capital.", "Միջազգային օդանավակայան Երևանում։", "Международный аэропорт, обслуживающий Ереван.", "train"),
        Landmark("Northern Avenue", "Հյուսիսային Պողոտա", "Северный Проспект", 40.1818, 44.5147, "Pedestrian avenue with upscale shops and restaurants.", "Հետիոտնային պողոտա՝ բարձրակարգ խանութներով և ռեստորաններով։", "Пешеходная торговая улица с элитными магазинами и ресторанами.", "park"),
        Landmark("Matenadaran Library", "Մատենադարան", "Матенадаран", 40.1920, 44.5211, "Repository of ancient Armenian manuscripts.", "Հին ձեռագրերի թանգարան-ինստիտուտ։", "Хранилище древних армянских рукописей.", "school"),
        Landmark("English Park", "Անգլիական Այգի", "Английский Парк", 40.1741, 44.5074, "Elegant public park near Republic Square.", "Հանրապետության հրապարակի մոտ գտնվող գեղեցիկ հանրային այգի։", "Элегантный городской парк рядом с Площадью Республики.", "sports"),
        Landmark("Vernissage Market", "Վերնիսաժ", "Вернисаж", 40.1755, 44.5173, "Large open-air crafts and souvenirs market.", "Բացօթյա մեծ տոնավաճառ՝ ձեռագործ աշխատանքների և հուշանվերների համար։", "Крупный рынок изделий ручной работы и сувениров под открытым небом.", "palette"),
        Landmark("Dalma Garden Mall", "Դալմա Գարդեն Մոլ", "Далма Гарден Молл", 40.1815, 44.4880, "Large dynamic shopping and entertainment hub.", "Մեծ առևտրի և զվարճանքի կենտրոն Երևանում։", "Крупный торгово-развлекательный центр.", "rocket")
    )

    val streets = listOf(
        Street(
            "Abovyan Street",
            listOf(40.1777 to 44.5126, 40.1830 to 44.5150, 40.1890 to 44.5180, 40.1911 to 44.5193),
            TrafficLevel.MEDIUM
        ),
        Street(
            "Mashtots Avenue Circular",
            listOf(40.1741 to 44.5074, 40.1818 to 44.5110, 40.1890 to 44.5130, 40.1920 to 44.5211, 40.1815 to 44.4880, 40.1741 to 44.5074),
            TrafficLevel.HIGH
        ),
        Street(
            "Sayat-Nova Avenue",
            listOf(40.1890 to 44.5130, 40.1860 to 44.5200, 40.1820 to 44.5290),
            TrafficLevel.LOW
        ),
        Street(
            "Baghramyan Avenue",
            listOf(40.1890 to 44.5110, 40.1920 to 44.5010, 40.1950 to 44.4920),
            TrafficLevel.MEDIUM
        ),
        Street(
            "Hrazdan River Gorge Drive",
            listOf(40.1980 to 44.4800, 40.1880 to 44.4750, 40.1780 to 44.4720, 40.1700 to 44.4700),
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
    fun getClosestLabel(lat: Double, lng: Double, lang: String): String {
        var minDistance = Double.MAX_VALUE
        var closestEn = "Custom Pin Landmark"
        var closestHy = "Կետ քարտեզի վրա"
        var closestRu = "Точка на карте"
        
        for (landmark in landmarks) {
            val d = getDistanceKm(lat, lng, landmark.lat, landmark.lng)
            if (d < minDistance) {
                minDistance = d
                closestEn = landmark.nameEn
                closestHy = landmark.nameHy
                closestRu = landmark.nameRu
            }
        }
        
        return if (minDistance < 1.2) {
            when (lang) {
                "hy" -> closestHy
                "ru" -> closestRu
                else -> closestEn
            }
        } else {
            when (lang) {
                "hy" -> String.format("Փողոցային կետ (%.4f, %.4f)", lat, lng)
                "ru" -> String.format("Уличная точка (%.4f, %.4f)", lat, lng)
                else -> String.format("Street point (%.4f, %.4f)", lat, lng)
            }
        }
    }

    // Powerful local Yandex simulated geocoder containing rich Yerevan streets and addresses
    fun searchYerevanAddress(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val cleanQuery = query.trim().lowercase()

        val baseStreets = listOf(
            StreetInfo("Abovyan Street", "Աբովյան փողոց", "Улица Абовяна", 40.1813, 44.5140),
            StreetInfo("Sayat-Nova Avenue", "Սայաթ-Նովա պողոտա", "Проспект Саят-Нова", 40.1855, 44.5210),
            StreetInfo("Mashtots Avenue", "Մաշտոցի պողոտա", "Проспект Маштоца", 40.1843, 44.5101),
            StreetInfo("Baghramyan Avenue", "Բաղրամյան պողոտա", "Проспект Баграмяна", 40.1925, 44.5015),
            StreetInfo("Tumanyan Street", "Թումանյան փողոց", "Улица Туманяна", 40.1835, 44.5126),
            StreetInfo("Teryan Street", "Տերյան փողոց", "Улица Теряна", 40.1852, 44.5135),
            StreetInfo("Pushkin Street", "Պուշկինի փողոց", "Улица Пушкина", 40.1812, 44.5110),
            StreetInfo("Nalbandyan Street", "Նալբանդյան փողոց", "Улица Налбандяна", 40.1795, 44.5165),
            StreetInfo("Koghbatsi Street", "Կողբացու փողոց", "Улица Кохбаци", 40.1805, 44.5090),
            StreetInfo("Amiryan Street", "Ամիրյան փողոց", "Улица Амиряна", 40.1770, 44.5085),
            StreetInfo("Saryan Street", "Սարյան փողոց", "Улица Сарьяна", 40.1840, 44.5050),
            StreetInfo("Komitas Avenue", "Կոմիտասի պողոտա", "Проспект Комитаса", 40.2080, 44.5050),
            StreetInfo("Kievyan Street", "Կիևյան փողոց", "Улица Киевян", 40.1970, 44.4850),
            StreetInfo("Myasnikyan Avenue", "Մյասնիկյան պողոտա", "Проспект Мясникяна", 40.1950, 44.5450),
            StreetInfo("Charents Street", "Չարենցի փողոց", "Улица Чаренца", 40.1840, 44.5280),
            StreetInfo("Halabyan Street", "Հալաբյան փողոց", "Улица Алабяна", 40.1980, 44.4750),
            StreetInfo("Leningradyan Street", "Լենինգրադյան փողոց", "Улица Ленинградян", 40.1870, 44.4700),
            StreetInfo("Erebuni Street", "Էրեբունու փողոց", "Улица Эребуни", 40.1450, 44.5300),
            StreetInfo("Davtashen 1st Block", "Դավթաշեն 1-ին թաղամաս", "Давташен 1-й квартал", 40.2150, 44.4820),
            StreetInfo("Nor Nork 2nd Block", "Նոր Նորք 2-րդ զանգված", "Нор Норк 2-й массив", 40.1900, 44.5600),
            StreetInfo("Shengavit District", "Շենգավիթ համայնք", "Район Шенгавит", 40.1450, 44.4850),
            StreetInfo("Avan-Arinj", "Ավան-Առինջ", "Ավան-Արինջ", 40.2220, 44.5750),
            StreetInfo("Republic Square", "Հանրապետության Հրապարակ", "Площадь Республики", 40.1777, 44.5126),
            StreetInfo("Cascade Complex", "Կասկադ Համալիր", "Каскад", 40.1911, 44.5193),
            StreetInfo("Vernissage Market", "Վերնիսաժ", "Вернисаж", 40.1755, 44.5173),
            StreetInfo("Dalma Garden Mall", "Դալմա Գարդեն Մոլ", "Далма Гарден Молл", 40.1815, 44.4880),
            StreetInfo("Rio Mall", "Ռիո Մոլ", "Рио Молл", 40.2010, 44.5120),
            StreetInfo("Yerevan Mall", "Երևան Մոլ", "Ереван Молл", 40.1550, 44.5050),
            StreetInfo("Buzand Street", "Բուզանդի փողոց", "Улица Бузанда", 40.1772, 44.5095),
            StreetInfo("Aram Street", "Արամի փողոց", "Улица Арама", 40.1790, 44.5112),
            StreetInfo("Tigran Mets Avenue", "Տիգրան Մեծի պողոտա", "Проспект Тиграна Меца", 40.1650, 44.5150),
            StreetInfo("Zakyan Street", "Զաքյան փողոց", "Улица Закяна", 40.1752, 44.5065),
            StreetInfo("Khorenatsi Street", "Խորենացու փողոց", "Улица Хоренаци", 40.1715, 44.5100),
            StreetInfo("Koryun Street", "Կորյունի փողոց", "Улица Корюна", 40.1885, 44.5230),
            StreetInfo("Paronyan Street", "Պարոնյան փողոց", "Улица Пароняна", 40.1825, 44.5005),
            StreetInfo("Leo Street", "Լեոյի փողոց", "Улица Лео", 40.1802, 44.5042),
            StreetInfo("Davit Anhaght Street", "Դավիթ Անհաղթի փողոց", "Улица Давида Анахта", 40.2030, 44.5310),
            StreetInfo("Rubinyants Street", "Ռուբինյանց փողոց", "Улица Рубинянца", 40.2085, 44.5420),
            StreetInfo("Gai Avenue", "Գայի պողոտա", "Проспект Гая", 40.1980, 44.5650),
            StreetInfo("Sebastia Street", "Սեբաստիայի փողոց", "Улица Себастия", 40.1780, 44.4640),
            StreetInfo("Raffi Street", "Րաֆֆու փողոց", "Улица Раффи", 40.1720, 44.4520),
            StreetInfo("Isakov Avenue", "Իսակովի պողոտա", "Проспект Исакова", 40.1650, 44.4850),
            StreetInfo("Arshakunyats Avenue", "Արշակունյաց պողոտա", "Проспект Аршакуняц", 40.1550, 44.5020),
            StreetInfo("Garegin Nzhdeh Street", "Գարեգին Նժդեհի փողոց", "Улица Гарегина Нжде", 40.1510, 44.4830),
            StreetInfo("Artashisyan Street", "Արտաշիսյան փողոց", "Улица Арташисяна", 40.1380, 44.4750),
            StreetInfo("Shirak Street", "Շիրակի փողոց", "Улица Ширака", 40.1320, 44.4710),
            StreetInfo("Monte Melkonyan Street", "Մոնթե Մելքոնյան փողոց", "Улица Монте Мелконяна", 40.1850, 44.4750),
            StreetInfo("Tsitsernakaberd Highway", "Ծիծեռնակաբերդի խճուղի", "Цицернакабердское шоссе", 40.1870, 44.4900),
            StreetInfo("Atenk Street", "Աթենքի փողոց", "Улица Атенка", 40.1780, 44.4920),
            StreetInfo("Saralanj Highway", "Սարալանջի խճուղի", "Сараланджское шоссе", 40.1910, 44.5270),
            StreetInfo("Melik-Adamyan Street", "Մելիք-Ադամյան փողոց", "Улица Мелик-Адамяна", 40.1765, 44.5120),
            StreetInfo("Lovers' Park", "Սիրահարների Այգի", "Парк Влюбленных", 40.1920, 44.5035),
            StreetInfo("English Park", "Անգլիական Այգի", "Английский Парк", 40.1741, 44.5074),
            StreetInfo("Victory Park", "Հաղթանակի Զբոսայգի", "Парк Победы", 40.1960, 44.5230),
            StreetInfo("Tumo Center", "Թումո Կենտրոն", "Центр Тумо", 40.1975, 44.4770),
            StreetInfo("Blue Mosque", "Կապույտ Մզկիթ", "Голубая Мечеть", 40.1785, 44.5056)
        )

        // Find potential house numbers (digits inside the query)
        val numberPattern = "\\b(\\d+)\\b".toRegex()
        val matchResult = numberPattern.find(cleanQuery)
        val houseNum = matchResult?.value ?: ""
        val queryWithoutNum = cleanQuery.replace(houseNum, "").trim()

        val results = mutableListOf<SearchResult>()
        for (street in baseStreets) {
            val matchesEn = street.en.lowercase().contains(queryWithoutNum) || queryWithoutNum.contains(street.en.lowercase())
            val matchesHy = street.hy.lowercase().contains(queryWithoutNum) || queryWithoutNum.contains(street.hy.lowercase())
            val matchesRu = street.ru.lowercase().contains(queryWithoutNum) || queryWithoutNum.contains(street.ru.lowercase())

            if (matchesEn || matchesHy || matchesRu || queryWithoutNum.isBlank()) {
                val numValue = houseNum.toIntOrNull() ?: 1
                val offsetLat = ((numValue % 10) - 5) * 0.0003
                val offsetLng = ((numValue % 10) - 5) * 0.0004

                val displayEn = if (houseNum.isNotEmpty()) "${street.en} $houseNum" else street.en
                val displayHy = if (houseNum.isNotEmpty()) "${street.hy} $houseNum" else street.hy
                val displayRu = if (houseNum.isNotEmpty()) "${street.ru} $houseNum" else street.ru

                results.add(SearchResult(
                    en = displayEn,
                    hy = displayHy,
                    ru = displayRu,
                    lat = street.lat + offsetLat,
                    lng = street.lng + offsetLng
                ))
            }
        }
        return results.take(5)
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
