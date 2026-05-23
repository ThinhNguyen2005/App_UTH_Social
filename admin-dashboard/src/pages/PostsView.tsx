import React, { useEffect, useState } from "react";
import { db, auth } from "../firebase/config";
import { 
  collection, 
  onSnapshot, 
  doc, 
  updateDoc, 
  arrayUnion, 
  arrayRemove, 
  query, 
  orderBy,
  addDoc 
} from "firebase/firestore";
import { Heart, Bookmark, MoreHorizontal, MessageCircle } from "lucide-react";

interface PostItem {
  id: string;
  userId: string;
  username: string;
  userAvatarUrl: string;
  textContent: string;
  imageUrls: string[];
  category: string;
  likes: number;
  commentCount: number;
  likedBy: string[];
  timestamp?: any;
}

interface PostsViewProps {
  onViewProfile?: (userId: string) => void;
}

export const PostsView: React.FC<PostsViewProps> = ({ onViewProfile }) => {
  const [posts, setPosts] = useState<PostItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [commentingPostId, setCommentingPostId] = useState<string | null>(null);
  const [newComment, setNewComment] = useState("");
  const [postComments, setPostComments] = useState<{[key: string]: any[]}>({});
  
  const currentUser = auth.currentUser;

  // Mock Stories Data (Instagram Web Style)
  const stories = [
    { id: 1, name: "Võ Quốc", avatar: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80", hasStory: true },
    { id: 2, name: "Thịnh Nguyễn", avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80", hasStory: true },
    { id: 3, name: "Mỹ Linh", avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80", hasStory: true },
    { id: 4, name: "Nam IT", avatar: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=150&q=80", hasStory: false },
    { id: 5, name: "Đoàn Hội", avatar: "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=150&q=80", hasStory: true },
    { id: 6, name: "Hải Nam", avatar: "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=150&q=80", hasStory: false }
  ];

  // Mock Suggestions Data
  const suggestions = [
    { id: "s-1", name: "Nguyễn Hoàng Nam", rel: "Khoa Công nghệ thông tin", avatar: "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=80&q=80" },
    { id: "s-2", name: "Trần Thanh Trúc", rel: "Sinh viên năm 2", avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=80&q=80" },
    { id: "s-3", name: "Phạm Minh Tuấn", rel: "Khoa Điện - Điện tử", avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=80&q=80" }
  ];

  useEffect(() => {
    setLoading(true);
    const q = query(collection(db, "posts"), orderBy("timestamp", "desc"));
    
    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        const postsList: PostItem[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          postsList.push({
            id: docSnap.id,
            userId: data.userId || "",
            username: data.username || "Sinh viên UTH",
            userAvatarUrl: data.userAvatarUrl || "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da",
            textContent: data.textContent || "",
            imageUrls: data.imageUrls || (data.imageUrl ? [data.imageUrl] : []),
            category: data.category || "Học tập",
            likes: data.likes || 0,
            commentCount: data.commentCount || 0,
            likedBy: data.likedBy || [],
            timestamp: data.timestamp
          });
        });
        setPosts(postsList);
        setLoading(false);
      },
      (error) => {
        console.warn("Firestore posts load restricted. Loading fallback mockup feed:", error);
        const mockFeed: PostItem[] = [
          {
            id: "post-01",
            userId: "uid-01",
            username: "31. Võ Anh Quốc",
            userAvatarUrl: "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da",
            textContent: "Còn gì đẹp hơn UTH trong một buổi chiều hoàng hôn! Chúc mọi người ôn thi học kỳ thật tốt nhé 📚",
            imageUrls: ["https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/media__1779525370800.png?alt=media&token=e11a3d3c-99d9-4824-81d3-63a152fb58f7"],
            category: "Học tập",
            likes: 18,
            commentCount: 2,
            likedBy: []
          },
          {
            id: "post-02",
            userId: "uid-02",
            username: "Nguyễn Thịnh",
            userAvatarUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80",
            textContent: "Giải bóng đá UTH Cup chính thức khai mạc tuần này! Mọi người ra sân cổ vũ cho đội khoa CNTT nhé ⚽🏆",
            imageUrls: [],
            category: "Xã hội",
            likes: 34,
            commentCount: 5,
            likedBy: []
          }
        ];
        setPosts(mockFeed);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, []);

  const handleLike = async (postId: string, likedBy: string[], currentLikes: number) => {
    if (!currentUser) {
      alert("Bạn cần đăng nhập để thả tim bài viết!");
      return;
    }
    const isLiked = likedBy.includes(currentUser.uid);
    const postRef = doc(db, "posts", postId);

    // Update local state immediately for instant feedback
    setPosts(prev => prev.map(p => {
      if (p.id === postId) {
        const nextLikedBy = isLiked ? p.likedBy.filter(uid => uid !== currentUser.uid) : [...p.likedBy, currentUser.uid];
        const nextLikes = isLiked ? Math.max(0, p.likes - 1) : p.likes + 1;
        return { ...p, likedBy: nextLikedBy, likes: nextLikes };
      }
      return p;
    }));

    try {
      if (isLiked) {
        await updateDoc(postRef, {
          likedBy: arrayRemove(currentUser.uid),
          likes: Math.max(0, currentLikes - 1)
        });
      } else {
        await updateDoc(postRef, {
          likedBy: arrayUnion(currentUser.uid),
          likes: currentLikes + 1
        });
      }
    } catch (e) {
      console.error("Error toggling like:", e);
    }
  };

  const handleShowComments = (postId: string) => {
    if (commentingPostId === postId) {
      setCommentingPostId(null);
      return;
    }
    setCommentingPostId(postId);

    // Real-time listener for comments of this post
    const commentsRef = collection(db, "posts", postId, "comments");
    onSnapshot(commentsRef, (snap) => {
      const list: any[] = [];
      snap.forEach(d => list.push({ id: d.id, ...d.data() }));
      setPostComments(prev => ({ ...prev, [postId]: list }));
    }, () => {
      // Mock comments fallback
      const mockComments = [
        { id: "c-1", username: "Thanh Trúc", textContent: "Ảnh đẹp quá anh ơi!" },
        { id: "c-2", username: "Hoàng Nam", textContent: "Khoa CNTT vô địch!" }
      ];
      setPostComments(prev => ({ ...prev, [postId]: mockComments }));
    });
  };

  const handleAddComment = async (postId: string) => {
    if (!newComment.trim() || !currentUser) return;
    try {
      const commentsRef = collection(db, "posts", postId, "comments");
      await addDoc(commentsRef, {
        userId: currentUser.uid,
        username: currentUser.displayName || currentUser.email?.split("@")[0] || "Sinh viên UTH",
        textContent: newComment.trim(),
        timestamp: new Date()
      });

      // Update comment count on post
      const postRef = doc(db, "posts", postId);
      const post = posts.find(p => p.id === postId);
      if (post) {
        await updateDoc(postRef, {
          commentCount: (post.commentCount || 0) + 1
        });
      }
      setNewComment("");
    } catch (err) {
      console.error("Error adding comment:", err);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 flex gap-8 justify-center relative items-start">
      
      {/* ========================================================================= */}
      {/* 1. LEFT / MIDDLE COLUMN: FEED STREAM */}
      {/* ========================================================================= */}
      <div className="w-full max-w-[470px] space-y-4 shrink-0">
        
        {/* Stories Horizontal Tray */}
        <div className="flex gap-4 p-4 bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl overflow-x-auto scrollbar-none shadow-sm">
          {stories.map((story) => (
            <div key={story.id} className="flex flex-col items-center gap-1.5 shrink-0 cursor-pointer">
              <div className={`p-[2px] rounded-full ${story.hasStory ? "bg-gradient-to-tr from-sky-500 to-indigo-600" : "bg-[var(--border-primary)]"}`}>
                <div className="w-12 h-12 rounded-full border-2 border-[var(--bg-card)] overflow-hidden">
                  <img src={story.avatar} alt={story.name} className="w-full h-full object-cover" />
                </div>
              </div>
              <span className="text-[10px] text-[var(--text-secondary)] font-medium max-w-[56px] truncate">{story.name}</span>
            </div>
          ))}
        </div>

        {/* Feed Cards Stream */}
        {loading ? (
          <div className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl py-16 text-center text-[var(--text-muted)] text-sm shadow-sm">
            <span className="inline-block w-6 h-6 border-2 border-[var(--border-primary)] border-t-sky-500 rounded-full animate-spin mr-2" />
            Đang tải bảng tin sinh viên...
          </div>
        ) : posts.length === 0 ? (
          <div className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl py-16 text-center text-[var(--text-muted)] text-sm shadow-sm">
            Chưa có bài đăng nào trên bảng tin.
          </div>
        ) : (
          posts.map((post) => {
            const isLiked = currentUser ? post.likedBy.includes(currentUser.uid) : false;
            const comments = postComments[post.id] || [];

            return (
              <div 
                key={post.id}
                className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl overflow-hidden hover:border-slate-300 dark:hover:border-slate-800 transition duration-300 shadow-sm text-left"
              >
                {/* Post Header */}
                <div className="p-3.5 flex items-center justify-between border-b border-[var(--border-secondary)] bg-[var(--bg-inset)]/10">
                  <div className="flex items-center gap-2.5">
                    <div 
                      onClick={() => onViewProfile && post.userId && onViewProfile(post.userId)}
                      className={`w-8 h-8 rounded-full border border-[var(--border-primary)] bg-[var(--bg-inset)] overflow-hidden flex items-center justify-center font-bold text-[var(--text-secondary)] text-xs ${onViewProfile ? "cursor-pointer" : ""}`}
                    >
                      {post.userAvatarUrl ? (
                        <img src={post.userAvatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                      ) : (
                        post.username?.[0]?.toUpperCase() || "S"
                      )}
                    </div>
                    <div>
                      <h4 
                        onClick={() => onViewProfile && post.userId && onViewProfile(post.userId)}
                        className={`text-xs font-bold text-[var(--text-primary)] ${onViewProfile ? "hover:underline cursor-pointer" : ""}`}
                      >
                        {post.username}
                      </h4>
                      <span className="text-[9px] text-sky-600 font-semibold font-mono tracking-wider">{post.category}</span>
                    </div>
                  </div>
                  <button className="text-[var(--text-muted)] hover:text-[var(--text-secondary)]">
                    <MoreHorizontal size={16} />
                  </button>
                </div>

                {/* Post Media (Main Image) */}
                {post.imageUrls && post.imageUrls.length > 0 && post.imageUrls[0] && (
                  <div className="bg-[var(--bg-inset)] overflow-hidden aspect-square border-b border-[var(--border-primary)] flex items-center justify-center">
                    <img 
                      src={post.imageUrls[0]} 
                      alt="Post Attachment" 
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        e.currentTarget.src = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=800&q=80";
                      }}
                    />
                  </div>
                )}

                {/* Card Interaction Area */}
                <div className="p-4 space-y-3">
                  
                  {/* Action Icons */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <button
                        onClick={() => handleLike(post.id, post.likedBy, post.likes)}
                        className={`transition hover:scale-110 ${isLiked ? "text-rose-500" : "text-[var(--text-secondary)] hover:text-[var(--text-primary)]"}`}
                      >
                        <Heart size={20} fill={isLiked ? "currentColor" : "none"} />
                      </button>

                      <button
                        onClick={() => handleShowComments(post.id)}
                        className="text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition hover:scale-110"
                      >
                        <MessageCircle size={20} />
                      </button>
                    </div>

                    <button className="text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition">
                      <Bookmark size={20} />
                    </button>
                  </div>

                  {/* Likes count */}
                  <div className="text-xs font-bold text-[var(--text-primary)]">
                    {post.likes} lượt thích
                  </div>

                  {/* Caption Text */}
                  <div className="text-xs text-[var(--text-secondary)] leading-relaxed">
                    <span 
                      onClick={() => onViewProfile && post.userId && onViewProfile(post.userId)}
                      className={`font-bold text-[var(--text-primary)] mr-2 ${onViewProfile ? "hover:underline cursor-pointer" : ""}`}
                    >
                      {post.username}
                    </span>
                    <span className="whitespace-pre-wrap">{post.textContent}</span>
                  </div>

                  {/* Comments count / toggle */}
                  {post.commentCount > 0 && (
                    <button
                      onClick={() => handleShowComments(post.id)}
                      className="text-[10px] text-[var(--text-muted)] hover:text-[var(--text-secondary)] font-semibold transition"
                    >
                      Xem tất cả {post.commentCount} bình luận
                    </button>
                  )}

                  {/* Comment Drawer / Section */}
                  {commentingPostId === post.id && (
                    <div className="space-y-2.5 pt-3 border-t border-[var(--border-secondary)] animate-fadeIn">
                      <div className="space-y-2 max-h-[160px] overflow-y-auto pr-1">
                        {comments.length === 0 ? (
                          <p className="text-[10px] text-[var(--text-muted)] italic">Chưa có bình luận nào.</p>
                        ) : (
                          comments.map((comment) => (
                            <div key={comment.id} className="text-xs">
                              <span 
                                onClick={() => onViewProfile && comment.userId && onViewProfile(comment.userId)}
                                className={`font-bold text-[var(--text-primary)] mr-1.5 ${onViewProfile && comment.userId ? "hover:underline cursor-pointer" : ""}`}
                              >
                                {comment.username}
                              </span>
                              <span className="text-[var(--text-secondary)]">{comment.textContent}</span>
                            </div>
                          ))
                        )}
                      </div>
                      
                      {/* Form to submit comment */}
                      <div className="flex gap-2 bg-[var(--bg-inset)] p-2 rounded-xl border border-[var(--border-primary)]">
                        <input
                          type="text"
                          value={newComment}
                          onChange={(e) => setNewComment(e.target.value)}
                          placeholder="Thêm bình luận..."
                          className="flex-1 bg-transparent border-0 outline-none text-xs text-[var(--text-primary)] placeholder-[var(--text-muted)]"
                        />
                        <button
                          onClick={() => handleAddComment(post.id)}
                          disabled={!newComment.trim()}
                          className="text-sky-600 hover:text-sky-500 text-xs font-bold disabled:opacity-40"
                        >
                          Đăng
                        </button>
                      </div>
                    </div>
                  )}

                </div>
              </div>
            );
          })
        )}
      </div>

      {/* ========================================================================= */}
      {/* 2. RIGHT COLUMN: DESKTOP RECOMMENDATION PANEL (Instagram Style) */}
      {/* ========================================================================= */}
      <aside className="hidden lg:block w-[320px] sticky top-8 space-y-6 text-left shrink-0">
        
        {/* User Card */}
        {currentUser && (
          <div className="flex items-center justify-between p-3.5 bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl shadow-sm">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-sky-500 to-indigo-600 border border-[var(--border-primary)] flex items-center justify-center font-bold text-white uppercase text-sm">
                {currentUser.email?.[0].toUpperCase() || "S"}
              </div>
              <div className="min-w-0">
                <h4 className="text-xs font-bold text-[var(--text-primary)] truncate">
                  {currentUser.displayName || currentUser.email?.split("@")[0] || "Sinh viên UTH"}
                </h4>
                <p className="text-[10px] text-[var(--text-muted)] truncate font-mono">{currentUser.email}</p>
              </div>
            </div>
            <span className="text-[8px] px-2 py-0.5 rounded bg-[var(--bg-inset)] text-sky-600 font-bold border border-[var(--border-primary)] uppercase tracking-widest font-mono">
              Sinh viên
            </span>
          </div>
        )}

        {/* Suggestion list */}
        <div className="space-y-4">
          <div className="flex justify-between items-center px-1">
            <span className="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-widest font-mono">Gợi ý cho bạn</span>
            <button className="text-[10px] font-bold text-[var(--text-muted)] hover:text-[var(--text-secondary)] font-mono">Xem tất cả</button>
          </div>

          <div className="space-y-3">
            {suggestions.map((s) => (
              <div key={s.id} className="flex items-center justify-between px-1">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full overflow-hidden border border-[var(--border-primary)]">
                    <img src={s.avatar} alt={s.name} className="w-full h-full object-cover" />
                  </div>
                  <div>
                    <h5 className="text-[11px] font-bold text-[var(--text-primary)] hover:underline cursor-pointer">{s.name}</h5>
                    <p className="text-[9px] text-[var(--text-muted)] truncate max-w-[150px]">{s.rel}</p>
                  </div>
                </div>
                <button className="text-[10px] font-bold text-sky-600 hover:text-sky-500 transition font-mono">Theo dõi</button>
              </div>
            ))}
          </div>
        </div>

        {/* Visual Footer */}
        <div className="text-[9px] text-[var(--text-muted)] px-1 leading-relaxed">
          <p>© 2026 UTH Social Companion Web.</p>
          <p className="mt-1">Built with high fidelity and design consistency.</p>
        </div>

      </aside>

    </div>
  );
};
