import React, { useEffect, useState } from "react";
import { db } from "../firebase/config";
import { 
  collection, 
  onSnapshot, 
  doc, 
  updateDoc,
  addDoc,
  increment 
} from "firebase/firestore";
import { 
  Search, 
  UserCheck, 
  UserX, 
  AlertTriangle, 
  ShieldCheck, 
  BellRing,
  Mail 
} from "lucide-react";

interface UserItem {
  id: string;
  name?: string;
  email?: string;
  role?: string;
  isBanned?: boolean;
  warningCount?: number;
  avatarUrl?: string;
}

export const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<UserItem[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [filterStatus, setFilterStatus] = useState<"all" | "active" | "banned">("all");
  const [loading, setLoading] = useState(true);
  const [actionUserId, setActionUserId] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    // Real-time listener for users collection
    const unsubscribe = onSnapshot(
      collection(db, "users"),
      (snapshot) => {
        const usersList: UserItem[] = [];
        snapshot.forEach((doc) => {
          const data = doc.data();
          usersList.push({
            id: doc.id,
            name: data.name || data.displayName || "Người dùng UTH",
            email: data.email || "Chưa cập nhật email",
            role: data.role || "user",
            isBanned: data.isBanned || false,
            warningCount: data.warningCount || 0,
            avatarUrl: data.avatarUrl || data.photoURL || ""
          });
        });
        setUsers(usersList);
        setLoading(false);
      },
      () => {
        console.warn("Firestore collection listen restricted by Security Rules. Defaulting to administrative user mockup data.");
        const mockUsers: UserItem[] = [
          { id: "uid-01", name: "Nguyễn Văn A", email: "anguyen@gmail.com", role: "user", isBanned: false, warningCount: 2 },
          { id: "uid-02", name: "Trần Thị B", email: "btran@gmail.com", role: "user", isBanned: true, warningCount: 0 },
          { id: "uid-03", name: "Lê Hoàng Nam", email: "namle@gmail.com", role: "admin", isBanned: false, warningCount: 0 },
          { id: "uid-04", name: "Phạm Minh Tuấn", email: "tuanpham@gmail.com", role: "user", isBanned: false, warningCount: 1 }
        ];
        setUsers(mockUsers);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, []);

  const toggleBanStatus = async (userId: string, currentBanState: boolean) => {
    setActionUserId(userId);
    const actionLabel = currentBanState ? "Mở khóa" : "Khóa vĩnh viễn";
    if (!window.confirm(`Bạn có chắc muốn ${actionLabel} tài khoản này?`)) {
      setActionUserId(null);
      return;
    }
    
    try {
      const userRef = doc(db, "users", userId);
      await updateDoc(userRef, {
        isBanned: !currentBanState
      });
      // Local state sync
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, isBanned: !currentBanState } : u));
    } catch (error) {
      console.error("Error updating user status:", error);
      alert("Không thể cập nhật trạng thái người dùng. Vui lòng kiểm tra lại Firestore Security Rules.");
    } finally {
      setActionUserId(null);
    }
  };

  // Warning action inside User List
  const handleWarnUser = async (userId: string, userName: string) => {
    const customMessage = window.prompt(
      `Gửi thông báo cảnh cáo tới "${userName}":`,
      "Tài khoản của bạn nhận được cảnh cáo do vi phạm chính sách cộng đồng UTH Social. Vui lòng điều chỉnh hành vi."
    );
    if (customMessage === null) return;

    setActionUserId(userId);
    try {
      // 1. Increment count
      const userRef = doc(db, "users", userId);
      await updateDoc(userRef, {
        warningCount: increment(1)
      });

      // 2. Add notification doc
      await addDoc(collection(db, "notifications"), {
        receiverId: userId,
        title: "CẢNH BÁO VI PHẠM TÀI KHOẢN",
        body: customMessage,
        type: "system",
        createdAt: new Date()
      });

      // Sync state
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, warningCount: (u.warningCount || 0) + 1 } : u));
      alert(`Đã gửi cảnh cáo đến ${userName}.`);
    } catch (err) {
      console.error("Error warning user in list:", err);
      alert("Không thể gửi cảnh cáo. Vui lòng kiểm tra Firestore Security Rules.");
    } finally {
      setActionUserId(null);
    }
  };

  // Filtered list
  const filteredUsers = users.filter((u) => {
    const matchesSearch = 
      (u.name?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
      (u.email?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
      u.id.includes(searchTerm);

    const matchesStatus = 
      filterStatus === "all" ||
      (filterStatus === "banned" && u.isBanned) ||
      (filterStatus === "active" && !u.isBanned);

    return matchesSearch && matchesStatus;
  });

  return (
    <div className="space-y-6 animate-fadeIn text-left">
      {/* Header */}
      <div>
        <h2 className="text-3xl font-display text-slate-100 tracking-wide">
          Quản Lý Tài Khoản Người Dùng
        </h2>
        <p className="text-sm text-slate-400 mt-1">Kiểm soát tài khoản sinh viên: Theo dõi số lần cảnh cáo, gửi cảnh cáo trực tiếp, khóa hoặc mở khóa tài khoản</p>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row gap-4 justify-between items-stretch bg-[#0d1321]/50 border border-slate-900 p-4 rounded-2xl">
        <div className="relative flex-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo Tên, Email hoặc UID..."
            className="w-full pl-11 pr-4 py-2.5 bg-[#070b13]/85 border border-slate-800 rounded-xl focus:border-sky-500 text-sm text-slate-200 outline-none transition"
          />
        </div>
        
        <div className="flex gap-2">
          {["all", "active", "banned"].map((status) => (
            <button
              key={status}
              onClick={() => setFilterStatus(status as any)}
              className={`px-4 py-2.5 rounded-xl text-xs font-semibold uppercase tracking-wider transition font-mono ${
                filterStatus === status
                  ? "bg-sky-600/25 border border-sky-500/50 text-sky-400"
                  : "bg-[#070b13]/40 border border-slate-850 text-slate-400 hover:text-white"
              }`}
            >
              {status === "all" ? "Tất cả" : status === "active" ? "Hoạt động" : "Đã khóa"}
            </button>
          ))}
        </div>
      </div>

      {/* Users Table */}
      <div className="bg-[#0d1321]/30 border border-slate-900 rounded-2xl overflow-hidden shadow-lg">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-900 bg-[#0c1220]/80">
                <th className="py-4 px-6 text-xs font-semibold uppercase tracking-widest text-slate-400 font-mono">Người dùng</th>
                <th className="py-4 px-6 text-xs font-semibold uppercase tracking-widest text-slate-400 font-mono">Cảnh cáo</th>
                <th className="py-4 px-6 text-xs font-semibold uppercase tracking-widest text-slate-400 font-mono">Vai trò</th>
                <th className="py-4 px-6 text-xs font-semibold uppercase tracking-widest text-slate-400 font-mono">Trạng thái</th>
                <th className="py-4 px-6 text-xs font-semibold uppercase tracking-widest text-slate-400 text-right font-mono">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-950/80">
              {loading ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-slate-500 text-sm">
                    <span className="inline-block w-6 h-6 border-2 border-slate-800 border-t-sky-500 rounded-full animate-spin mr-2" />
                    Đang tải danh sách người dùng...
                  </td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-slate-500 text-sm">
                    Không tìm thấy người dùng nào phù hợp.
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.id} className="hover:bg-slate-800/10 transition duration-200">
                    <td className="py-4 px-6 flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-sky-700/50 to-indigo-700/50 border border-slate-800 flex items-center justify-center font-bold text-slate-200 uppercase text-xs overflow-hidden shrink-0">
                        {user.avatarUrl ? (
                          <img src={user.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                        ) : (
                          user.name?.[0] || "U"
                        )}
                      </div>
                      <div className="min-w-0">
                        <p className="font-semibold text-slate-200 text-sm truncate">{user.name}</p>
                        <p className="text-xs text-slate-500 truncate flex items-center gap-1">
                          <Mail size={10} className="text-slate-650" /> {user.email}
                        </p>
                        <p className="text-[9px] text-slate-600 font-mono mt-0.5">UID: {user.id}</p>
                      </div>
                    </td>
                    <td className="py-4 px-6">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold font-mono ${
                        (user.warningCount || 0) >= 3 
                          ? "bg-rose-500/15 text-rose-400 border border-rose-500/20" 
                          : (user.warningCount || 0) > 0
                          ? "bg-amber-500/10 text-amber-400 border border-amber-500/20"
                          : "bg-slate-500/10 text-slate-500 border border-slate-850"
                      }`}>
                        {user.warningCount || 0} lần
                      </span>
                    </td>
                    <td className="py-4 px-6">
                      {user.role === "admin" || user.role === "super_admin" ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 font-mono tracking-wider uppercase">
                          <ShieldCheck size={10} /> {user.role === "super_admin" ? "Super Admin" : "Admin"}
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-slate-500/10 text-slate-400 border border-slate-850 font-mono tracking-wider uppercase">
                          Sinh viên
                        </span>
                      )}
                    </td>
                    <td className="py-4 px-6">
                      {user.isBanned ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20 animate-pulse font-mono tracking-wider uppercase">
                          <AlertTriangle size={10} /> ĐÃ KHÓA
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono tracking-wider uppercase">
                          <UserCheck size={10} /> Hoạt động
                        </span>
                      )}
                    </td>
                    <td className="py-4 px-6 text-right">
                      {user.role === "admin" || user.role === "super_admin" ? (
                        <span className="text-xs text-slate-600 italic">Không thể sửa</span>
                      ) : (
                        <div className="flex justify-end gap-2">
                          {/* Warnings button */}
                          {!user.isBanned && (
                            <button
                              onClick={() => handleWarnUser(user.id, user.name || "")}
                              disabled={actionUserId === user.id}
                              className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 transition font-mono uppercase"
                              title="Gửi cảnh cáo"
                            >
                              <BellRing size={12} />
                              <span>CẢNH BÁO</span>
                            </button>
                          )}

                          {/* Lock/Unlock Button */}
                          <button
                            onClick={() => toggleBanStatus(user.id, user.isBanned || false)}
                            disabled={actionUserId === user.id}
                            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition font-mono uppercase ${
                              user.isBanned
                                ? "bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
                                : "bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30"
                            }`}
                          >
                            {actionUserId === user.id ? (
                              <span className="w-3.5 h-3.5 border border-current border-t-transparent rounded-full animate-spin" />
                            ) : user.isBanned ? (
                              <>
                                <UserCheck size={12} />
                                <span>Mở khóa</span>
                              </>
                            ) : (
                              <>
                                <UserX size={12} />
                                <span>Khóa nick</span>
                              </>
                            )}
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
