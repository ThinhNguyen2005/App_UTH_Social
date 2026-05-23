import React, { useEffect, useState } from "react";
import { db, auth } from "../firebase/config";
import { 
  collection, 
  onSnapshot, 
  doc, 
  updateDoc,
  setDoc,
  deleteDoc
} from "firebase/firestore";
import { 
  Search, 
  UserPlus, 
  UserMinus, 
  Shield, 
  ShieldCheck, 
  ShieldAlert, 
  AlertTriangle,
  Mail
} from "lucide-react";

interface UserItem {
  id: string;
  name?: string;
  email?: string;
  role?: string;
  avatarUrl?: string;
}

export const AddAdminView: React.FC = () => {
  const [users, setUsers] = useState<UserItem[]>([]);
  const [adminRoles, setAdminRoles] = useState<Record<string, { role: string; grantedBy?: string }>>({});
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionUserId, setActionUserId] = useState<string | null>(null);
  
  const currentUser = auth.currentUser;

  // 1. Listen to admin_users roles collection
  useEffect(() => {
    const unsubscribeAdmin = onSnapshot(
      collection(db, "admin_users"),
      (snapshot) => {
        const roles: Record<string, { role: string; grantedBy?: string }> = {};
        snapshot.forEach((doc) => {
          const data = doc.data();
          roles[doc.id] = {
            role: data.role || "admin",
            grantedBy: data.grantedBy || ""
          };
        });
        setAdminRoles(roles);
      },
      (err) => {
        console.warn("Blocked querying admin_users collection:", err);
      }
    );
    return () => unsubscribeAdmin();
  }, []);

  // 2. Listen to users profile metadata collection
  useEffect(() => {
    setLoading(true);
    const unsubscribeUsers = onSnapshot(
      collection(db, "users"),
      (snapshot) => {
        const usersList: UserItem[] = [];
        snapshot.forEach((doc) => {
          const data = doc.data();
          usersList.push({
            id: doc.id,
            name: data.username || data.name || data.displayName || "Sinh viên UTH",
            email: data.email || "",
            role: "user", // Base role, dynamically mapped later
            avatarUrl: data.avatarUrl || data.photoURL || ""
          });
        });
        setUsers(usersList);
        setLoading(false);
      },
      () => {
        console.warn("Firestore user query blocked by security rules. Loading mockup users.");
        const mockUsers: UserItem[] = [
          { id: "uid-01", name: "Nguyễn Văn A", email: "anguyen@gmail.com", role: "user" },
          { id: "uid-02", name: "Trần Thị B", email: "btran@gmail.com", role: "user" },
          { id: "uid-03", name: "Lê Hoàng Nam", email: "namle@gmail.com", role: "admin" },
          { id: "uid-04", name: "Phạm Minh Tuấn", email: "tuanpham@gmail.com", role: "user" },
          { id: "uid-05", name: "Trần Thế Vĩ (Demo)", email: "super@example.com", role: "super_admin" }
        ];
        setUsers(mockUsers);
        setLoading(false);
      }
    );

    return () => unsubscribeUsers();
  }, []);

  const handleUpdateRole = async (userId: string, newRole: "user" | "admin" | "super_admin") => {
    // Safety check: Cannot demote oneself
    if (userId === currentUser?.uid) {
      alert("Bạn không thể tự thay đổi vai trò của chính mình!");
      return;
    }

    const roleLabel = newRole === "admin" ? "Admin" : newRole === "super_admin" ? "Super Admin" : "Sinh viên thường";
    if (!window.confirm(`Bạn có chắc chắn muốn thay đổi vai trò của người dùng này thành [${roleLabel}]?`)) {
      return;
    }

    setActionUserId(userId);
    try {
      if (newRole === "user") {
        // Demote: Remove from admin_users
        await deleteDoc(doc(db, "admin_users", userId));
        try {
          await updateDoc(doc(db, "users", userId), { role: "user" });
        } catch (uErr) {
          console.warn("Syncing role to users doc failed:", uErr);
        }
      } else {
        // Promote: Add to admin_users
        await setDoc(doc(db, "admin_users", userId), {
          role: newRole,
          grantedBy: currentUser?.uid || "system",
          grantedAt: new Date(),
          permissions: newRole === "super_admin" ? ["all"] : []
        });
        try {
          await updateDoc(doc(db, "users", userId), { role: newRole });
        } catch (uErr) {
          console.warn("Syncing role to users doc failed:", uErr);
        }
      }
      
      alert(`Đã cập nhật vai trò người dùng thành công.`);
    } catch (error) {
      console.error("Error promoting user:", error);
      alert("Không thể gán quyền. Vui lòng kiểm tra lại Firestore Security Rules.");
    } finally {
      setActionUserId(null);
    }
  };

  // Map user roles dynamically based on adminRoles state
  const usersWithRoles = users.map(u => ({
    ...u,
    role: adminRoles[u.id]?.role || u.role
  }));

  // Ensure any admin UIDs not loaded in user metadata still display in list
  const mergedUsers = [...usersWithRoles];
  Object.keys(adminRoles).forEach(uid => {
    if (!users.some(u => u.id === uid)) {
      mergedUsers.push({
        id: uid,
        name: `Admin (${uid.substring(0, 6)})`,
        email: "Đang tải...",
        role: adminRoles[uid].role,
        avatarUrl: ""
      });
    }
  });

  // Admins List (role: admin, super_admin)
  const administrators = mergedUsers.filter(u => u.role === "admin" || u.role === "super_admin");

  // Non-admins search results list
  const nonAdmins = mergedUsers.filter(u => u.role !== "admin" && u.role !== "super_admin");

  // Filter search results
  const filteredCandidates = nonAdmins.filter(u => {
    if (!searchTerm.trim()) return false; // Show nothing if search is empty to avoid list clutter
    return (
      (u.name?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
      (u.email?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
      u.id.includes(searchTerm)
    );
  });

  return (
    <div className="space-y-6 animate-fadeIn text-left">
      {/* Header */}
      <div>
        <h2 className="text-3xl font-display text-slate-100 tracking-wide">
          Chỉ Định Quyền Quản Trị
        </h2>
        <p className="text-sm text-slate-400 mt-1">Chỉ dành cho Super Admin: Tìm kiếm tài khoản sinh viên để cấp quyền hoặc hạ cấp quản trị viên</p>
      </div>

      {/* Grid panels */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Panel 1: Search & Promote */}
        <div className="bg-[#0d1321]/40 border border-slate-900 rounded-2xl p-6 space-y-4">
          <h3 className="text-md font-semibold text-slate-200 flex items-center gap-2">
            <UserPlus size={18} className="text-sky-400" />
            Tìm kiếm & Cấp quyền Admin
          </h3>

          <div className="relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Nhập tên, email hoặc UID của sinh viên..."
              className="w-full pl-11 pr-4 py-2.5 bg-[#070b13]/85 border border-slate-800 rounded-xl focus:border-sky-500 text-sm text-slate-200 outline-none transition"
            />
          </div>

          <div className="space-y-2 max-h-[300px] overflow-y-auto pr-1">
            {loading ? (
              <div className="py-8 text-center text-slate-500 text-xs">
                Đang tải danh sách người dùng...
              </div>
            ) : !searchTerm.trim() ? (
              <div className="py-8 text-center text-slate-600 text-xs italic">
                Nhập từ khóa tìm kiếm để hiển thị các tài khoản sinh viên có thể gán quyền.
              </div>
            ) : filteredCandidates.length === 0 ? (
              <div className="py-8 text-center text-slate-500 text-xs">
                Không tìm thấy sinh viên nào phù hợp (hoặc tài khoản đã có quyền Admin).
              </div>
            ) : (
              filteredCandidates.map((user) => (
                <div 
                  key={user.id} 
                  className="flex items-center justify-between p-3.5 bg-[#090d16]/90 border border-slate-900 rounded-xl hover:border-slate-800 transition"
                >
                  <div className="min-w-0 flex-1 pr-3">
                    <p className="font-semibold text-slate-200 text-sm truncate">{user.name}</p>
                    <p className="text-xs text-slate-500 truncate flex items-center gap-1 mt-0.5">
                      <Mail size={10} /> {user.email || "Chưa cập nhật email"}
                    </p>
                  </div>
                  
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleUpdateRole(user.id, "admin")}
                      disabled={actionUserId === user.id}
                      className="px-2.5 py-1.5 bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 rounded-lg text-xs font-semibold transition font-mono"
                    >
                      + ADMIN
                    </button>
                    <button
                      onClick={() => handleUpdateRole(user.id, "super_admin")}
                      disabled={actionUserId === user.id}
                      className="px-2.5 py-1.5 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded-lg text-xs font-semibold transition font-mono"
                    >
                      + SUPER
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="text-[11px] text-slate-500 bg-amber-500/5 border border-amber-500/10 p-3.5 rounded-xl space-y-2 leading-relaxed">
            <span className="font-bold flex items-center gap-1 text-amber-500/80 font-mono uppercase text-[9px]">
              <AlertTriangle size={10} /> Cảnh báo bảo mật
            </span>
            <p>
              Quyền <strong>Super Admin</strong> có khả năng tạo, sửa, gỡ quyền quản trị của các Admin khác và quản trị toàn bộ danh mục bài viết. Hãy cân nhắc kỹ trước khi cấp quyền hạn cao nhất này.
            </p>
          </div>
        </div>

        {/* Panel 2: Current Admins List */}
        <div className="bg-[#0d1321]/40 border border-slate-900 rounded-2xl p-6 space-y-4">
          <h3 className="text-md font-semibold text-slate-200 flex items-center gap-2">
            <Shield size={18} className="text-amber-400" />
            Ban Quản Trị Hệ Thống ({administrators.length})
          </h3>

          <div className="space-y-3 max-h-[460px] overflow-y-auto pr-1">
            {loading ? (
              <div className="py-8 text-center text-slate-500 text-xs">
                Đang tải ban quản trị...
              </div>
            ) : administrators.length === 0 ? (
              <div className="py-8 text-center text-slate-500 text-xs">
                Không có quản trị viên nào.
              </div>
            ) : (
              administrators.map((user) => {
                const isSuper = user.role === "super_admin" || user.role === "superadmin";
                const isSelf = user.id === currentUser?.uid;

                return (
                  <div 
                    key={user.id} 
                    className="flex items-center justify-between p-3.5 bg-[#090d16]/90 border border-slate-900 rounded-xl hover:border-slate-800 transition"
                  >
                    <div className="min-w-0 pr-3">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-slate-200 text-sm truncate">{user.name}</span>
                        {isSelf && (
                          <span className="text-[8px] px-1.5 py-0.2 bg-slate-850 rounded text-slate-400 border border-slate-800 uppercase font-mono">Tôi</span>
                        )}
                      </div>
                      <p className="text-xs text-slate-500 truncate mt-0.5">{user.email}</p>
                      
                      <div className="mt-1.5 flex items-center gap-1.5">
                        {isSuper ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[9px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20 font-mono uppercase tracking-wider">
                            <ShieldAlert size={9} /> Super Admin
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[9px] font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 font-mono uppercase tracking-wider">
                            <ShieldCheck size={9} /> Admin
                          </span>
                        )}
                      </div>
                    </div>

                    <div>
                      {isSelf ? (
                        <span className="text-xs text-slate-600 italic font-mono pr-2">Không thể tự sửa</span>
                      ) : (
                        <button
                          onClick={() => handleUpdateRole(user.id, "user")}
                          disabled={actionUserId === user.id}
                          className="p-1.5 bg-slate-950 hover:bg-rose-950/20 border border-slate-900 hover:border-rose-900/30 text-slate-500 hover:text-rose-400 rounded-lg transition"
                          title="Hạ quyền xuống Sinh viên thường"
                        >
                          {actionUserId === user.id ? (
                            <span className="inline-block w-3.5 h-3.5 border border-current border-t-transparent rounded-full animate-spin" />
                          ) : (
                            <UserMinus size={13} />
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
