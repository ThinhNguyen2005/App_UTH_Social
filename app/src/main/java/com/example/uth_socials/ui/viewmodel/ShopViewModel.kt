package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.uth_socials.R
import com.example.uth_socials.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ShopViewModel : ViewModel() {
    private val _products = MutableStateFlow(sampleProducts)
    val products: StateFlow<List<Product>> = _products

    fun search(query: String) {
        //Filter (cơ bản)
        _products.value = sampleProducts.filter {
            it.name.contains(query, ignoreCase = true)
//            it.price.toString().contains(query, ignoreCase = true)
        }
    }

    companion object {
        private val sampleProducts = listOf(
            Product(1, "Giáo trình tư tưởng Hồ Chí Minh", 20.0, R.drawable.book06),
            Product(2, "Những kẻ xuất chúng", 26.0, R.drawable.book05),
            Product(3, "Giấc mơ hoá rồng", 29.0, R.drawable.book04),
            Product(4, "Từ vựng cuộc sống", 23.0, R.drawable.book03),
            Product(5, "Đắc nhân tâm", 50.0, R.drawable.dacnhantam),
        )
    }
}
