package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    private var isFirestoreInitialized = false
    private var firestore: FirebaseFirestore? = null

    fun initialize(context: Context) {
        try {
            // Check if Firebase is initialized. If not, try initializing it.
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            firestore = FirebaseFirestore.getInstance()
            isFirestoreInitialized = true
            Log.d(TAG, "Firebase Firestore initialized successfully!")
        } catch (e: Throwable) {
            isFirestoreInitialized = false
            firestore = null
            Log.w(TAG, "Firebase Firestore could not be initialized (Missing google-services.json?): ${e.localizedMessage}")
        }
    }

    fun isAvailable(): Boolean {
        return isFirestoreInitialized && firestore != null
    }

    // Sync/Upload an order state write to Firestore
    fun syncOrder(order: OrderEntity) {
        if (!isAvailable()) return
        try {
            val db = firestore ?: return
            val orderMap = mutableMapOf<String, Any>()
            orderMap["id"] = order.id
            orderMap["pickupAddress"] = order.pickupAddress
            orderMap["pickupLat"] = order.pickupLat
            orderMap["pickupLng"] = order.pickupLng
            orderMap["dropoffAddress"] = order.dropoffAddress
            orderMap["dropoffLat"] = order.dropoffLat
            orderMap["dropoffLng"] = order.dropoffLng
            orderMap["status"] = order.status
            orderMap["carType"] = order.carType
            orderMap["fare"] = order.fare
            orderMap["estimatedTimeMinutes"] = order.estimatedTimeMinutes
            orderMap["driverId"] = order.driverId ?: ""
            orderMap["driverLat"] = order.driverLat
            orderMap["driverLng"] = order.driverLng
            db.collection("all_orders")
                .document(order.id.toString())
                .set(orderMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Order #${order.id} automatically synced to Firestore successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed syncing order #${order.id} to Firestore: ${e.localizedMessage}")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Error during Firestore write: ${e.localizedMessage}")
        }
    }

    // Remove single order document from Firestore
    fun deleteOrderFromFirestore(orderId: Long) {
        if (!isAvailable()) return
        try {
            val db = firestore ?: return
            db.collection("all_orders")
                .document(orderId.toString())
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Order #$orderId deleted from Firestore.")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Error deleting Firestore document: ${e.localizedMessage}")
        }
    }

    // Clear all simulated collection records
    fun clearAllOrdersFromFirestore() {
        if (!isAvailable()) return
        try {
            val db = firestore ?: return
            db.collection("all_orders")
                .get()
                .addOnSuccessListener { result ->
                    for (doc in result) {
                        doc.reference.delete()
                    }
                    Log.d(TAG, "Cleared all Firestore active live simulated orders.")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Error clearing Firestore collection: ${e.localizedMessage}")
        }
    }

    // Sync/Upload user profile information to Firestore
    fun syncUserProfile(profile: UserProfile) {
        if (!isAvailable()) return
        try {
            val db = firestore ?: return
            val userMap = mutableMapOf<String, Any>()
            userMap["name"] = profile.name
            userMap["phone"] = profile.phone
            userMap["email"] = profile.email
            userMap["role"] = profile.role
            profile.vehicleModel?.let { userMap["vehicleModel"] = it }
            profile.vehicleColor?.let { userMap["vehicleColor"] = it }
            profile.licensePlate?.let { userMap["licensePlate"] = it }
            profile.vehicleClass?.let { userMap["vehicleClass"] = it }

            db.collection("users")
                .document(profile.phone) // phone as unique key
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "User profile successfully synced to Firestore!")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Error syncing user profile: ${e.localizedMessage}")
        }
    }
}
