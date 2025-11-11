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

// Trạng thái cho màn hình danh sách chính (List)
data class ListUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(), // Danh sách sau khi search
    val searchQuery: String = "",
    val error: String? = null
)

// Trạng thái cho màn hình chi tiết (Detail)
data class DetailUiState(
    val isLoading: Boolean = false,
    val product: Product? = null,
    val error: String? = null
)

// Trạng thái cho các tác vụ (Create, Update, Delete)
sealed class OperationUiState {
    object Idle : OperationUiState()
    object Loading : OperationUiState()
    data class Success(val message: String) : OperationUiState()
    data class Error(val message: String) : OperationUiState()
}

private const val TAG = "ProductViewModel"  //Phân biệt log của lớp này với các lớp khác trong Logcat khi debug ứng dụng.

class ProductViewModel2(
) : ViewModel() {
    private val repository = ProductRepository()

    // MutableStateFlow để lưu query tìm kiếm
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // --- 1. STATE CHO DANH SÁCH REALTIME với SEARCH ---
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

    // --- 2. STATE CHO CÁC TÁC VỤ (C, U, D) ---
    private val _operationState = MutableStateFlow<OperationUiState>(OperationUiState.Idle)
    val operationState = _operationState.asStateFlow()

    // --- 3. STATE CHO MÀN HÌNH CHI TIẾT (Get by ID) ---
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


    /**
     * Thêm sản phẩm mới
     */
    fun addProduct(product: Product) {
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading
            try {
                val newId = repository.createProduct(product)
                _operationState.value = OperationUiState.Success("Tạo sản phẩm $newId thành công!")
            } catch (e: Exception) {
                _operationState.value = OperationUiState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    /**
     * Cập nhật sản phẩm
     */
    fun updateProduct(product: Product) {
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading
            try {
                repository.updateProduct(product)
                _operationState.value = OperationUiState.Success("Cập nhật ${product.id} thành công!")
            } catch (e: Exception) {
                _operationState.value = OperationUiState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    /**
     * Xóa sản phẩm
     */
    fun deleteProduct(productId: String?) {
        if (productId.isNullOrBlank()) {
            _operationState.value = OperationUiState.Error("Product ID không hợp lệ")
            return
        }
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading
            try {
                repository.deleteProduct(productId)
                _operationState.value = OperationUiState.Success("Xóa sản phẩm $productId thành công!")
            } catch (e: Exception) {
                _operationState.value = OperationUiState.Error(e.message ?: "Lỗi khi xóa sản phẩm")
            }
        }
    }

    /**
     * Reset trạng thái tác vụ (gọi sau khi UI đã hiển thị Success/Error)
     */
    fun resetOperationState() {
        _operationState.value = OperationUiState.Idle
    }
}