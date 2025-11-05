package com.example.uth_socials.data.repository

import android.net.Uri
import android.util.Log
import com.example.uth_socials.data.model.Product
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val TAG = "ProductRepository" //Phan biet cac lop khi debug

/***
 * ProductRepository - chịu trách nhiệm tương tác trực tiếp với Firestore.
 *
 * - getProductsRealtime(): phát ra Flow<List<Product>> và lắng nghe realtime changes.
 * - getProductById(): suspend function lấy 1 product theo id (one-shot).
 * - add/update/delete: suspend functions để thao tác CRUD.
 *
 * Tất cả các thao tác Firestore dùng `.await()` (kotlinx-coroutines-play-services)
 * để chạy trong coroutine thay vì callback.
 ***/

class ProductRepository {
    //Khởi tạo kết nối đến cơ sở dữ liệu Cloud Firestore mặc định và gán nó vào một biến tên là db.
    private val db = FirebaseFirestore.getInstance()
    private val storage = Firebase.storage

    //Tạo một "tham chiếu" đến collection có tên là "products" trong cơ sở dữ liệu.
    private val productCollection = db.collection("products")

    /**
     * (REALTIME) Lấy toàn bộ sản phẩm và lắng nghe thay đổi.
     * Trả về một Flow, sẽ phát ra (emit) danh sách mới mỗi khi có thay đổi.
     */
    fun getProductsStream(): Flow<List<Product>> {
        return productCollection
            .orderBy("timestamp", Query.Direction.DESCENDING) // Sắp xếp
            .snapshots() //Tạo listener realtime
            .map { querySnapshot ->
                querySnapshot.toObjects(Product::class.java) //Map QuerySnapshot sang List<Product>
            }
            .catch { exception ->
                //Xử lý lỗi nếu listener thất bại(Mất mạng/ Không có quyền truy cập), tránh crash app.
                Log.e("ProductRepository", "Lỗi khi lắng nghe products: ", exception)
                emit(emptyList()) // Emit list rỗng khi lỗi để UI không bị treo loading mãi mãi
            }
    }

    //(ONE-SHOT) Lấy toàn bộ sản phẩm 1 lần.
//    suspend fun getAllProducts(): List<Product> {
//        return try {
//            productCollection
//                .orderBy("timestamp", Query.Direction.DESCENDING)
//                .get().await().toObjects(Product::class.java)
//        } catch (e: Exception) {
//            Log.e("ProductRepository", "Lỗi khi lấy tất cả products (one-shot): ", e)
//            emptyList()
//        }
//    }

    suspend fun getProductById(productId: String): Product? {
        return try {
            val document = productCollection.document(productId).get().await()
            document.toObject(Product::class.java)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Lỗi khi lấy product by ID: $productId", e)
            null
        }
    }

    /**
     * Thêm một sản phẩm mới vào Firestore và trả về ID của nó.
     * Ném ra (throws) một Exception nếu có lỗi xảy ra.
     */
    suspend fun createProduct(product: Product): String {
        // .await() sẽ tự động ném ra Exception nếu Task thất bại.
        val documentRef = productCollection.add(product).await()

        // Trả về ID của tài liệu mới được tạo
        return documentRef.id
    }

    /**
     * (ONE-SHOT) Cập nhật một sản phẩm đã có.
     * Cần Product có chứa 'id'
     */
    suspend fun updateProduct(product: Product) {
        if (product.id == null) {
            throw IllegalArgumentException("Product ID không được null khi cập nhật")
        }
        // Dùng .set() để ghi đè toàn bộ đối tượng
        productCollection.document(product.id).set(product).await()
    }

    /**
     * (ONE-SHOT) Xóa một sản phẩm theo ID.
     */
    suspend fun deleteProduct(productId: String) {
        productCollection.document(productId).delete().await()
    }

    //Upload ảnh và trả về URL
    suspend fun uploadImage(imageUri: Uri): String {
        // Đặt tên file theo timestamp để tránh trùng lặp và dễ quản lý hơn UUID đơn thuần
        val fileName = "product_images/${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)

        // Upload file và đợi (dùng .await() từ thư viện play-services)
        storageRef.putFile(imageUri).await()

        // Lấy URL tải về và đợi
        return storageRef.downloadUrl.await().toString()
    }
}