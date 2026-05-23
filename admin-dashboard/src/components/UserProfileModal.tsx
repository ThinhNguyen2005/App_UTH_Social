import React, { useEffect, useState } from "react";
import { db, auth } from "../firebase/config";
import { doc, getDoc, collection, query, where, getDocs } from "firebase/firestore";
import { X, MessageSquare, GraduationCap, MapPin, Mail, Phone, Heart, MessageCircle } from "lucide-react";

interface UserProfileModalProps {
  userId: string;
  onClose: () => void;
  onStartChat: (userId: string) => void;
}

interface UserProfile {
  name: string;
  avatarUrl: string;
  email: string;
  phone?: string;
  major?: string;
  campus?: string;
  bio?: string;
}

interface UserPost {
  id: string;
  imageUrls: string[];
  textContent: string;
  likes: number;
  commentCount: number;
}

export const UserProfileModal: React.FC<UserProfileModalProps> = ({ userId, onClose, onStartChat }) => {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [posts, setPosts] = useState<UserPost[]>([]);
  const [loading, setLoading] = useState(true);

  const currentUser = auth.currentUser;

  useEffect(() => {
    const fetchUserData = async () => {
      setLoading(true);
      try {
        // 1. Fetch User Profile
        const userRef = doc(db, "users", userId);
        const userSnap = await getDoc(userRef);
        if (userSnap.exists()) {
          const data = userSnap.data();
          setProfile({
            name: data.username || data.name || data.displayName || "Sinh viên UTH",
            email: data.email || "",
            phone: data.phone || "",
            major: data.major || "Chưa cập nhật",
            campus: data.campus || "Chưa cập nhật",
            bio: data.bio || "Xin chào! Mình là sinh viên UTH.",
            avatarUrl: data.avatarUrl || data.photoURL || "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da"
          });
        } else {
          // Fallback if user doc doesn't exist
          setProfile({
            name: "Sinh viên UTH",
            email: "",
            avatarUrl: "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da"
          });
        }

        // 2. Fetch User's Posts
        const postsQuery = query(collection(db, "posts"), where("userId", "==", userId));
        const postsSnap = await getDocs(postsQuery);
        const postsList: UserPost[] = [];
        postsSnap.forEach((docSnap) => {
          const data = docSnap.data();
          postsList.push({
            id: docSnap.id,
            imageUrls: data.imageUrls || (data.imageUrl ? [data.imageUrl] : []),
            textContent: data.textContent || "",
            likes: data.likes || 0,
            commentCount: data.commentCount || 0
          });
        });
        setPosts(postsList);
      } catch (err) {
        console.error("Error loading user profile details:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, [userId]);

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-xl bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh] text-left">
        
        {/* Header */}
        <div className="p-4 border-b border-[var(--border-secondary)] flex justify-between items-center bg-[var(--bg-inset)]/10">
          <span className="font-semibold text-xs text-[var(--text-secondary)] uppercase tracking-wider font-mono">Thông tin sinh viên</span>
          <button 
            onClick={onClose}
            className="p-1 hover:bg-[var(--bg-inset)] rounded-lg text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition"
          >
            <X size={16} />
          </button>
        </div>

        {loading ? (
          <div className="p-12 text-center text-[var(--text-secondary)] text-sm flex flex-col items-center gap-2">
            <span className="inline-block w-6 h-6 border-2 border-[var(--border-primary)] border-t-sky-500 rounded-full animate-spin" />
            Đang tải thông tin cá nhân...
          </div>
        ) : !profile ? (
          <div className="p-8 text-center text-[var(--text-secondary)] text-xs">
            Không tìm thấy thông tin người dùng này.
          </div>
        ) : (
          <div className="overflow-y-auto flex-1 p-6 space-y-6">
            
            {/* Top Identity Block */}
            <div className="flex flex-col sm:flex-row items-center gap-5 pb-6 border-b border-[var(--border-secondary)]">
              <div className="w-20 h-20 rounded-full overflow-hidden border-2 border-sky-500 bg-[var(--bg-inset)] shrink-0 flex items-center justify-center text-2xl font-bold text-[var(--text-secondary)]">
                {profile.avatarUrl ? (
                  <img src={profile.avatarUrl} alt={profile.name} className="w-full h-full object-cover" />
                ) : (
                  profile.name[0].toUpperCase()
                )}
              </div>
              
              <div className="flex-1 text-center sm:text-left space-y-3">
                <div className="space-y-1">
                  <h3 className="text-lg font-bold text-[var(--text-primary)]">{profile.name}</h3>
                  <p className="text-xs text-[var(--text-muted)] italic max-w-[320px] whitespace-pre-wrap">{profile.bio}</p>
                </div>
                
                <div className="flex flex-wrap gap-2 justify-center sm:justify-start">
                  {currentUser && currentUser.uid !== userId && (
                    <button
                      onClick={() => onStartChat(userId)}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-bold transition shadow-sm"
                    >
                      <MessageSquare size={14} />
                      Nhắn tin
                    </button>
                  )}
                  <span className="px-2.5 py-1.5 bg-[var(--bg-inset)] text-[10px] font-bold text-sky-600 border border-[var(--border-primary)] rounded-xl font-mono uppercase tracking-wider">
                    {posts.length} Bài viết
                  </span>
                </div>
              </div>
            </div>

            {/* Details Fields Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="flex items-center gap-3 p-3 bg-[var(--bg-inset)] rounded-xl border border-[var(--border-primary)]">
                <GraduationCap size={16} className="text-sky-600" />
                <div>
                  <span className="block text-[8px] uppercase tracking-wider font-semibold font-mono text-[var(--text-muted)]">Ngành học</span>
                  <span className="font-semibold text-[var(--text-secondary)]">{profile.major}</span>
                </div>
              </div>

              <div className="flex items-center gap-3 p-3 bg-[var(--bg-inset)] rounded-xl border border-[var(--border-primary)]">
                <MapPin size={16} className="text-sky-600" />
                <div>
                  <span className="block text-[8px] uppercase tracking-wider font-semibold font-mono text-[var(--text-muted)]">Cơ sở học</span>
                  <span className="font-semibold text-[var(--text-secondary)]">{profile.campus}</span>
                </div>
              </div>

              {profile.email && (
                <div className="flex items-center gap-3 p-3 bg-[var(--bg-inset)] rounded-xl border border-[var(--border-primary)]">
                  <Mail size={16} className="text-sky-600" />
                  <div className="min-w-0 flex-1">
                    <span className="block text-[8px] uppercase tracking-wider font-semibold font-mono text-[var(--text-muted)]">Thư điện tử</span>
                    <span className="font-semibold text-[var(--text-secondary)] truncate block">{profile.email}</span>
                  </div>
                </div>
              )}

              {profile.phone && (
                <div className="flex items-center gap-3 p-3 bg-[var(--bg-inset)] rounded-xl border border-[var(--border-primary)]">
                  <Phone size={16} className="text-sky-600" />
                  <div>
                    <span className="block text-[8px] uppercase tracking-wider font-semibold font-mono text-[var(--text-muted)]">Số điện thoại</span>
                    <span className="font-semibold text-[var(--text-secondary)] font-mono">{profile.phone}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Posts Subsection */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold text-[var(--text-primary)] uppercase tracking-widest font-mono">Bài đăng của sinh viên</h4>
              {posts.length === 0 ? (
                <div className="py-6 text-center text-[var(--text-muted)] text-xs border border-dashed border-[var(--border-primary)] rounded-2xl italic">
                  Chưa đăng tải bài viết nào.
                </div>
              ) : (
                <div className="grid grid-cols-3 gap-3">
                  {posts.map((post) => (
                    <div 
                      key={post.id} 
                      className="aspect-square bg-[var(--bg-inset)] rounded-xl overflow-hidden relative group border border-[var(--border-primary)]"
                    >
                      {post.imageUrls && post.imageUrls[0] ? (
                        <img src={post.imageUrls[0]} alt="Post item" className="w-full h-full object-cover" />
                      ) : (
                        <div className="p-3 w-full h-full flex items-center justify-center text-[9px] text-[var(--text-muted)] italic line-clamp-4 leading-relaxed">
                          {post.textContent}
                        </div>
                      )}
                      
                      {/* Hover Info Overlay */}
                      <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 flex items-center justify-center gap-3 text-white transition duration-200">
                        <span className="flex items-center gap-1.5 font-bold font-mono text-xs">
                          <Heart size={14} fill="currentColor" /> {post.likes}
                        </span>
                        <span className="flex items-center gap-1.5 font-bold font-mono text-xs">
                          <MessageCircle size={14} /> {post.commentCount}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

          </div>
        )}
      </div>
    </div>
  );
};
