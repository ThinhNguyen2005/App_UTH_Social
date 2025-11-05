package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.repository.ProductRepository
import com.example.uth_socials.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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

class ProductViewModel(
//    private val repository: ProductRepository //Them repository vao constructor
) : ViewModel() {
    private val repository = ProductRepository()

    //------------------------------------------------------------------------------------------------------------------------------------------------
//    private val _products = MutableStateFlow<List<Product>>(emptyList())
//    val products = _products.asStateFlow()
//    private val _isLoading = MutableStateFlow(false)
//    val isLoading: StateFlow<Boolean> = _isLoading
//    private val _error = MutableStateFlow<String?>(null)
//    val error: StateFlow<String?> = _error
//    init {
//        loadProducts()
//    }
//    fun loadProducts() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                _products.value = repository.getAllProducts()
//                _error.value = null
//            } catch (e: Exception) {
//                _error.value = e.message
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
    //------------------------------------------------------------------------------------------------------------------------------------------------

    // --- 1. STATE CHO DANH SÁCH REALTIME ---
    val listUiState: StateFlow<ListUiState> = repository.getProductsStream()
        .map { productList ->
            ListUiState(products = productList, isLoading = false)
        }
        .catch { e ->
            emit(ListUiState(isLoading = false, error = e.message))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),    //nếu người dùng thoát app quá 5 giây thì tự ngắt kết nối realtime để tiết kiệm pin/băng thông.
            initialValue = ListUiState(isLoading = true) // Ban đầu là loading
        )
    // --- 2. STATE CHO CÁC TÁC VỤ (C, U, D) ---
    private val _operationState = MutableStateFlow<OperationUiState>(OperationUiState.Idle)
    val operationState = _operationState.asStateFlow()

    // --- 3. STATE CHO MÀN HÌNH CHI TIẾT (Get by ID) ---
    private val _detailState = MutableStateFlow(DetailUiState())
    val detailState = _detailState.asStateFlow()

    // === CÁC HÀM TÁC VỤ (gọi từ UI) ===
    /**
     * Lấy chi tiết một sản phẩm theo ID (cho màn hình Detail)
     */
//    fun getProductById(productId: String?) {
//        viewModelScope.launch {
//            _detailState.value = DetailUiState(isLoading = true)
//            try {
//                val product = repository.getProductById(productId)
//                _detailState.value = DetailUiState(product = product)
//            } catch (e: Exception) {
//                _detailState.value = DetailUiState(error = e.message)
//            }
//        }
//    }

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
//    fun deleteProduct(productId: String) {
//        viewModelScope.launch {
//            _operationState.value = OperationUiState.Loading
//            try {
//                repository.deleteProduct(productId)
//                _operationState.value = OperationUiState.Success("Xóa $productId thành công!")
//            } catch (e: Exception) {
//                _operationState.value = OperationUiState.Error(e.message ?: "Lỗi không xác định")
//            }
//        }
//    }

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

    //Dùng hàm này thay vì add/update trực tiếp từ UI
//    fun saveProduct(product: Product, imageUri: Uri?) {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                var productToSave = product
//
//                // 1. Nếu có ảnh mới (imageUri != null)
//                if (imageUri != null) {
//                    // 1a. Upload ảnh lên Storage và lấy URL
//                    val imageUrl = repository.uploadImage(imageUri)
//                    // 1b. Cập nhật product với URL mới
//                    productToSave = product.copy(imageUrl = imageUrl)
//                }
//
//                // 2. Lưu product vào Firestore
//                if (productToSave.id.isNullOrEmpty()) {
//                    //Thêm mới
//                    // val newId = repository.createProduct(productToSave)
//                    val newProduct = productToSave.copy(id = UUID.randomUUID().toString())
//                    repository.createProduct(newProduct)
//                } else {
//                    //Cập nhật
//                    repository.updateProduct(productToSave)
//                }
//
//                // 3. Tải lại danh sách
//                loadProducts()
//
//            } catch (e: Exception) {
//                _error.value = "Lỗi khi lưu sản phẩm: ${e.message}"
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
}
