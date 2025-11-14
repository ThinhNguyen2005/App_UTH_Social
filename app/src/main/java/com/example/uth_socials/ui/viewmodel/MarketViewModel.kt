package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.repository.ProductRepository
import com.example.uth_socials.data.shop.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ProductViewModel:
 * - products: StateFlow để UI collect
 * - init: subscribe realtime (hoặc bạn có thể gọi loadOnce() để lấy one-shot)
 * - add/update/delete: chạy trong viewModelScope (coroutines)
 */

//State cho màn hình danh sách chính
data class ListUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(), // Danh sách sau khi search
    val searchQuery: String = "",
    val error: String? = null
)

//State cho màn hình chi tiết
data class DetailUiState(
    val isLoading: Boolean = false,
    val product: Product? = null,
    val error: String? = null
)

private const val TAG = "ProductViewModel"

class ProductViewModel2: ViewModel() {
    private val repository = ProductRepository()

    //MutableStateFlow để lưu query tìm kiếm
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //STATE CHO DANH SÁCH REALTIME với SEARCH ---
    val listUiState: StateFlow<ListUiState> = combine(
        repository.getProductsStream(),
        _searchQuery
    ) { productList, query ->
        val filtered = if (query.isBlank()) {
            productList
        } else {
            productList.filter { product ->
                // Tìm kiếm theo tên
                val matchesName = product.name.contains(query, ignoreCase = true)

                // Tìm kiếm theo giá (nếu query là số)
                val matchesPrice = try {
                    val searchPrice = query.toDoubleOrNull()
                    if (searchPrice != null) {
                        // Tìm sản phẩm có giá gần đúng (± 10%)
                        val lowerBound = searchPrice * 0.9
                        val upperBound = searchPrice * 1.1
                        product.price in lowerBound..upperBound
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }

                matchesName || matchesPrice
            }
        }

        ListUiState(
            products = productList,
            filteredProducts = filtered,
            searchQuery = query,
            isLoading = false
        )
    }
        .catch { e ->
            emit(ListUiState(isLoading = false, error = e.message))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ListUiState(isLoading = true)
        )

    //STATE CHO MÀN HÌNH CHI TIẾT (Get by ID) ---
    private val _detailState = MutableStateFlow(DetailUiState())
    val detailState = _detailState.asStateFlow()

    // === HÀM TÌM KIẾM ===
    /**
     * Cập nhật query tìm kiếm
     * Hỗ trợ tìm theo:
     * - Tên sản phẩm (text)
     * - Giá sản phẩm (số)
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Xóa query tìm kiếm
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    // === CÁC HÀM TÁC VỤ (gọi từ UI) ===
    /**
     * Lấy chi tiết một sản phẩm theo ID (cho màn hình Detail)
     */
    fun getProductById(productId: String?) {
        if (productId.isNullOrBlank()) {
            _detailState.value = DetailUiState(error = "Product id không hợp lệ")
            return
        }
        viewModelScope.launch {
            try {
                val product = repository.getProductById(productId) // non-null
                _detailState.value = DetailUiState(product = product)
            } catch (e: Exception) {
                _detailState.value = DetailUiState(error = e.message)
            }
        }
    }
}