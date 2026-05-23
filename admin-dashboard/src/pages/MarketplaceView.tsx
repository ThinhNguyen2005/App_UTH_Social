import React, { useEffect, useState } from "react";
import { db, auth } from "../firebase/config";
import { collection, onSnapshot, addDoc } from "firebase/firestore";
import { Phone, User, Plus, X, Image, Tag, DollarSign } from "lucide-react";

interface ProductItem {
  id: string;
  name: string;
  description: string;
  price: number;
  imageUrl?: string;
  sellerId?: string;
  sellerName?: string;
  sellerPhone?: string;
  category?: string;
}

interface MarketplaceViewProps {
  onViewProfile?: (userId: string) => void;
}

export const MarketplaceView: React.FC<MarketplaceViewProps> = ({ onViewProfile }) => {
  const [products, setProducts] = useState<ProductItem[]>([]);
  const [loading, setLoading] = useState(true);
  
  // Compose product modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newName, setNewName] = useState("");
  const [newPrice, setNewPrice] = useState("");
  const [newCategory, setNewCategory] = useState("Sách");
  const [newDescription, setNewDescription] = useState("");
  const [newImageUrl, setNewImageUrl] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const currentUser = auth.currentUser;

  // Format currency
  const formatVND = (value: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND"
    }).format(value);
  };

  useEffect(() => {
    setLoading(true);
    
    const unsubscribe = onSnapshot(
      collection(db, "products"),
      (snapshot) => {
        const prodList: ProductItem[] = [];
        snapshot.forEach((doc) => {
          const data = doc.data();
          prodList.push({
            id: doc.id,
            name: data.name || "Sản phẩm sinh viên",
            description: data.description || "Không có mô tả sản phẩm.",
            price: data.price || 0,
            imageUrl: (data.imageUrls && Array.isArray(data.imageUrls) && data.imageUrls.length > 0)
              ? data.imageUrls[0]
              : (data.imageUrl || ""),
            sellerId: data.userId || "",
            sellerName: data.userName || data.sellerName || "Sinh viên UTH",
            sellerPhone: data.sellerPhone || "Chưa cập nhật",
            category: data.type || data.category || "Học tập"
          });
        });
        setProducts(prodList);
        setLoading(false);
      },
      (error) => {
        console.warn("Firestore products query restricted. Loading mockup items:", error);
        const mockProducts: ProductItem[] = [
          {
            id: "prod-01",
            name: "Sách giáo trình Giải tích 1 & 2 UTH",
            description: "Giáo trình còn mới 95%, không vẽ bậy, thích hợp cho các bạn sinh viên năm nhất IT/Điện tử.",
            price: 50000,
            imageUrl: "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=400&q=80",
            sellerId: "vvrTdGbamOPz8wEkSV2kwgMJeG43",
            sellerName: "Nguyễn Thịnh",
            sellerPhone: "0912345678",
            category: "Sách"
          },
          {
            id: "prod-02",
            name: "Bàn phím cơ Dareu EK87 cũ",
            description: "Mọi phím hoạt động hoàn hảo, switch Blue bấm giòn tai. Giá sinh viên để gom tiền lên đời.",
            price: 250000,
            imageUrl: "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=400&q=80",
            sellerId: "jUdmXzgPTSWTw4CH0J7oChxuf362",
            sellerName: "Trọng Nghĩa",
            sellerPhone: "0987654321",
            category: "Đồ điện tử"
          }
        ];
        setProducts(mockProducts);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, []);

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentUser) {
      alert("Bạn cần đăng nhập để đăng bán sản phẩm!");
      return;
    }

    setErrorMessage("");

    // Enforce that image URL is provided
    if (!newImageUrl.trim()) {
      setErrorMessage("Bạn phải cung cấp hình ảnh sản phẩm để đăng bán!");
      return;
    }

    setIsSubmitting(true);
    try {
      await addDoc(collection(db, "products"), {
        name: newName.trim(),
        price: Number(newPrice) || 0,
        type: newCategory,
        description: newDescription.trim(),
        imageUrls: [newImageUrl.trim()],
        userId: currentUser.uid,
        userName: currentUser.displayName || currentUser.email?.split("@")[0] || "Sinh viên UTH",
        userAvatar: currentUser.photoURL || "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da",
        timestamp: new Date(),
        saves: 0,
        shares: 0,
        likedBy: [],
        savedBy: [],
        campus: null
      });

      // Clear states and close
      setNewName("");
      setNewPrice("");
      setNewCategory("Sách");
      setNewDescription("");
      setNewImageUrl("");
      setIsModalOpen(false);
      alert("Đăng bán sản phẩm thành công!");
    } catch (err) {
      console.error("Error creating product:", err);
      setErrorMessage("Không thể tạo sản phẩm. Vui lòng kiểm tra lại kết nối hoặc quyền truy cập.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 text-left animate-fadeIn">
      {/* Page Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold font-display text-[var(--text-primary)]">Chợ Sinh Viên UTH</h2>
          <p className="text-xs text-[var(--text-secondary)] mt-1">Kênh trao đổi mua bán tài liệu học tập, đồ dùng cũ tiện lợi trong trường</p>
        </div>
        
        {currentUser && (
          <button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-1.5 px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-bold transition shadow-sm"
          >
            <Plus size={16} />
            Đăng bán
          </button>
        )}
      </div>

      {/* Products Grid */}
      {loading ? (
        <div className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl py-12 text-center text-[var(--text-secondary)] text-sm shadow-sm">
          <span className="inline-block w-6 h-6 border-2 border-[var(--border-primary)] border-t-sky-500 rounded-full animate-spin mr-2" />
          Đang tải danh sách sản phẩm...
        </div>
      ) : products.length === 0 ? (
        <div className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl py-12 text-center text-[var(--text-secondary)] text-sm shadow-sm">
          Chưa có sản phẩm nào được đăng bán.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {products.map((product) => (
            <div 
              key={product.id}
              className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl overflow-hidden shadow-sm hover:border-slate-300 dark:hover:border-slate-800 transition duration-300 flex flex-col justify-between"
            >
              {/* Product Image */}
              <div className="relative h-44 bg-[var(--bg-inset)] overflow-hidden border-b border-[var(--border-primary)]">
                <img 
                  src={product.imageUrl || "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80"}
                  alt={product.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition duration-300"
                  onError={(e) => {
                    e.currentTarget.src = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80";
                  }}
                />
                
                {/* Category Badge */}
                {product.category && (
                  <span className="absolute top-3 left-3 px-2 py-0.5 rounded text-[8px] font-bold bg-[var(--bg-card)]/80 text-sky-600 border border-[var(--border-primary)] font-mono uppercase tracking-wider">
                    {product.category}
                  </span>
                )}
              </div>

              {/* Product Info */}
              <div className="p-4 flex-1 flex flex-col justify-between space-y-4">
                <div className="space-y-1">
                  <h4 className="text-sm font-semibold text-[var(--text-primary)] line-clamp-1 hover:text-sky-600 transition">
                    {product.name}
                  </h4>
                  <p className="text-xs text-[var(--text-secondary)] line-clamp-2 leading-relaxed">
                    {product.description}
                  </p>
                </div>

                {/* Price & Seller */}
                <div className="space-y-3 pt-2 border-t border-[var(--border-secondary)]">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-[var(--text-muted)] font-semibold uppercase tracking-wider font-mono">Giá bán</span>
                    <span className="text-sm font-bold text-sky-600 font-mono">
                      {formatVND(product.price)}
                    </span>
                  </div>

                  <div className="space-y-1 bg-[var(--bg-inset)] p-2.5 rounded-xl border border-[var(--border-primary)] text-[10px] text-[var(--text-secondary)]">
                    <div className="flex items-center gap-1.5">
                      <User size={12} className="text-[var(--text-muted)]" />
                      <span className="truncate">
                        Người bán:{" "}
                        <span 
                          onClick={() => onViewProfile && product.sellerId && onViewProfile(product.sellerId)}
                          className={`font-semibold text-[var(--text-primary)] ${onViewProfile && product.sellerId ? "hover:underline cursor-pointer" : ""}`}
                        >
                          {product.sellerName}
                        </span>
                      </span>
                    </div>
                    {product.sellerPhone && (
                      <div className="flex items-center gap-1.5 mt-1">
                        <Phone size={12} className="text-[var(--text-muted)]" />
                        <span className="font-mono">SĐT: {product.sellerPhone}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Compose Product Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-lg bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
            
            {/* Modal Header */}
            <div className="p-4 border-b border-[var(--border-secondary)] flex justify-between items-center bg-[var(--bg-inset)]/10">
              <h3 className="font-bold text-sm text-[var(--text-primary)] flex items-center gap-2">
                <Plus size={16} className="text-sky-600" />
                Đăng Sản Phẩm Mới
              </h3>
              <button 
                onClick={() => setIsModalOpen(false)}
                className="p-1 hover:bg-[var(--bg-inset)] rounded-lg text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition"
              >
                <X size={16} />
              </button>
            </div>

            {/* Error Message */}
            {errorMessage && (
              <div className="p-3 mx-4 mt-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-500 text-xs font-semibold leading-relaxed">
                {errorMessage}
              </div>
            )}

            {/* Form */}
            <form onSubmit={handleCreateProduct} className="p-4 space-y-4 overflow-y-auto flex-1 text-xs">
              
              {/* Product Name */}
              <div className="space-y-1.5">
                <label className="block font-semibold text-[var(--text-secondary)] uppercase tracking-wider font-mono">Tên sản phẩm *</label>
                <input
                  type="text"
                  required
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="Ví dụ: Giáo trình Giải tích UTH cũ..."
                  className="w-full px-3.5 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl text-xs text-[var(--text-primary)] placeholder-[var(--text-muted)] outline-none focus:border-sky-500 transition"
                />
              </div>

              {/* Price & Category */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="block font-semibold text-[var(--text-secondary)] uppercase tracking-wider font-mono">Giá bán (VND) *</label>
                  <div className="relative">
                    <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" size={14} />
                    <input
                      type="number"
                      required
                      value={newPrice}
                      onChange={(e) => setNewPrice(e.target.value)}
                      placeholder="15000"
                      className="w-full pl-9 pr-3 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl text-xs text-[var(--text-primary)] placeholder-[var(--text-muted)] outline-none focus:border-sky-500 transition"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="block font-semibold text-[var(--text-secondary)] uppercase tracking-wider font-mono">Danh mục *</label>
                  <div className="relative">
                    <Tag className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" size={14} />
                    <select
                      value={newCategory}
                      onChange={(e) => setNewCategory(e.target.value)}
                      className="w-full pl-9 pr-3 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl text-xs text-[var(--text-primary)] outline-none focus:border-sky-500 transition appearance-none"
                    >
                      <option value="Sách">Sách & Giáo trình</option>
                      <option value="Đồ điện tử">Đồ điện tử</option>
                      <option value="Quần áo">Quần áo</option>
                      <option value="Khác">Khác</option>
                    </select>
                  </div>
                </div>
              </div>

              {/* Image URL */}
              <div className="space-y-1.5">
                <label className="block font-semibold text-[var(--text-secondary)] uppercase tracking-wider font-mono">Đường dẫn ảnh sản phẩm * (Bắt buộc)</label>
                <div className="relative">
                  <Image className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" size={14} />
                  <input
                    type="url"
                    required
                    value={newImageUrl}
                    onChange={(e) => setNewImageUrl(e.target.value)}
                    placeholder="https://firebasestorage.googleapis.com/... hoặc link ảnh bất kỳ"
                    className="w-full pl-9 pr-3 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl text-xs text-[var(--text-primary)] placeholder-[var(--text-muted)] outline-none focus:border-sky-500 transition"
                  />
                </div>
              </div>

              {/* Description */}
              <div className="space-y-1.5">
                <label className="block font-semibold text-[var(--text-secondary)] uppercase tracking-wider font-mono">Mô tả chi tiết *</label>
                <textarea
                  required
                  rows={4}
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  placeholder="Mô tả tình trạng sản phẩm, địa điểm giao dịch trong campus trường..."
                  className="w-full px-3.5 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl text-xs text-[var(--text-primary)] placeholder-[var(--text-muted)] outline-none focus:border-sky-500 transition resize-none"
                />
              </div>

              {/* Submit Buttons */}
              <div className="flex justify-end gap-3 pt-3 border-t border-[var(--border-secondary)]">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 bg-[var(--bg-inset)] hover:bg-[var(--border-secondary)] text-[var(--text-secondary)] font-semibold rounded-xl transition"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-5 py-2 bg-sky-600 hover:bg-sky-500 text-white font-bold rounded-xl transition disabled:opacity-50"
                >
                  {isSubmitting ? "Đang xử lý..." : "Đăng bán"}
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

    </div>
  );
};
