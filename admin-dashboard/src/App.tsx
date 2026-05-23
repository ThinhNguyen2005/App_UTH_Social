import { useEffect, useState } from "react";
import { auth, db } from "./firebase/config";
import { onAuthStateChanged, type User } from "firebase/auth";
import { doc, getDoc, updateDoc, setDoc } from "firebase/firestore";
import { Landing } from "./pages/Landing";
import { Login } from "./pages/Login";
import { Layout } from "./components/Layout";
import { DashboardHome } from "./pages/DashboardHome";
import { UserManagement } from "./pages/UserManagement";
import { ReportsView } from "./pages/ReportsView";
import { CategoriesView } from "./pages/CategoriesView";
import { AddAdminView } from "./pages/AddAdminView";
import { PostsView } from "./pages/PostsView";
import { MarketplaceView } from "./pages/MarketplaceView";
import { NotificationsView } from "./pages/NotificationsView";
import { ProfileView } from "./pages/ProfileView";
import { ChatsView } from "./pages/ChatsView";
import { UserProfileModal } from "./components/UserProfileModal";
import "./App.css";

function App() {
  const [currentView, setCurrentView] = useState<"landing" | "admin">("landing");
  const [user, setUser] = useState<User | null>(null);
  const [userRole, setUserRole] = useState<string>("user");
  const [isAdminMode, setIsAdminMode] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("posts"); // Default tab is posts (feed)
  const [selectedProfileId, setSelectedProfileId] = useState<string | null>(null);
  const [selectedChatId, setSelectedChatId] = useState<string | null>(null);
  const [isDemoMode, setIsDemoMode] = useState(false);

  useEffect(() => {
    // Listen to Firebase auth changes
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      setUser(currentUser);
      if (currentUser) {
        try {
          const userRef = doc(db, "users", currentUser.uid);
          const adminRef = doc(db, "admin_users", currentUser.uid);
          
          const [userSnap, adminSnap] = await Promise.all([
            getDoc(userRef),
            getDoc(adminRef)
          ]);
          
          const email = currentUser.email || "";
          const isEmailAdmin = email.includes("admin") || email.includes("super");
          let role = "user";
          
          if (adminSnap.exists()) {
            role = adminSnap.data().role || "admin";
          } else if (userSnap.exists()) {
            role = userSnap.data().role || "user";
          }
          
          // Auto-promote email based admins to super_admin if they are marked as user in Firestore
          if (isEmailAdmin && role === "user") {
            role = "super_admin";
            try {
              await setDoc(adminRef, {
                role: "super_admin",
                grantedBy: "system",
                grantedAt: new Date(),
                permissions: ["all"]
              });
              if (userSnap.exists()) {
                await updateDoc(userRef, { role: "super_admin" });
              }
            } catch (upErr) {
              console.warn("Firestore role promotion restricted by security rules:", upErr);
            }
          }
          
          setUserRole(role);
          const isPrivileged = role === "admin" || role === "super_admin" || role === "superadmin";
          setIsAdminMode(isPrivileged);
          setActiveTab(isPrivileged ? "dashboard" : "posts");
          
          if (!userSnap.exists()) {
            // Seed missing user profile dynamically
            try {
              await setDoc(userRef, {
                name: currentUser.displayName || email.split("@")[0] || "Sinh viên UTH",
                email: email,
                avatarUrl: currentUser.photoURL || "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da",
                bio: "Xin chào! Mình là sinh viên UTH.",
                role: role,
                warningCount: 0,
                isBanned: false,
                followers: [],
                following: [],
                userId: currentUser.uid
              });
            } catch (seedErr) {
              console.warn("Firestore profile seeding restricted by rules:", seedErr);
            }
          }
        } catch (e) {
          console.error("Error fetching user role:", e);
          const email = currentUser.email || "";
          const isEmailAdmin = email.includes("admin") || email.includes("super");
          const role = isEmailAdmin ? "super_admin" : "user";
          setUserRole(role);
          setIsAdminMode(isEmailAdmin);
          setActiveTab(isEmailAdmin ? "dashboard" : "posts");
        }
      } else {
        setUserRole("user");
        setIsAdminMode(false);
        setActiveTab("posts");
      }
      setAuthLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleLogout = async () => {
    setIsDemoMode(false);
    setUserRole("user");
    setIsAdminMode(false);
    setActiveTab("posts");
    try {
      await auth.signOut();
    } catch (e) {
      console.error(e);
    }
  };

  const handleEnterDemo = () => {
    setIsDemoMode(true);
    setUserRole("super_admin"); // Demo gets full access
    setIsAdminMode(true);
    setActiveTab("dashboard");
  };

  if (authLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#070b13] text-slate-455">
        <span className="w-10 h-10 border-2 border-slate-800 border-t-sky-500 rounded-full animate-spin mb-4" />
        <p className="text-xs uppercase tracking-widest font-semibold text-slate-500">
          Đang khởi tạo kết nối...
        </p>
      </div>
    );
  }

  // 1. Render Landing Page
  if (currentView === "landing") {
    return <Landing onEnterLogin={() => setCurrentView("admin")} />;
  }

  // 2. Admin Portal View: Not Logged In
  if (!user && !isDemoMode) {
    return (
      <Login 
        onBackToLanding={() => setCurrentView("landing")} 
        onEnterDemo={handleEnterDemo}
      />
    );
  }

  // 3. Admin Portal View: Logged In
  return (
    <>
      <Layout 
        activeTab={activeTab} 
        setActiveTab={setActiveTab}
        onBackToLanding={() => setCurrentView("landing")}
        onLogout={handleLogout}
        userRole={userRole}
        isAdminMode={isAdminMode}
        setIsAdminMode={setIsAdminMode}
      >
        {/* Admin Mode views */}
        {isAdminMode && activeTab === "dashboard" && <DashboardHome />}
        {isAdminMode && activeTab === "users" && <UserManagement />}
        {isAdminMode && activeTab === "reports" && <ReportsView />}
        {isAdminMode && activeTab === "categories" && <CategoriesView />}
        {isAdminMode && activeTab === "add-admin" && <AddAdminView />}

        {/* Client Mode views */}
        {!isAdminMode && activeTab === "posts" && (
          <PostsView onViewProfile={(uid) => setSelectedProfileId(uid)} />
        )}
        {!isAdminMode && activeTab === "chats" && (
          <ChatsView 
            selectedChatId={selectedChatId} 
            setSelectedChatId={setSelectedChatId}
            onViewProfile={(uid) => setSelectedProfileId(uid)}
          />
        )}
        {!isAdminMode && activeTab === "market" && (
          <MarketplaceView onViewProfile={(uid) => setSelectedProfileId(uid)} />
        )}
        {!isAdminMode && activeTab === "notifications" && <NotificationsView />}
        {!isAdminMode && activeTab === "profile" && <ProfileView />}
      </Layout>

      {selectedProfileId && (
        <UserProfileModal
          userId={selectedProfileId}
          onClose={() => setSelectedProfileId(null)}
          onStartChat={async (targetUserId) => {
            if (!user) return;
            const chatId = [user.uid, targetUserId].sort().join('_');
            try {
              const chatRef = doc(db, "chats", chatId);
              const chatSnap = await getDoc(chatRef);
              if (!chatSnap.exists()) {
                await setDoc(chatRef, {
                  participants: [user.uid, targetUserId],
                  lastMessage: "",
                  lastSenderId: "",
                  timestamp: new Date()
                });
              }
            } catch (err) {
              console.warn("Could not create chat document on start:", err);
            }
            setSelectedChatId(chatId);
            setActiveTab("chats");
            setSelectedProfileId(null);
          }}
        />
      )}
    </>
  );
}

export default App;
