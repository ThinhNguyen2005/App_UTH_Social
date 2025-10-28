package com.example.uth_socials.data.model

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val imageRes: Int? = null,
    val isFavorite: Boolean = false,
    val description: String? = null
)
