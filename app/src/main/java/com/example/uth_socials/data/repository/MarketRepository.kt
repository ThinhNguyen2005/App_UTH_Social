package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.market.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
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
    fun getProductsStream(): Flow<List<Product>> {
        return productCollection
            .orderBy("createdAt", Query.Direction.DESCENDING) // Sắp xếp
            .snapshots() //Tạo listener realtime
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
            }
            .catch { exception ->
                //Xử lý lỗi nếu listener thất bại(Mất mạng/ Không có quyền truy cập), tránh crash app.
                Log.e(TAG, "Lỗi khi lắng nghe products: ", exception)
                emit(emptyList()) // Emit list rỗng khi lỗi để UI không bị treo loading mãi mãi
            }
    }
    //(ONE-SHOT) Lấy chi tiết 1 sản phẩm theo ID, Sử dụng cho ProductDetailScreen.
    suspend fun getProductById(productId: String): Product? {
        return try {
            val document = productCollection.document(productId).get().await()
            document.toObject(Product::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy product by ID: $productId", e)
            null
        }
    }
}