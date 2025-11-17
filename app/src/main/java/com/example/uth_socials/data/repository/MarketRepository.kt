package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.market.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.collections.mapNotNull
import kotlin.jvm.java

private const val TAG = "MarketRepository"

/**
 * ProductRepository - chịu trách nhiệm tương tác trực tiếp với Firestore.
 * - getProductsRealtime(): phát ra Flow<List<Product>> và lắng nghe realtime changes.
 * - getProductById(): suspend function lấy 1 product theo id (one-shot).
 */

class ProductRepository {
    //Khởi tạo kết nối đến cơ sở dữ liệu Cloud Firestore mặc định và gán nó vào một biến tên là db.
    private val db = FirebaseFirestore.getInstance()
    //Tạo một "tham chiếu" đến collection có tên là "products" trong cơ sở dữ liệu.
    private val productCollection = db.collection("products")

    /**
     * (REALTIME) Lấy toàn bộ sản phẩm và lắng nghe thay đổi.
     * Trả về một Flow, sẽ phát ra (emit) danh sách mới mỗi khi có thay đổi.
     */
    fun getProductsStream(): Flow<List<Product>> = callbackFlow {
        Log.d(TAG, "Đang thiết lập listener...")

        val listener = productCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Lỗi snapshot: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w(TAG, "⚠️ Snapshot null")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                Log.d(TAG, "Nhận được ${snapshot.size()} documents")

                val products = snapshot.documents.mapNotNull { doc ->
                    try {
                        val product = doc.toObject(Product::class.java)
                        product?.copy(id = doc.id)?.also {
                            Log.d(TAG, "Parsed: ${it.name} - ${it.price}đ")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi parse document ${doc.id}: ${e.message}")
                        null
                    }
                }

                Log.d(TAG, "Tổng ${products.size} products hợp lệ")
                trySend(products)
            }

        awaitClose {
            Log.d(TAG, "Đóng listener")
            listener.remove()
        }
    }.catch { e ->
        Log.e(TAG, "Flow error: ${e.message}", e)
        emit(emptyList())
    }
    //(ONE-SHOT) Lấy chi tiết 1 sản phẩm theo ID, Sử dụng cho ProductDetailScreen.
    suspend fun getProductById(productId: String): Product? {
        return try {
            Log.d(TAG, "Loading product: $productId")
            val doc = productCollection.document(productId).get().await()

            if (!doc.exists()) {
                Log.w(TAG, "Document không tồn tại: $productId")
                return null
            }

            doc.toObject(Product::class.java)?.copy(id = doc.id)?.also {
                Log.d(TAG, "Loaded: ${it.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi getProductById: ${e.message}", e)
            null
        }
    }
}