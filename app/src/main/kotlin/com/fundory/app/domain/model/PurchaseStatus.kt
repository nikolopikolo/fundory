package com.fundory.app.domain.model

enum class PurchaseStatus(val displayName: String) {
    NOT_PURCHASED("Not Purchased"),
    ORDERED("Ordered"),
    SHIPPED("Shipped"),
    ARRIVED_AT_WAREHOUSE("Arrived at Warehouse"),
    IN_TRANSIT("In Transit"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled")
}

