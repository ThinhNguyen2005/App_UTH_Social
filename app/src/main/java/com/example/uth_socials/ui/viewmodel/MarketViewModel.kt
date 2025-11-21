package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.repository.MarketRepository
import com.example.uth_socials.data.market.Product
import com.example.uth_socials.data.repository.ChatRepository
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MarketViewModel:
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
    val seller: User? = null,
    val error: String? = null
)

private const val TAG = "MarketViewModel"

class MarketViewModel: ViewModel() {
    private val repository = MarketRepository()
    private val userRepository = UserRepository()
    private val chatRepository = ChatRepository()

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
     * Lấy chi tiết sản phẩm VÀ thông tin người bán theo ID
     *
     * Luồng hoạt động:
     * 1. Kiểm tra productId hợp lệ
     * 2. Set state loading = true
     * 3. Load Product từ ProductRepository
     * 4. Nếu Product có userId -> Load User từ UserRepository
     * 5. Cập nhật state với Product + Seller
     *
     * @param productId ID của sản phẩm cần load
     */

    init {
        // Log khi ViewModel được khởi tạo
        Log.d(TAG, "MarketViewModel initialized")

        // Theo dõi state changes
        viewModelScope.launch {
            listUiState.collect { state ->
                Log.d(TAG, """
                State Update:
                - Loading: ${state.isLoading}
                - Products: ${state.products.size}
                - Filtered: ${state.filteredProducts.size}
                - Query: "${state.searchQuery}"
                - Error: ${state.error}
            """.trimIndent())
            }
        }
    }
    fun getProductById(productId: String?) {
        //Validate productId
        if (productId.isNullOrBlank()) {
            _detailState.value = DetailUiState(
                isLoading = false,
                error = "Product ID không hợp lệ"
            )
            Log.e(TAG, "ProductId is null or blank")
            return
        }

        //Bắt đầu loading
        _detailState.value = DetailUiState(isLoading = true)
        Log.d(TAG, "Loading product with ID: $productId")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                //Load Product (async để có thể cancel nếu cần)
                val productDeferred = async {
                    repository.getProductById(productId)
                }
                val product = productDeferred.await()

                //Kiểm tra Product có tồn tại không
                if (product == null) {
                    withContext(Dispatchers.Main) {
                        _detailState.value = DetailUiState(
                            isLoading = false,
                            error = "Không tìm thấy sản phẩm"
                        )
                    }
                    Log.e(TAG, "Product not found with ID: $productId")
                    return@launch
                }

                Log.d(TAG, "Product loaded: ${product.name}, userId: ${product.userId}")

                //Load Seller nếu có userId
                val seller = if (!product.userId.isNullOrEmpty()) {
                    Log.d(TAG, "Loading seller with userId: ${product.userId}")

                    val sellerDeferred = async {
                        userRepository.getUser(product.userId)
                    }
                    val loadedSeller = sellerDeferred.await()

                    if (loadedSeller != null) {
                        Log.d(TAG, "Seller loaded: ${loadedSeller.username}")
                    } else {
                        Log.w(TAG, "Seller not found with userId: ${product.userId}")
                    }

                    loadedSeller
                } else {
                    Log.w(TAG, "Product has no userId")
                    null
                }

                //Cập nhật state với cả Product và Seller
                withContext(Dispatchers.Main) {
                    _detailState.value = DetailUiState(
                        isLoading = false,
                        product = product,
                        seller = seller,
                        error = null
                    )
                }

                Log.d(TAG, "DetailState updated successfully")

            } catch (e: Exception) {
                //Xử lý lỗi
                Log.e(TAG, "Error loading product detail for ID: $productId", e)

                withContext(Dispatchers.Main) {
                    _detailState.value = DetailUiState(
                        isLoading = false,
                        product = null,
                        seller = null,
                        error = "Có lỗi xảy ra: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Mở chat với người bán sản phẩm
     *
     * @param sellerId ID của người bán
     * @param onChatReady Callback trả về chatId để navigate
     */
    fun openChatWithSeller(sellerId: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUserId = userRepository.getCurrentUserId()

                if (currentUserId == null) {
                    Log.w("MarketViewModel", "User chưa đăng nhập")
                    // Có thể hiển thị thông báo yêu cầu đăng nhập
                    return@launch
                }

                if (currentUserId == sellerId) {
                    Log.w("MarketViewModel", "Không thể nhắn tin với chính mình")
                    return@launch
                }

                // Kiểm tra chat đã tồn tại chưa
                val existingChatId = chatRepository.getExistingChatId(sellerId)

                if (existingChatId != null) {
                    // Chat đã tồn tại -> mở ngay
                    onChatReady(existingChatId)
                } else {
                    // Chat chưa tồn tại -> tạo chatId mới
                    val newChatId = chatRepository.buildChatId(currentUserId, sellerId)
                    onChatReady(newChatId)
                }
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Lỗi khi mở chat", e)
            }
        }
    }

    /**
     *Refresh lại data (dùng cho pull-to-refresh)
     */
    fun refreshProduct(productId: String?) {
        Log.d(TAG, "Refreshing product: $productId")
        getProductById(productId)
    }

    /**
     *Clear detail state (dùng khi rời khỏi màn hình)
     */
    fun clearDetailState() {
        _detailState.value = DetailUiState()
        Log.d(TAG, "Detail state cleared")
    }
}