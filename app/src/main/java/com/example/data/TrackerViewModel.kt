package com.example.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class UserRole {
    PASSENGER, DRIVER
}

data class UserProfile(
    val name: String,
    val phone: String,
    val email: String,
    val role: String, // PASSENGER or DRIVER
    val vehicleModel: String? = null,
    val vehicleColor: String? = null,
    val licensePlate: String? = null,
    val vehicleClass: String? = null // Economy, Comfort, Business, Cargo
)

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = OrderRepository(database.orderDao())

    // UI States
    val currentUser = MutableStateFlow<UserProfile?>(null)
    val currentRole = MutableStateFlow(UserRole.PASSENGER)
    val currentLanguage = MutableStateFlow("en")
    val orders = repository.allOrders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingOrders = repository.pendingOrders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection details (Passenger view)
    val pickupName = MutableStateFlow("Republic Square")
    val pickupLat = MutableStateFlow(40.1777)
    val pickupLng = MutableStateFlow(44.5126)

    val dropoffName = MutableStateFlow("Northern Avenue")
    val dropoffLat = MutableStateFlow(40.1818)
    val dropoffLng = MutableStateFlow(44.5147)

    val selectedCarType = MutableStateFlow("Comfort")
    val currentTrafficLevel = MutableStateFlow(TrafficLevel.MEDIUM)

    // Active order flow (for both roles)
    val activeOrder = MutableStateFlow<OrderEntity?>(null)

    // Driver specific status
    val driverOnline = MutableStateFlow(true)
    val driverLat = MutableStateFlow(40.1911) // Initialized at Cascade Complex
    val driverLng = MutableStateFlow(44.5193)

    // Drive Navigation simulation variables
    private var simulationJob: Job? = null
    val simulationRoute = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val currentRouteStepIndex = MutableStateFlow(0)
    val isSimulating = MutableStateFlow(false)
    val navigationText = MutableStateFlow("Awaiting route accept...")

    init {
        FirestoreHelper.initialize(application)
        
        // Restore user state from SharedPreferences
        val sharedPrefs = application.getSharedPreferences("navigator_prefs", android.content.Context.MODE_PRIVATE)
        val name = sharedPrefs.getString("user_name", "") ?: ""
        val phone = sharedPrefs.getString("user_phone", "") ?: ""
        val email = sharedPrefs.getString("user_email", "") ?: ""
        val role = sharedPrefs.getString("user_role", "") ?: ""
        
        if (name.isNotEmpty() && phone.isNotEmpty() && role.isNotEmpty()) {
            val vehicleModel = sharedPrefs.getString("driver_vehicle", null)
            val vehicleColor = sharedPrefs.getString("driver_color", null)
            val licensePlate = sharedPrefs.getString("driver_plate", null)
            val vehicleClass = sharedPrefs.getString("driver_class", null)
            
            val profile = UserProfile(
                name = name,
                phone = phone,
                email = email,
                role = role,
                vehicleModel = vehicleModel,
                vehicleColor = vehicleColor,
                licensePlate = licensePlate,
                vehicleClass = vehicleClass
            )
            currentUser.value = profile
            currentRole.value = if (role == "DRIVER") UserRole.DRIVER else UserRole.PASSENGER
        }

        // Automatically check if there is an active order in the orders list that's not completed or cancelled
        viewModelScope.launch {
            orders.collect { orderList ->
                val active = orderList.firstOrNull { it.status != "COMPLETED" && it.status != "CANCELLED" }
                if (active != null) {
                    activeOrder.value = active
                    // If we have an active order, and we are the driver or passenger, we need to handle its simulation
                    if (active.status == "ACCEPTED" || active.status == "ARRIVED" || active.status == "IN_PROGRESS") {
                        if (simulationJob == null) {
                            startVehicleSimulation(active)
                        }
                    }
                } else if (activeOrder.value?.status == "COMPLETED" || activeOrder.value?.status == "CANCELLED") {
                    // Reset
                }
            }
        }
    }

    fun registerUser(profile: UserProfile) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("navigator_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("user_name", profile.name)
            putString("user_phone", profile.phone)
            putString("user_email", profile.email)
            putString("user_role", profile.role)
            putString("driver_vehicle", profile.vehicleModel)
            putString("driver_color", profile.vehicleColor)
            putString("driver_plate", profile.licensePlate)
            putString("driver_class", profile.vehicleClass)
            apply()
        }
        currentUser.value = profile
        currentRole.value = if (profile.role == "DRIVER") UserRole.DRIVER else UserRole.PASSENGER
        FirestoreHelper.syncUserProfile(profile)
    }

    fun logout() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("navigator_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
        currentUser.value = null
    }

    // Set pickup coordinates
    fun setPickup(lat: Double, lng: Double) {
        pickupLat.value = lat
        pickupLng.value = lng
        pickupName.value = MapHelper.getClosestLabel(lat, lng, currentLanguage.value)
    }

    // Set dropoff coordinates
    fun setDropoff(lat: Double, lng: Double) {
        dropoffLat.value = lat
        dropoffLng.value = lng
        dropoffName.value = MapHelper.getClosestLabel(lat, lng, currentLanguage.value)
    }

    // Reset current pickup/dropoff selections
    fun swapLocations() {
        val tempL = pickupLat.value
        val tempG = pickupLng.value
        val tempN = pickupName.value

        pickupLat.value = dropoffLat.value
        pickupLng.value = dropoffLng.value
        pickupName.value = dropoffName.value

        dropoffLat.value = tempL
        dropoffLng.value = tempG
        dropoffName.value = tempN
    }

    // Book/Create Order (Passenger role)
    fun createOrder() {
        viewModelScope.launch {
            val dist = MapHelper.getDistanceKm(pickupLat.value, pickupLng.value, dropoffLat.value, dropoffLng.value)
            val fareVal = MapHelper.calculateFare(dist, selectedCarType.value, currentTrafficLevel.value)
            val minutes = MapHelper.estimateTravelTimeMinutes(dist, currentTrafficLevel.value)

            val order = OrderEntity(
                pickupAddress = pickupName.value,
                pickupLat = pickupLat.value,
                pickupLng = pickupLng.value,
                dropoffAddress = dropoffName.value,
                dropoffLat = dropoffLat.value,
                dropoffLng = dropoffLng.value,
                status = "PENDING",
                carType = selectedCarType.value,
                fare = fareVal,
                estimatedTimeMinutes = minutes
            )
            val id = repository.insertOrder(order)
            val insertedOrder = order.copy(id = id)
            activeOrder.value = insertedOrder
            FirestoreHelper.syncOrder(insertedOrder)
            Log.d("TrackerViewModel", "Created Order: $insertedOrder")
        }
    }

    // Driver Action: Online toggle
    fun toggleDriverOnline() {
        driverOnline.value = !driverOnline.value
    }

    // Driver Action: Accept a pending order
    fun acceptOrder(order: OrderEntity) {
        viewModelScope.launch {
            val updated = order.copy(
                status = "ACCEPTED",
                driverId = "Driver Ivan (Cab 243)",
                driverLat = driverLat.value,
                driverLng = driverLng.value
            )
            repository.updateOrder(updated)
            activeOrder.value = updated
            FirestoreHelper.syncOrder(updated)
            startVehicleSimulation(updated)
        }
    }

    // Driver Action: Arrived at pickup
    fun markArrivedAtPickup() {
        val order = activeOrder.value ?: return
        viewModelScope.launch {
            val updated = order.copy(status = "ARRIVED")
            repository.updateOrder(updated)
            activeOrder.value = updated
            FirestoreHelper.syncOrder(updated)
            navigationText.value = "Arrived at Pickup. Waiting for customer..."
        }
    }

    // Driver Action: Passenger boarded, journey starts
    fun startJourney() {
        val order = activeOrder.value ?: return
        viewModelScope.launch {
            val updated = order.copy(status = "IN_PROGRESS")
            repository.updateOrder(updated)
            activeOrder.value = updated
            FirestoreHelper.syncOrder(updated)
            startJourneySimulation(updated)
        }
    }

    // Driver / Passenger Action: Cancel order
    fun cancelOrder() {
        val order = activeOrder.value ?: return
        viewModelScope.launch {
            simulationJob?.cancel()
            simulationJob = null
            isSimulating.value = false
            simulationRoute.value = emptyList()

            val updated = order.copy(status = "CANCELLED")
            repository.updateOrder(updated)
            FirestoreHelper.syncOrder(updated)
            activeOrder.value = null
        }
    }

    // Clear history
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            FirestoreHelper.clearAllOrdersFromFirestore()
            activeOrder.value = null
        }
    }

    // Driver Simulation Task: Moving from current Driver Location -> Pickup Location
    private fun startVehicleSimulation(order: OrderEntity) {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            isSimulating.value = true
            navigationText.value = "Navigating to Pickup point..."
            
            // Route from driver current position to pickup location
            val route = MapHelper.generateRoutePoints(
                driverLat.value, driverLng.value,
                order.pickupLat, order.pickupLng
            )
            simulationRoute.value = route
            currentRouteStepIndex.value = 0

            for (i in route.indices) {
                delay(350) // Simulation speed interval (smooth movement of vehicle)
                val coord = route[i]
                driverLat.value = coord.first
                driverLng.value = coord.second
                currentRouteStepIndex.value = i

                // Update server side / db representation with current vehicle position
                val current = activeOrder.value?.copy(
                    driverLat = coord.first,
                    driverLng = coord.second
                )
                activeOrder.value = current
                if (current != null) {
                    FirestoreHelper.syncOrder(current)
                }

                val dist = MapHelper.getDistanceKm(coord.first, coord.second, order.pickupLat, order.pickupLng)
                if (dist < 0.2) {
                    navigationText.value = "Arrived near pickup location. Tap 'Arrived'."
                } else {
                    navigationText.value = "Driving to Pickup. Distance: ${String.format("%.1f", dist)} km"
                }
            }

            // Automatically switch status to ARRIVED if user is looking at simulation or let driver declare it
            markArrivedAtPickup()
        }
    }

    // Driver Simulation Task: Moving from Pickup Location -> Dropoff Location
    private fun startJourneySimulation(order: OrderEntity) {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            isSimulating.value = true
            navigationText.value = "Journey in progress. Driving to Destination..."

            val route = MapHelper.generateRoutePoints(
                order.pickupLat, order.pickupLng,
                order.dropoffLat, order.dropoffLng
            )
            simulationRoute.value = route
            currentRouteStepIndex.value = 0

            for (i in route.indices) {
                delay(400) // Travel step simulation
                val coord = route[i]
                driverLat.value = coord.first
                driverLng.value = coord.second
                currentRouteStepIndex.value = i

                // Update order driver coordinates in the repository
                val updatedOrder = activeOrder.value?.copy(
                    driverLat = coord.first,
                    driverLng = coord.second
                )
                activeOrder.value = updatedOrder
                if (updatedOrder != null) {
                    FirestoreHelper.syncOrder(updatedOrder)
                }

                val dist = MapHelper.getDistanceKm(coord.first, coord.second, order.dropoffLat, order.dropoffLng)
                val remainingMins = MapHelper.estimateTravelTimeMinutes(dist, currentTrafficLevel.value)
                navigationText.value = "Navigating to: ${order.dropoffAddress}. Rem: ${String.format("%.1f", dist)} km (~$remainingMins min)"
            }

            // Completed!
            navigationText.value = "Destination reached safely!"
            delay(1000)
            
            val finalOrder = activeOrder.value?.copy(status = "COMPLETED")
            if (finalOrder != null) {
                repository.updateOrder(finalOrder)
                FirestoreHelper.syncOrder(finalOrder)
            }
            activeOrder.value = null
            isSimulating.value = false
            simulationRoute.value = emptyList()
            simulationJob = null
        }
    }

    fun setLanguage(langCode: String) {
        currentLanguage.value = langCode
        pickupName.value = MapHelper.getClosestLabel(pickupLat.value, pickupLng.value, langCode)
        dropoffName.value = MapHelper.getClosestLabel(dropoffLat.value, dropoffLng.value, langCode)
    }
}
