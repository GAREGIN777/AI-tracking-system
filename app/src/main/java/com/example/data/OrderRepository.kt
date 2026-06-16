package com.example.data

import kotlinx.coroutines.flow.Flow

class OrderRepository(private val orderDao: OrderDao) {
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()
    val pendingOrders: Flow<List<OrderEntity>> = orderDao.getPendingOrders()

    fun getOrderById(id: Long): Flow<OrderEntity?> = orderDao.getOrderById(id)

    suspend fun insertOrder(order: OrderEntity): Long {
        return orderDao.insertOrder(order)
    }

    suspend fun updateOrder(order: OrderEntity) {
        orderDao.updateOrder(order)
    }

    suspend fun deleteOrder(order: OrderEntity) {
        orderDao.deleteOrder(order)
    }

    suspend fun clearAll() {
        orderDao.deleteAllOrders()
    }
}
