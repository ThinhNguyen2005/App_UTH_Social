import React, { useEffect, useState } from "react";
import { db } from "../firebase/config";
import { 
  collection, 
  onSnapshot, 
  doc, 
  setDoc, 
  deleteDoc 
} from "firebase/firestore";
import { 
  Plus, 
  Trash2, 
  FolderPlus, 
  AlertCircle, 
  Layers, 
  ArrowUpDown 
} from "lucide-react";

interface CategoryItem {
  id: string;
  name: string;
  order: number;
}

export const CategoriesView: React.FC = () => {
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [newName, setNewName] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setLoading(true);
    // Real-time listener for categories collection
    const unsubscribe = onSnapshot(
      collection(db, "categories"),
      (snapshot) => {
        const list: CategoryItem[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          list.push({
            id: docSnap.id,
            name: data.name || "",
            order: typeof data.order === "number" ? data.order : 0
          });
        });
        
        // Sort by order ascending
        list.sort((a, b) => a.order - b.order);
        setCategories(list);
        setLoading(false);
      },
      () => {
        console.warn("Firestore categories listen restricted. Using app defaults.");
        const defaultList: CategoryItem[] = [
          { id: "all", name: "Tất cả", order: 0 },
          { id: "latest", name: "Mới nhất", order: 1 },
          { id: "study", name: "Học tập", order: 2 },
          { id: "social", name: "Xã hội", order: 3 },
          { id: "entertainment", name: "Giải trí", order: 4 },
          { id: "technology", name: "Công nghệ", order: 5 }
        ];
        setCategories(defaultList);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, []);

  // Simple slugify helper to turn "Giải trí" into "entertainment" or "giai-tri"
  const slugify = (text: string) => {
    return text
      .toString()
      .toLowerCase()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "") // remove accents
      .replace(/[đĐ]/g, "d")
      .replace(/[^a-z0-9 -]/g, "") // remove invalid chars
      .replace(/\s+/g, "-") // collapse whitespace and replace by -
      .replace(/-+/g, "-") // collapse dashes
      .trim();
  };

  const handleAddCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || submitting) return;

    setSubmitting(true);
    const id = slugify(newName);
    if (!id) {
      alert("Tên danh mục không hợp lệ để tạo mã định danh (slug)");
      setSubmitting(false);
      return;
    }

    // Check if duplicate ID exists
    if (categories.some((c) => c.id === id)) {
      alert("Danh mục này đã tồn tại (hoặc trùng mã định danh)");
      setSubmitting(false);
      return;
    }

    const nextOrder = categories.length > 0 
      ? Math.max(...categories.map(c => c.order)) + 1 
      : 1;

    try {
      const catRef = doc(db, "categories", id);
      await setDoc(catRef, {
        id,
        name: newName.trim(),
        order: nextOrder
      });

      // Update local state if firebase rules restrict/delay
      setCategories(prev => [...prev, { id, name: newName.trim(), order: nextOrder }].sort((a, b) => a.order - b.order));
      setNewName("");
    } catch (e) {
      console.error("Error writing category:", e);
      alert("Không thể lưu danh mục. Vui lòng kiểm tra Firestore Security Rules.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteCategory = async (id: string) => {
    // Avoid deleting core app navigation categories to prevent app crashes
    const reservedIds = ["all", "latest", "study", "social"];
    if (reservedIds.includes(id)) {
      alert(`Không thể xóa danh mục hệ thống "${id}". Đây là các danh mục thiết yếu của ứng dụng di động.`);
      return;
    }

    if (!window.confirm("Bạn có chắc chắn muốn xóa danh mục này? Các bài viết thuộc danh mục này có thể cần được phân loại lại.")) {
      return;
    }

    setActionId(id);
    try {
      await deleteDoc(doc(db, "categories", id));
      // Update local state for demo mode
      setCategories(prev => prev.filter(c => c.id !== id));
    } catch (e) {
      console.error("Error deleting category:", e);
      alert("Không thể xóa danh mục. Vui lòng kiểm tra Firestore Security Rules.");
    } finally {
      setActionId(null);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn text-left">
      {/* Header */}
      <div>
        <h2 className="text-3xl font-display text-slate-100 tracking-wide">
          Quản Lý Danh Mục Bài Viết
        </h2>
        <p className="text-sm text-slate-400 mt-1">Cấu hình các chủ đề/phân loại bài đăng hỗ trợ bộ lọc trên ứng dụng di động</p>
      </div>

      {/* Main Container Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* 1. Add Category Card */}
        <div className="bg-[#0d1321]/40 border border-slate-900 rounded-2xl p-6 h-fit">
          <h3 className="text-md font-semibold text-slate-200 mb-4 flex items-center gap-2">
            <FolderPlus size={18} className="text-sky-400" />
            Thêm danh mục mới
          </h3>
          
          <form onSubmit={handleAddCategory} className="space-y-4">
            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest font-mono mb-2">Tên danh mục</label>
              <input
                type="text"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="Ví dụ: Thể thao, Đời sống..."
                required
                className="w-full px-4 py-2.5 bg-[#070b13]/85 border border-slate-800 rounded-xl focus:border-sky-500 text-sm text-slate-200 outline-none transition"
              />
            </div>
            
            <div className="bg-[#070b13]/40 border border-slate-900/60 p-3 rounded-xl">
              <span className="block text-[9px] font-bold text-slate-500 uppercase tracking-widest font-mono mb-1">Mã định danh tự động (Slug)</span>
              <code className="text-xs font-semibold text-sky-400 font-mono">
                {newName ? slugify(newName) : "chua-co"}
              </code>
            </div>

            <button
              type="submit"
              disabled={submitting || !newName.trim()}
              className="w-full py-2.5 bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white font-bold rounded-xl text-xs flex items-center justify-center gap-2 transition disabled:opacity-50"
            >
              {submitting ? (
                <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <Plus size={14} />
                  XÁC NHẬN THÊM
                </>
              )}
            </button>
          </form>
          
          <div className="mt-5 text-[11px] text-slate-500 bg-amber-500/5 border border-amber-500/10 p-3.5 rounded-xl space-y-2 leading-relaxed">
            <span className="font-bold flex items-center gap-1 text-amber-500/80 font-mono uppercase text-[9px]">
              <AlertCircle size={10} /> Chú ý phân quyền
            </span>
            <p>
              Hệ thống di động sử dụng mã định danh (slug) để hiển thị nhãn và lọc bài viết. Hãy cẩn thận khi đặt tên và hạn chế xóa các danh mục đang có nhiều bài viết.
            </p>
          </div>
        </div>

        {/* 2. Categories List Card */}
        <div className="bg-[#0d1321]/40 border border-slate-900 rounded-2xl p-6 lg:col-span-2 space-y-4">
          <h3 className="text-md font-semibold text-slate-200 flex items-center justify-between">
            <span className="flex items-center gap-2">
              <Layers size={18} className="text-sky-400" />
              Danh sách phân loại ({categories.length})
            </span>
            <span className="text-[10px] px-2 py-0.5 bg-[#070b13] rounded border border-slate-800 text-slate-400 font-mono flex items-center gap-1">
              <ArrowUpDown size={10} /> Sắp xếp theo Order
            </span>
          </h3>

          <div className="space-y-3 max-h-[460px] overflow-y-auto pr-1">
            {loading ? (
              <div className="py-12 text-center text-slate-500 text-sm">
                <span className="inline-block w-6 h-6 border-2 border-slate-800 border-t-sky-500 rounded-full animate-spin mr-2" />
                Đang tải danh sách danh mục...
              </div>
            ) : categories.length === 0 ? (
              <div className="py-12 text-center text-slate-500 text-sm">
                Không tìm thấy danh mục nào.
              </div>
            ) : (
              categories.map((category) => {
                const isCore = ["all", "latest", "study", "social"].includes(category.id);
                return (
                  <div 
                    key={category.id} 
                    className="flex items-center justify-between p-3.5 bg-[#090d16]/90 border border-slate-900 rounded-xl hover:border-slate-800 transition group"
                  >
                    <div className="flex items-center gap-3">
                      <span className="w-6 h-6 rounded-lg bg-slate-950 border border-slate-850 flex items-center justify-center text-[10px] font-bold text-sky-400 font-mono">
                        {category.order}
                      </span>
                      <div>
                        <span className="text-sm font-semibold text-slate-200">{category.name}</span>
                        <code className="block text-[10px] text-slate-500 font-mono mt-0.5">slug: {category.id}</code>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      {isCore ? (
                        <span className="text-[9px] px-2 py-0.5 rounded bg-slate-800 border border-slate-850 text-slate-500 font-mono font-bold uppercase tracking-wider">
                          Mặc định
                        </span>
                      ) : (
                        <button
                          onClick={() => handleDeleteCategory(category.id)}
                          disabled={actionId === category.id}
                          className="p-1.5 bg-slate-950 group-hover:bg-rose-950/20 border border-slate-900 group-hover:border-rose-900/30 text-slate-500 group-hover:text-rose-400 rounded-lg transition"
                          title="Xóa danh mục"
                        >
                          {actionId === category.id ? (
                            <span className="inline-block w-3.5 h-3.5 border border-current border-t-transparent rounded-full animate-spin" />
                          ) : (
                            <Trash2 size={13} />
                          )}
                        </button>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

      </div>
    </div>
  );
};
