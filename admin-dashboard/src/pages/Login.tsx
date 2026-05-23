import React, { useState } from "react";
import { auth, db } from "../firebase/config";
import { 
  signInWithEmailAndPassword, 
  signInWithPopup, 
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
  updateProfile
} from "firebase/auth";
import { doc, getDoc, setDoc, updateDoc } from "firebase/firestore";
import { KeyRound, Mail, AlertTriangle, ArrowLeft, User, UserPlus, LogIn } from "lucide-react";

interface LoginProps {
  onBackToLanding: () => void;
  onEnterDemo: () => void;
}

export const Login: React.FC<LoginProps> = ({ onBackToLanding, onEnterDemo }) => {
  const [activeTab, setActiveTab] = useState<"login" | "register">("login");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Helper to create profile inside Firestore /users/{uid} on registration or first login
  const createUserProfileIfNotExists = async (user: any, nameInput?: string) => {
    try {
      const userRef = doc(db, "users", user.uid);
      const adminRef = doc(db, "admin_users", user.uid);
      
      const [userSnap, adminSnap] = await Promise.all([
        getDoc(userRef),
        getDoc(adminRef)
      ]);

      const email = user.email || "";
      const isEmailAdmin = email.includes("admin") || email.includes("super");
      let role = "user";

      if (adminSnap.exists()) {
        role = adminSnap.data().role || "admin";
      } else if (isEmailAdmin) {
        role = "super_admin";
        try {
          await setDoc(adminRef, {
            role: "super_admin",
            grantedBy: "system",
            grantedAt: new Date(),
            permissions: ["all"]
          });
        } catch (err) {
          console.warn("Could not seed admin_users collection:", err);
        }
      } else if (userSnap.exists()) {
        role = userSnap.data().role || "user";
      }

      if (!userSnap.exists()) {
        await setDoc(userRef, {
          name: nameInput || user.displayName || user.email?.split("@")[0] || "Sinh viên UTH",
          email: email,
          avatarUrl: user.photoURL || "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da",
          bio: "Xin chào! Mình là sinh viên UTH.",
          role: role,
          warningCount: 0,
          isBanned: false,
          followers: [],
          following: [],
          userId: user.uid
        });
      } else {
        const data = userSnap.data();
        if (role !== "user" && data.role !== role) {
          await updateDoc(userRef, { role: role });
        }
      }
    } catch (e) {
      console.error("Error creating user profile in Firestore:", e);
    }
  };

  const handleEmailAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      if (activeTab === "login") {
        // 1. Log In Flow
        const credential = await signInWithEmailAndPassword(auth, email, password);
        await createUserProfileIfNotExists(credential.user);
      } else {
        // 2. Register/Sign Up Flow
        if (password.length < 6) {
          setError("Mật khẩu phải chứa ít nhất 6 ký tự.");
          setLoading(false);
          return;
        }
        const credential = await createUserWithEmailAndPassword(auth, email, password);
        // Set display name in auth
        if (fullName.trim()) {
          await updateProfile(credential.user, { displayName: fullName.trim() });
        }
        // Save database profile
        await createUserProfileIfNotExists(credential.user, fullName.trim());
        alert("Đăng ký tài khoản sinh viên thành công!");
      }
    } catch (err: any) {
      console.error(err);
      if (err.code === "auth/invalid-credential" || err.code === "auth/user-not-found" || err.code === "auth/wrong-password") {
        setError("Email hoặc Mật khẩu không chính xác.");
      } else if (err.code === "auth/email-already-in-use") {
        setError("Địa chỉ email này đã được sử dụng bởi tài khoản khác.");
      } else if (err.code === "auth/invalid-email") {
        setError("Định dạng địa chỉ email không hợp lệ.");
      } else {
        setError("Có lỗi xảy ra khi xác thực tài khoản. Vui lòng thử lại.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setLoading(true);
    setError("");
    const provider = new GoogleAuthProvider();

    try {
      const credential = await signInWithPopup(auth, provider);
      await createUserProfileIfNotExists(credential.user);
    } catch (err: any) {
      console.error(err);
      if (err.code !== "auth/popup-closed-by-user") {
        setError("Đăng nhập bằng Google thất bại hoặc bị hủy.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative min-h-screen flex items-center justify-center bg-slate-50 px-4 overflow-hidden font-sans text-left">
      
      {/* Back to Home floating action */}
      <button
        onClick={onBackToLanding}
        className="absolute top-6 left-6 z-20 flex items-center gap-1.5 px-3 py-2 bg-white hover:bg-slate-100 border border-slate-200 text-xs font-semibold text-slate-500 hover:text-slate-800 rounded-xl shadow-sm transition animate-fadeIn"
      >
        <ArrowLeft size={14} />
        Quay lại Trang chủ
      </button>

      {/* Decorative gradient blur rings */}
      <div className="absolute top-1/4 left-1/4 w-[35rem] h-[35rem] bg-sky-500/5 rounded-full blur-[140px] pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-[35rem] h-[35rem] bg-indigo-500/5 rounded-full blur-[140px] pointer-events-none" />

      {/* Form Card wrapper */}
      <div className="w-full max-w-[390px] bg-white border border-slate-200 rounded-2xl p-6 md:p-8 shadow-xl z-10 space-y-6 animate-fadeIn">
        
        {/* Logo and Greeting Header */}
        <div className="text-center space-y-1">
          <h2 className="text-2xl font-extrabold font-display tracking-wide text-transparent bg-clip-text bg-gradient-to-r from-sky-600 to-indigo-600">
            UTH Social
          </h2>
          <p className="text-xs text-slate-400 font-medium">
            {activeTab === "login" ? "Kết nối học tập & mạng xã hội sinh viên" : "Tạo tài khoản sinh viên mới"}
          </p>
        </div>

        {/* Tab Selection */}
        <div className="flex border-b border-slate-200">
          <button
            onClick={() => {
              setActiveTab("login");
              setError("");
            }}
            className={`flex-1 pb-3 text-xs font-bold uppercase tracking-wider transition-all duration-200 border-b-2 font-mono flex items-center justify-center gap-1.5 ${
              activeTab === "login" 
                ? "border-sky-500 text-sky-600" 
                : "border-transparent text-slate-400 hover:text-slate-600"
            }`}
          >
            <LogIn size={13} />
            Đăng nhập
          </button>
          <button
            onClick={() => {
              setActiveTab("register");
              setError("");
            }}
            className={`flex-1 pb-3 text-xs font-bold uppercase tracking-wider transition-all duration-200 border-b-2 font-mono flex items-center justify-center gap-1.5 ${
              activeTab === "register" 
                ? "border-sky-500 text-sky-600" 
                : "border-transparent text-slate-400 hover:text-slate-600"
            }`}
          >
            <UserPlus size={13} />
            Đăng ký
          </button>
        </div>

        {/* Error alerting banner */}
        {error && (
          <div className="flex items-start gap-2.5 p-3.5 bg-rose-50 border border-rose-100 text-rose-600 rounded-xl text-xs leading-relaxed animate-fadeIn">
            <AlertTriangle className="shrink-0 mt-0.5" size={14} />
            <span>{error}</span>
          </div>
        )}

        {/* Form elements */}
        <form onSubmit={handleEmailAuth} className="space-y-4">
          {/* Full Name input (Only for registration) */}
          {activeTab === "register" && (
            <div className="space-y-1.5 animate-fadeIn">
              <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-widest font-mono">
                Họ và tên
              </label>
              <div className="relative">
                <User className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                <input
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Nguyễn Văn A"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:border-sky-500 focus:ring-1 focus:ring-sky-500 text-sm text-slate-800 placeholder-slate-400 outline-none transition"
                />
              </div>
            </div>
          )}

          {/* Email input */}
          <div className="space-y-1.5">
            <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-widest font-mono">
              Địa chỉ Email
            </label>
            <div className="relative">
              <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
              <input
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="sinhvien@uth.edu.vn"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:border-sky-500 focus:ring-1 focus:ring-sky-500 text-sm text-slate-800 placeholder-slate-400 outline-none transition"
              />
            </div>
          </div>

          {/* Password input */}
          <div className="space-y-1.5">
            <label className="block text-[9px] font-bold text-slate-500 uppercase tracking-widest font-mono">
              Mật khẩu
            </label>
            <div className="relative">
              <KeyRound className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
              <input
                type="password"
                required
                autoComplete={activeTab === "login" ? "current-password" : "new-password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:border-sky-500 focus:ring-1 focus:ring-sky-500 text-sm text-slate-800 placeholder-slate-400 outline-none transition"
              />
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 px-4 bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white font-bold rounded-xl transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center text-xs tracking-wider uppercase font-mono shadow-md hover:shadow-lg cursor-pointer"
          >
            {loading ? (
              <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
            ) : activeTab === "login" ? (
              "ĐĂNG NHẬP"
            ) : (
              "ĐĂNG KÝ MỚI"
            )}
          </button>
        </form>

        {/* Divider */}
        <div className="relative flex items-center">
          <div className="flex-grow border-t border-slate-200"></div>
          <span className="flex-shrink mx-3 text-[9px] font-bold text-slate-400 uppercase tracking-wider font-mono">Hoặc</span>
          <div className="flex-grow border-t border-slate-200"></div>
        </div>

        {/* Google OAuth action */}
        <button
          onClick={handleGoogleLogin}
          disabled={loading}
          className="w-full py-2.5 px-4 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 font-bold rounded-xl transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2.5 text-xs font-mono shadow-sm cursor-pointer"
        >
          <svg className="w-3.5 h-3.5 shrink-0" viewBox="0 0 24 24">
            <path
              fill="#EA4335"
              d="M12.24 10.285V14.4h6.887c-.648 2.41-2.519 4.114-5.18 4.114-3.522 0-6.377-2.87-6.377-6.41s2.855-6.41 6.377-6.41c1.628 0 3.118.614 4.257 1.621l3.076-3.077C19.248 2.378 15.932 1 12.24 1 6.033 1 1 6.033 1 12.24s5.033 11.24 11.24 11.24c5.84 0 10.74-4.18 10.74-11.24 0-.693-.06-1.36-.172-1.955H12.24z"
            />
          </svg>
          TIẾP TỤC VỚI GOOGLE
        </button>

        {/* Minimalist Demo bypass trigger */}
        <div className="text-center pt-2">
          <button
            type="button"
            onClick={onEnterDemo}
            className="text-[10px] font-bold text-sky-600 hover:text-sky-500 transition font-mono uppercase tracking-wider cursor-pointer"
          >
            DÙNG THỬ NHANH (CHẾ ĐỘ DEMO)
          </button>
        </div>

      </div>
    </div>
  );
};
