import React, { useEffect, useState } from "react";
import { db, auth } from "../firebase/config";
import { doc, getDoc, updateDoc } from "firebase/firestore";
import { User, Phone, BookOpen, GraduationCap, AlignLeft, Save, AlertCircle, Check } from "lucide-react";

export const ProfileView: React.FC = () => {
  const [profile, setProfile] = useState({
    username: "",
    email: "",
    phone: "",
    major: "",
    campus: "",
    bio: "",
    avatarUrl: ""
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const currentUser = auth.currentUser;

  useEffect(() => {
    const fetchProfile = async () => {
      if (!currentUser) return;
      setLoading(true);
      try {
        const userRef = doc(db, "users", currentUser.uid);
        const userSnap = await getDoc(userRef);
        if (userSnap.exists()) {
          const data = userSnap.data();
          setProfile({
            username: data.username || data.displayName || currentUser.displayName || "",
            email: currentUser.email || data.email || "",
            phone: data.phone || "",
            major: data.major || "",
            campus: data.campus || "",
            bio: data.bio || "",
            avatarUrl: data.avatarUrl || currentUser.photoURL || ""
          });
        }
      } catch (error) {
        console.error("Error fetching profile from Firestore:", error);
        setProfile({
          username: currentUser.displayName || currentUser.email?.split("@")[0] || "Sinh viên UTH",
          email: currentUser.email || "",
          phone: "0912345678",
          major: "Công nghệ thông tin",
          campus: "Cơ sở 1 - Điện Biên Phủ",
          bio: "Học tập và rèn luyện tốt tại UTH!",
          avatarUrl: currentUser.photoURL || "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da"
        });
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [currentUser]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentUser) return;

    setSaving(true);
    setMessage(null);
    try {
      const userRef = doc(db, "users", currentUser.uid);
      await updateDoc(userRef, {
        username: profile.username,
        phone: profile.phone,
        major: profile.major,
        campus: profile.campus,
        bio: profile.bio,
        avatarUrl: profile.avatarUrl
      });
      setMessage({ type: "success", text: "Cập nhật thông tin trang cá nhân thành công!" });
    } catch (error) {
      console.error("Error updating profile:", error);
      setMessage({ type: "error", text: "Không thể lưu thông tin. Có lỗi xảy ra hoặc quyền truy cập bị hạn chế." });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-xl mx-auto space-y-6 text-left animate-fadeIn">
      {/* Page Header */}
      <div>
        <h2 className="text-2xl font-bold font-display text-[var(--text-primary)]">Quản lý Trang Cá Nhân</h2>
        <p className="text-xs text-[var(--text-secondary)] mt-1">Cập nhật hồ sơ sinh viên, thông tin liên lạc hiển thị trên mạng xã hội</p>
      </div>

      {loading ? (
        <div className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl py-12 text-center text-[var(--text-secondary)] text-sm shadow-sm">
          <span className="inline-block w-6 h-6 border-2 border-[var(--border-primary)] border-t-sky-500 rounded-full animate-spin mr-2" />
          Đang tải thông tin cá nhân...
        </div>
      ) : (
        <form onSubmit={handleSave} className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl p-6 space-y-6 shadow-sm">
          
          {/* Status Message */}
          {message && (
            <div className={`p-4 rounded-xl text-xs leading-relaxed flex items-start gap-3 border ${
              message.type === "success" 
                ? "bg-emerald-500/10 border-emerald-500/20 text-emerald-400" 
                : "bg-rose-500/10 border-rose-500/20 text-rose-400"
            }`}>
              {message.type === "success" ? <Check size={16} className="shrink-0" /> : <AlertCircle size={16} className="shrink-0" />}
              <span>{message.text}</span>
            </div>
          )}

          {/* Avatar Preview & URL */}
          <div className="flex flex-col sm:flex-row items-center gap-4 p-4 bg-[var(--bg-inset)] rounded-xl border border-[var(--border-primary)]">
            <div className="w-16 h-16 rounded-full bg-slate-200 dark:bg-slate-800 border border-[var(--border-primary)] overflow-hidden flex items-center justify-center font-bold text-[var(--text-secondary)] text-xl uppercase shrink-0">
              {profile.avatarUrl ? (
                <img src={profile.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
              ) : (
                profile.username?.[0] || "U"
              )}
            </div>
            <div className="flex-1 w-full space-y-1">
              <label className="block text-[10px] font-semibold text-[var(--text-secondary)] uppercase tracking-widest font-mono">Đường dẫn ảnh đại diện</label>
              <input
                type="text"
                value={profile.avatarUrl}
                onChange={(e) => setProfile({ ...profile, avatarUrl: e.target.value })}
                placeholder="https://..."
                className="w-full bg-transparent border-b border-[var(--border-primary)] focus:border-sky-500 text-xs text-[var(--text-secondary)] outline-none pb-1"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Username */}
            <div className="space-y-1.5">
              <label className="block text-[10px] font-semibold text-[var(--text-secondary)] uppercase tracking-widest font-mono">Họ và tên</label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
                <input
                  type="text"
                  required
                  value={profile.username}
                  onChange={(e) => setProfile({ ...profile, username: e.target.value })}
                  placeholder="Nguyễn Văn A"
                  className="w-full pl-10 pr-4 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl focus:border-sky-500 text-sm text-[var(--text-primary)] outline-none transition"
                />
              </div>
            </div>

            {/* Email (Read Only) */}
            <div className="space-y-1.5 opacity-60">
              <label className="block text-[10px] font-semibold text-[var(--text-secondary)] uppercase tracking-widest font-mono">Email UTH</label>
              <div className="relative">
                <input
                  type="email"
                  disabled
                  value={profile.email}
                  className="w-full px-4 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl text-sm text-slate-500 outline-none cursor-not-allowed"
                />
              </div>
            </div>

            {/* Phone */}
            <div className="space-y-1.5">
              <label className="block text-[10px] font-semibold text-[var(--text-secondary)] uppercase tracking-widest font-mono">Số điện thoại</label>
              <div className="relative">
                <Phone className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
                <input
                  type="text"
                  value={profile.phone}
                  onChange={(e) => setProfile({ ...profile, phone: e.target.value })}
                  placeholder="09xxxxxxxx"
                  className="w-full pl-10 pr-4 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl focus:border-sky-500 text-sm text-[var(--text-primary)] outline-none transition"
                />
              </div>
            </div>

            {/* Major */}
            <div className="space-y-1.5">
              <label className="block text-[10px] font-semibold text-[var(--text-secondary)] uppercase tracking-widest font-mono">Ngành học</label>
              <div className="relative">
                <GraduationCap className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
                <input
                  type="text"
                  value={profile.major}
                  onChange={(e) => setProfile({ ...profile, major: e.target.value })}
                  placeholder="Công nghệ Thông tin"
                  className="w-full pl-10 pr-4 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl focus:border-sky-500 text-sm text-[var(--text-primary)] outline-none transition"
                />
              </div>
            </div>
          </div>

          {/* Campus */}
          <div className="space-y-1.5">
            <label className="block text-[10px] font-semibold text-[var(--text-secondary)] uppercase tracking-widest font-mono">Phân hiệu / Cơ sở học tập</label>
            <div className="relative">
              <BookOpen className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
              <input
                type="text"
                value={profile.campus}
                onChange={(e) => setProfile({ ...profile, campus: e.target.value })}
                placeholder="Cơ sở 1 - Điện Biên Phủ"
                className="w-full pl-10 pr-4 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl focus:border-sky-500 text-sm text-[var(--text-primary)] outline-none transition"
              />
            </div>
          </div>

          {/* Bio */}
          <div className="space-y-1.5">
            <label className="block text-[10px] font-semibold text-[var(--text-secondary)] uppercase tracking-widest font-mono">Tiểu sử (Bio)</label>
            <div className="relative">
              <AlignLeft className="absolute left-3 top-4 text-slate-500" size={16} />
              <textarea
                value={profile.bio}
                onChange={(e) => setProfile({ ...profile, bio: e.target.value })}
                placeholder="Giới thiệu đôi nét về bản thân của bạn..."
                className="w-full pl-10 pr-4 py-3 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl focus:border-sky-500 text-sm text-[var(--text-primary)] outline-none transition h-24 resize-none"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={saving}
            className="w-full py-3 bg-gradient-to-r from-accent to-accent-secondary hover:from-accent hover:to-accent-secondary text-white font-bold rounded-xl text-xs flex items-center justify-center gap-1.5 transition shadow-md disabled:opacity-50"
          >
            {saving ? (
              <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <>
                <Save size={14} />
                <span>Lưu thông tin</span>
              </>
            )}
          </button>

        </form>
      )}
    </div>
  );
};
