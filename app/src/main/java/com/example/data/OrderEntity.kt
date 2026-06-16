package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoffAddress: String,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val status: String, // PENDING, ACCEPTED, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED
    val carType: String, // Economy, Comfort, Business
    val fare: Double,
    val estimatedTimeMinutes: Int,
    val driverId: String? = null,
    val driverLat: Double = 0.0,
    val driverLng: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
