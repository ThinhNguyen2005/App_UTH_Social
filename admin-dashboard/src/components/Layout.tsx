import React, { useState } from "react";
import { auth, db } from "../firebase/config";
import { collection, addDoc } from "firebase/firestore";
import { 
  LayoutDashboard, 
  Users, 
  LogOut, 
  Menu, 
  X, 
  ShieldAlert,
  ArrowLeft,
  ShoppingBag,
  Bell,
  User,
  AlertOctagon,
  Tags,
  UserPlus,
  Shield,
  Home,
  PlusSquare,
  Image,
  Send,
  MessageSquare
} from "lucide-react";

interface LayoutProps {
  children: React.ReactNode;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  onBackToLanding: () => void;
  onLogout: () => void;
  userRole: string;
  isAdminMode: boolean;
  setIsAdminMode: (val: boolean) => void;
}

export const Layout: React.FC<LayoutProps> = ({ 
  children, 
  activeTab, 
  setActiveTab,
  onBackToLanding,
  onLogout,
  userRole,
  isAdminMode,
  setIsAdminMode
}) => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isComposeOpen, setIsComposeOpen] = useState(false);
  const [composeText, setComposeText] = useState("");
  const [composeImage, setComposeImage] = useState("");
  const [composeCategory, setComposeCategory] = useState("Học tập");
  const [isPublishing, setIsPublishing] = useState(false);
  const [showImageInput, setShowImageInput] = useState(false);

  const currentUser = auth.currentUser;

  // Determine if this user has admin role privileges
  const isPrivileged = userRole === "admin" || userRole === "super_admin" || userRole === "superadmin";

  const clientNavItems = [
    { id: "posts", name: "Trang chủ", icon: Home },
    { id: "chats", name: "Tin nhắn", icon: MessageSquare },
    { id: "market", name: "Chợ sinh viên", icon: ShoppingBag },
    { id: "notifications", name: "Thông báo", icon: Bell },
    { id: "profile", name: "Trang cá nhân", icon: User },
  ];

  const adminNavItems = [
    { id: "dashboard", name: "Tổng quan", icon: LayoutDashboard },
    { id: "users", name: "Quản lý User", icon: Users },
    { id: "reports", name: "Báo cáo bài viết", icon: AlertOctagon },
    { id: "categories", name: "Quản lý Danh mục", icon: Tags },
  ];

  if (userRole === "super_admin" || userRole === "superadmin") {
    adminNavItems.push({ id: "add-admin", name: "Thêm Admin mới", icon: UserPlus });
  }

  const handleModeToggle = () => {
    const nextMode = !isAdminMode;
    setIsAdminMode(nextMode);
    setActiveTab(nextMode ? "dashboard" : "posts");
  };

  const handleCreatePost = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!composeText.trim() && !composeImage.trim()) return;
    if (!currentUser) return;

    setIsPublishing(true);
    try {
      const newPost = {
        userId: currentUser.uid,
        username: currentUser.displayName || currentUser.email?.split("@")[0] || "Sinh viên UTH",
        userAvatarUrl: currentUser.photoURL || "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da",
        textContent: composeText.trim(),
        imageUrls: composeImage.trim() ? [composeImage.trim()] : [],
        category: composeCategory,
        likes: 0,
        commentCount: 0,
        likedBy: [],
        savedBy: [],
        timestamp: new Date()
      };

      await addDoc(collection(db, "posts"), newPost);
      setComposeText("");
      setComposeImage("");
      setIsComposeOpen(false);
      setShowImageInput(false);
    } catch (err) {
      console.error("Error creating post in Layout:", err);
      alert("Không thể đăng bài viết. Vui lòng kiểm tra lại kết nối.");
    } finally {
      setIsPublishing(false);
    }
  };

  return (
    <div className={`flex h-screen overflow-hidden font-sans transition-colors duration-300 ${
      isAdminMode 
        ? "admin-theme bg-[var(--bg-page)] text-[var(--text-primary)]" 
        : "client-theme bg-[var(--bg-page)] text-[var(--text-primary)]"
    }`}>
      
      {/* ========================================================================= */}
      {/* 1. SIDEBAR NAVIGATION */}
      {/* ========================================================================= */}
      
      {/* Admin Mode Sidebar (Traditional Dashboard Layout) */}
      {isAdminMode ? (
        <aside
          className="fixed inset-y-0 left-0 z-40 w-64 bg-[#0d1321] border-r border-slate-900 flex flex-col transform transition-transform duration-300 md:translate-x-0 md:static"
        >
          {/* Sidebar Brand Logo */}
          <div className="h-16 flex items-center px-6 border-b border-slate-900 gap-3">
            <div className="p-2 bg-gradient-to-tr from-accent to-accent-secondary rounded-lg text-white">
              <ShieldAlert size={20} />
            </div>
            <div>
              <h1 className="font-display font-bold text-lg leading-tight tracking-wider bg-clip-text text-transparent bg-gradient-to-r from-white to-slate-400">
                UTH Social
              </h1>
              <span className="text-[10px] text-sky-400 font-semibold uppercase tracking-widest font-mono">Hệ thống Admin</span>
            </div>
          </div>

          {/* Sidebar Toggle Switch */}
          {isPrivileged && (
            <div className="px-4 pt-5 pb-2">
              <button
                onClick={handleModeToggle}
                className="w-full flex items-center justify-between px-4 py-2.5 rounded-xl text-xs font-bold border bg-amber-500/10 border-amber-500/30 text-amber-400 transition duration-200"
              >
                <div className="flex items-center gap-2">
                  <Shield size={14} />
                  <span>CHẾ ĐỘ ADMIN</span>
                </div>
                <span className="text-[9px] px-1.5 py-0.5 rounded bg-slate-950 font-mono">Đổi</span>
              </button>
            </div>
          )}

          {/* Sidebar Menu Options */}
          <nav className="flex-1 px-4 py-4 space-y-1 overflow-y-auto">
            <span className="block px-4 text-[9px] font-bold text-slate-500 uppercase tracking-widest font-mono mb-2">Quản trị</span>
            {adminNavItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setActiveTab(item.id);
                    setIsSidebarOpen(false);
                  }}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? "bg-gradient-to-r from-amber-600/20 to-orange-600/10 border-l-4 border-amber-500 text-amber-400"
                      : "text-slate-400 hover:bg-slate-800/20 hover:text-slate-200 border-l-4 border-transparent"
                  }`}
                >
                  <Icon size={18} />
                  {item.name}
                </button>
              );
            })}
          </nav>

          {/* Sidebar Footer User Card */}
          <div className="p-4 border-t border-slate-900 bg-[#0a0e19]">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-sky-600 to-indigo-600 flex items-center justify-center font-bold text-white text-sm">
                {currentUser?.email?.[0].toUpperCase() || "U"}
              </div>
              <div className="overflow-hidden flex-1">
                <p className="text-xs font-semibold text-slate-300 truncate">
                  {currentUser?.email || currentUser?.displayName || "Member"}
                </p>
                <p className="text-[10px] text-sky-500 font-medium uppercase font-mono tracking-wider">
                  {userRole === "super_admin" || userRole === "superadmin" ? "Super Admin" : "Admin"}
                </p>
              </div>
            </div>
            <button
              onClick={onLogout}
              className="w-full flex items-center justify-center gap-2 px-3 py-2 text-xs font-semibold text-rose-455 hover:text-white hover:bg-rose-950/20 border border-rose-900/30 hover:border-rose-900 rounded-lg transition"
            >
              <LogOut size={14} />
              Đăng xuất
            </button>
          </div>
        </aside>
      ) : (
        /* Client Mode Sidebar (Instagram Web Replica Layout - Clean White Theme) */
        <aside
          className="hidden md:flex md:flex-col md:w-20 xl:w-60 bg-[var(--bg-card)] border-r border-[var(--border-primary)] h-full py-8 px-3 justify-between shrink-0 transition-all duration-300"
        >
          <div className="space-y-8">
            {/* Logo area */}
            <div className="px-3 py-2">
              <h1 className="font-display font-extrabold text-xl tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-sky-600 to-indigo-600 xl:block hidden">
                UTH Social
              </h1>
              <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-sky-500 to-indigo-600 flex items-center justify-center text-white xl:hidden mx-auto font-bold font-display text-sm">
                U
              </div>
            </div>

            {/* Menu Items */}
            <nav className="space-y-2">
              {clientNavItems.map((item) => {
                const Icon = item.icon;
                const isActive = activeTab === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => setActiveTab(item.id)}
                    className={`w-full flex items-center gap-4 px-3.5 py-3 rounded-xl transition duration-200 group ${
                      isActive 
                        ? "bg-[var(--bg-inset)] text-sky-600 font-bold" 
                        : "text-[var(--text-secondary)] hover:bg-[var(--bg-inset)]/60 hover:text-[var(--text-primary)]"
                    }`}
                  >
                    <Icon size={20} className={`shrink-0 transition group-hover:scale-105 ${isActive ? "text-sky-600" : "text-[var(--text-secondary)]"}`} />
                    <span className="text-sm hidden xl:inline">{item.name}</span>
                  </button>
                );
              })}

              {/* Compose Button Trigger */}
              <button
                onClick={() => setIsComposeOpen(true)}
                className="w-full flex items-center gap-4 px-3.5 py-3 rounded-xl text-[var(--text-secondary)] hover:bg-[var(--bg-inset)]/60 hover:text-[var(--text-primary)] transition duration-200 group"
              >
                <PlusSquare size={20} className="shrink-0 transition group-hover:scale-105 text-[var(--text-secondary)]" />
                <span className="text-sm hidden xl:inline">Tạo bài viết</span>
              </button>
            </nav>
          </div>

          {/* Bottom user settings/switches in Instagram Sidebar */}
          <div className="space-y-3">
            {/* Mode switch for privileged users */}
            {isPrivileged && (
              <button
                onClick={handleModeToggle}
                className="w-full flex items-center gap-4 px-3.5 py-3 rounded-xl text-amber-600 hover:bg-amber-500/5 hover:text-amber-700 transition duration-200 font-mono text-xs font-bold"
              >
                <Shield size={20} className="shrink-0 text-amber-500" />
                <span className="hidden xl:inline uppercase">Chế độ Admin</span>
              </button>
            )}

            {/* Logout button */}
            <button
              onClick={onLogout}
              className="w-full flex items-center gap-4 px-3.5 py-3 rounded-xl text-rose-600 hover:bg-rose-500/5 hover:text-rose-700 transition duration-200 font-mono text-xs font-bold"
            >
              <LogOut size={20} className="shrink-0 text-rose-500" />
              <span className="hidden xl:inline uppercase">Đăng xuất</span>
            </button>
          </div>
        </aside>
      )}

      {/* ========================================================================= */}
      {/* 2. MAIN LAYOUT CONTAINER */}
      {/* ========================================================================= */}
      
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden relative">
        
        {/* Mobile Header (For Client View Only) */}
        {!isAdminMode && (
          <header className="md:hidden h-14 border-b border-[var(--border-primary)] flex items-center justify-between px-5 bg-[var(--bg-card)]/90 backdrop-blur-md sticky top-0 z-30 shrink-0">
            <h1 className="font-display font-extrabold text-md tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-sky-600 to-indigo-600">
              UTH Social
            </h1>
            <div className="flex items-center gap-3">
              {isPrivileged && (
                <button 
                  onClick={handleModeToggle}
                  className="p-1.5 text-amber-600 bg-amber-500/10 border border-amber-500/20 rounded-lg text-xs"
                >
                  <Shield size={14} />
                </button>
              )}
              <button 
                onClick={onLogout}
                className="p-1.5 text-rose-600 bg-rose-500/10 border border-rose-500/20 rounded-lg text-xs"
              >
                <LogOut size={14} />
              </button>
            </div>
          </header>
        )}

        {/* Traditional Topbar Header (For Admin Mode Only) */}
        {isAdminMode && (
          <header className="h-16 border-b border-slate-900 flex items-center justify-between px-8 bg-[#0a0e19]/40 backdrop-blur-md sticky top-0 z-10 shrink-0">
            <div className="flex items-center gap-3 md:hidden">
              <button
                onClick={() => setIsSidebarOpen(!isSidebarOpen)}
                className="p-2 bg-[#101726]/80 rounded-lg border border-slate-800 text-slate-400"
              >
                {isSidebarOpen ? <X size={16} /> : <Menu size={16} />}
              </button>
              <span className="text-sm font-semibold text-slate-300">UTH Social Admin</span>
            </div>
            
            <div className="text-sm font-semibold text-slate-400 md:block hidden">
              Hệ thống Mạng xã hội & Quản trị UTH Social
            </div>
            
            <div className="flex items-center gap-4 ml-auto">
              <button
                onClick={onBackToLanding}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-[#111827] border border-slate-800 rounded-lg hover:bg-slate-800 text-xs text-slate-300 hover:text-white transition"
              >
                <ArrowLeft size={12} />
                Về Trang chủ
              </button>
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                Firebase Online
              </span>
            </div>
          </header>
        )}

        {/* Children scrollable area */}
        <main className={`flex-1 overflow-y-auto ${isAdminMode ? "p-8 max-w-7xl w-full mx-auto" : "md:py-8"}`}>
          {children}
        </main>

        {/* Bottom Nav Bar on Mobile (Client Mode Only) */}
        {!isAdminMode && (
          <nav 
            className="md:hidden h-14 bg-[var(--bg-card)] border-t border-[var(--border-primary)] flex items-center justify-around px-2 pb-safe sticky bottom-0 z-30 shrink-0"
          >
            {clientNavItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveTab(item.id)}
                  className={`p-2 rounded-xl transition-all ${isActive ? "text-sky-600" : "text-[var(--text-muted)]"}`}
                >
                  <Icon size={20} />
                </button>
              );
            })}
            <button
              onClick={() => setIsComposeOpen(true)}
              className="p-2 text-[var(--text-muted)] hover:text-sky-650"
            >
              <PlusSquare size={20} />
            </button>
          </nav>
        )}

        {/* Mobile Sidebar overlay (For Admin view) */}
        {isAdminMode && isSidebarOpen && (
          <div
            onClick={() => setIsSidebarOpen(false)}
            className="fixed inset-0 z-35 bg-black/60 backdrop-blur-sm md:hidden"
          />
        )}
      </div>

      {/* ========================================================================= */}
      {/* 3. COMPOSE POST MODAL (Instagram Styled - Responsive Theme) */}
      {/* ========================================================================= */}
      {isComposeOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl max-w-md w-full overflow-hidden shadow-2xl animate-scaleUp text-left">
            
            {/* Modal Header */}
            <div className="p-4 border-b border-[var(--border-primary)] bg-[var(--bg-inset)]/50 flex items-center justify-between">
              <h3 className="text-sm font-bold text-[var(--text-primary)]">Tạo bài viết mới</h3>
              <button
                onClick={() => {
                  setIsComposeOpen(false);
                  setShowImageInput(false);
                  setComposeText("");
                  setComposeImage("");
                }}
                className="p-1 hover:bg-[var(--bg-inset)] rounded-lg text-[var(--text-muted)] hover:text-[var(--text-primary)] transition"
              >
                <X size={16} />
              </button>
            </div>

            {/* Modal Form */}
            <form onSubmit={handleCreatePost} className="p-5 space-y-4">
              <div className="flex gap-3">
                <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-sky-500 to-indigo-600 flex items-center justify-center font-bold text-white shrink-0">
                  {currentUser?.email?.[0].toUpperCase() || "S"}
                </div>
                <div className="flex-1">
                  <p className="text-xs font-semibold text-[var(--text-primary)]">{currentUser?.displayName || currentUser?.email?.split("@")[0] || "Sinh viên UTH"}</p>
                  <span className="text-[9px] px-1.5 py-0.2 rounded bg-[var(--bg-inset)] font-mono text-[var(--text-muted)] uppercase tracking-wide">Người viết</span>
                </div>
              </div>

              {/* Text content area */}
              <textarea
                value={composeText}
                onChange={(e) => setComposeText(e.target.value)}
                placeholder="Chia sẻ hoạt động học tập, đoàn hội mới của bạn hôm nay..."
                required
                className="w-full bg-transparent border-0 text-[var(--text-primary)] placeholder-[var(--text-muted)] focus:ring-0 outline-none text-sm resize-none h-32 pt-2"
              />

              {/* Optional image input URL */}
              {showImageInput ? (
                <div className="space-y-1.5 animate-fadeIn">
                  <label className="block text-[9px] font-bold text-[var(--text-muted)] uppercase tracking-widest font-mono">URL hình ảnh minh họa</label>
                  <div className="flex items-center gap-2 bg-[var(--bg-inset)] p-2.5 rounded-xl border border-[var(--border-primary)]">
                    <Image size={14} className="text-[var(--text-muted)] shrink-0" />
                    <input
                      type="text"
                      value={composeImage}
                      onChange={(e) => setComposeImage(e.target.value)}
                      placeholder="Nhập link hình ảnh: https://..."
                      className="w-full bg-transparent border-0 outline-none text-xs text-[var(--text-primary)] placeholder-[var(--text-muted)]"
                    />
                  </div>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => setShowImageInput(true)}
                  className="flex items-center gap-2 text-xs font-semibold text-sky-600 hover:text-sky-500 transition"
                >
                  <PlusSquare size={14} /> Thêm ảnh đính kèm
                </button>
              )}

              {/* Selection category and submit */}
              <div className="border-t border-[var(--border-primary)] pt-4 flex items-center justify-between">
                <div className="flex flex-col gap-1">
                  <label className="text-[8px] font-bold text-[var(--text-muted)] uppercase tracking-widest font-mono">Chủ đề phân loại</label>
                  <select
                    value={composeCategory}
                    onChange={(e) => setComposeCategory(e.target.value)}
                    className="bg-[var(--bg-inset)] border border-[var(--border-primary)] text-[var(--text-secondary)] text-xs font-semibold rounded-xl px-2 py-1.5 outline-none cursor-pointer"
                  >
                    <option value="Học tập">Học tập</option>
                    <option value="Xã hội">Xã hội</option>
                    <option value="Đoàn hội">Đoàn hội</option>
                    <option value="Giải trí">Giải trí</option>
                    <option value="Công nghệ">Công nghệ</option>
                  </select>
                </div>

                <button
                  type="submit"
                  disabled={isPublishing || (!composeText.trim() && !composeImage.trim())}
                  className="px-4 py-2 bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white font-bold rounded-xl text-xs flex items-center gap-1.5 transition disabled:opacity-50"
                >
                  {isPublishing ? (
                    <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <>
                      <Send size={12} />
                      <span>ĐĂNG BÀI</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
