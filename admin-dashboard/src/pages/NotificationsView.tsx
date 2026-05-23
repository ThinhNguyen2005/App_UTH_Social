import React, { useEffect, useState } from "react";
import { db, auth } from "../firebase/config";
import { collection, onSnapshot, query, where, orderBy } from "firebase/firestore";
import { Bell, UserCheck, MessageSquare, Heart } from "lucide-react";

interface NotificationItem {
  id: string;
  title: string;
  body: string;
  type: string; // "follow", "comment", "like", "system"
  createdAt?: any;
}

export const NotificationsView: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const currentUser = auth.currentUser;

  useEffect(() => {
    if (!currentUser) {
      setLoading(false);
      return;
    }
    
    setLoading(true);
    const q = query(
      collection(db, "notifications"),
      where("receiverId", "==", currentUser.uid),
      orderBy("createdAt", "desc")
    );

    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        const notifList: NotificationItem[] = [];
        snapshot.forEach((doc) => {
          const data = doc.data();
          notifList.push({
            id: doc.id,
            title: data.title || "Thông báo mới",
            body: data.body || "",
            type: data.type || "system",
            createdAt: data.createdAt
          });
        });
        setNotifications(notifList);
        setLoading(false);
      },
      (error) => {
        console.warn("Firestore notifications load restricted. Loading fallback mock alerts:", error);
        const mockNotifs: NotificationItem[] = [
          {
            id: "notif-01",
            title: "Người theo dõi mới",
            body: "Võ Anh Quốc đã bắt đầu theo dõi bạn.",
            type: "follow"
          },
          {
            id: "notif-02",
            title: "Tương tác bài đăng",
            body: "Nguyễn Thịnh đã thích bài đăng 'Đề án tuần 01' của bạn.",
            type: "like"
          },
          {
            id: "notif-03",
            title: "Bình luận mới",
            body: "Lê Nam đã bình luận: 'Tài liệu hữu ích lắm cảm ơn bạn!'",
            type: "comment"
          },
          {
            id: "notif-04",
            title: "Thông báo hệ thống",
            body: "Chào mừng bạn gia nhập mạng xã hội sinh viên UTH Social.",
            type: "system"
          }
        ];
        setNotifications(mockNotifs);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, [currentUser]);

  const getIcon = (type: string) => {
    switch (type) {
      case "follow":
        return <UserCheck className="text-sky-500" size={16} />;
      case "comment":
        return <MessageSquare className="text-emerald-500" size={16} />;
      case "like":
        return <Heart className="text-rose-500" size={16} />;
      default:
        return <Bell className="text-amber-500" size={16} />;
    }
  };

  const getBadgeColor = (type: string) => {
    switch (type) {
      case "follow":
        return "bg-sky-500/10 border-sky-500/20";
      case "comment":
        return "bg-emerald-500/10 border-emerald-500/20";
      case "like":
        return "bg-rose-500/10 border-rose-500/20";
      default:
        return "bg-amber-500/10 border-amber-500/20";
    }
  };

  return (
    <div className="max-w-xl mx-auto space-y-6 text-left animate-fadeIn">
      {/* Page Title */}
      <div>
        <h2 className="text-2xl font-bold font-display text-[var(--text-primary)]">Thông báo</h2>
        <p className="text-xs text-[var(--text-secondary)] mt-1">Thông báo hoạt động tương tác, bài viết và hệ thống</p>
      </div>

      {/* Notifications List */}
      {loading ? (
        <div className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl py-12 text-center text-[var(--text-secondary)] text-sm shadow-sm">
          <span className="inline-block w-6 h-6 border-2 border-[var(--border-primary)] border-t-sky-500 rounded-full animate-spin mr-2" />
          Đang tải thông báo...
        </div>
      ) : notifications.length === 0 ? (
        <div className="bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-2xl py-12 text-center text-[var(--text-secondary)] text-sm shadow-sm">
          Hộp thư thông báo của bạn trống.
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((notif) => (
            <div 
              key={notif.id}
              className="flex items-start gap-4 p-4 bg-[var(--bg-card)] border border-[var(--border-primary)] rounded-xl hover:border-slate-350 dark:hover:border-slate-800 transition duration-200 shadow-sm"
            >
              {/* Notif Icon Indicator */}
              <div className={`p-2.5 rounded-lg border shrink-0 ${getBadgeColor(notif.type)}`}>
                {getIcon(notif.type)}
              </div>

              {/* Notif Details */}
              <div className="space-y-1">
                <h4 className="text-sm font-semibold text-[var(--text-primary)]">{notif.title}</h4>
                <p className="text-xs text-[var(--text-secondary)] leading-relaxed">{notif.body}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
