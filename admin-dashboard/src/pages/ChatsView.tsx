import React, { useEffect, useState, useRef } from "react";
import { db, auth } from "../firebase/config";
import { 
  collection, 
  query, 
  where, 
  orderBy, 
  onSnapshot, 
  addDoc, 
  updateDoc, 
  doc, 
  getDoc,
  limit
} from "firebase/firestore";
import { Send, MessageSquare, Circle, ArrowLeft } from "lucide-react";

interface ChatThread {
  id: string;
  participants: string[];
  lastMessage: string;
  lastSenderId: string;
  timestamp: any;
}

interface MessageItem {
  id: string;
  senderId: string;
  text: string;
  timestamp: any;
  seen: boolean;
}

interface ChatsViewProps {
  selectedChatId: string | null;
  setSelectedChatId: (chatId: string | null) => void;
  onViewProfile?: (userId: string) => void;
}

export const ChatsView: React.FC<ChatsViewProps> = ({ 
  selectedChatId, 
  setSelectedChatId,
  onViewProfile
}) => {
  const [threads, setThreads] = useState<ChatThread[]>([]);
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [userProfiles, setUserProfiles] = useState<Record<string, { name: string; avatarUrl: string }>>({});
  const [loadingThreads, setLoadingThreads] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [newMessageText, setNewMessageText] = useState("");
  const [isSending, setIsSending] = useState(false);

  const currentUser = auth.currentUser;
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 1. Fetch Chat Threads
  useEffect(() => {
    if (!currentUser) return;
    setLoadingThreads(true);

    const q = query(
      collection(db, "chats"),
      where("participants", "array-contains", currentUser.uid)
    );

    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        const threadList: ChatThread[] = [];
        snapshot.forEach((doc) => {
          const data = doc.data();
          threadList.push({
            id: doc.id,
            participants: data.participants || [],
            lastMessage: data.lastMessage || "",
            lastSenderId: data.lastSenderId || "",
            timestamp: data.timestamp
          });
        });
        
        // Sort threads locally by timestamp (descending)
        threadList.sort((a, b) => {
          const t1 = a.timestamp?.seconds || a.timestamp?.toMillis?.() || 0;
          const t2 = b.timestamp?.seconds || b.timestamp?.toMillis?.() || 0;
          return t2 - t1;
        });

        setThreads(threadList);
        setLoadingThreads(false);
      },
      (error) => {
        console.error("Firestore chats subscription error, using mock:", error);
        // Fallback mockup threads
        const mockThreads: ChatThread[] = [
          {
            id: "vvrTdGbamOPz8wEkSV2kwgMJeG43_jUdmXzgPTSWTw4CH0J7oChxuf362",
            participants: ["vvrTdGbamOPz8wEkSV2kwgMJeG43", "jUdmXzgPTSWTw4CH0J7oChxuf362"],
            lastMessage: "Xin chào! Mình cần hỏi về tài liệu môn học.",
            lastSenderId: "jUdmXzgPTSWTw4CH0J7oChxuf362",
            timestamp: { seconds: Date.now() / 1000 }
          }
        ];
        setThreads(mockThreads);
        setLoadingThreads(false);
      }
    );

    return () => unsubscribe();
  }, [currentUser]);

  // 2. Fetch User Profiles for thread participants
  useEffect(() => {
    threads.forEach((thread) => {
      const otherUid = thread.participants.find(uid => uid !== currentUser?.uid);
      if (otherUid && !userProfiles[otherUid]) {
        // Fetch metadata
        const fetchProfile = async () => {
          try {
            const userSnap = await getDoc(doc(db, "users", otherUid));
            if (userSnap.exists()) {
              const data = userSnap.data();
              setUserProfiles(prev => ({
                ...prev,
                [otherUid]: {
                  name: data.username || data.name || data.displayName || "Sinh viên UTH",
                  avatarUrl: data.avatarUrl || data.photoURL || ""
                }
              }));
            } else {
              setUserProfiles(prev => ({
                ...prev,
                [otherUid]: {
                  name: `Sinh viên (${otherUid.substring(0, 6)})`,
                  avatarUrl: ""
                }
              }));
            }
          } catch (e) {
            console.error("Error fetching other user profile:", e);
          }
        };
        fetchProfile();
      }
    });
  }, [threads, userProfiles, currentUser]);

  // 3. Listen to messages for the active selected chat
  useEffect(() => {
    if (!selectedChatId) {
      setMessages([]);
      return;
    }

    setLoadingMessages(true);
    const msgQuery = query(
      collection(db, "chats", selectedChatId, "messages"),
      orderBy("timestamp", "asc"),
      limit(100)
    );

    const unsubscribe = onSnapshot(
      msgQuery,
      (snapshot) => {
        const msgList: MessageItem[] = [];
        snapshot.forEach((doc) => {
          const data = doc.data();
          msgList.push({
            id: doc.id,
            senderId: data.senderId || "",
            text: data.text || "",
            timestamp: data.timestamp,
            seen: data.seen || false
          });
        });
        setMessages(msgList);
        setLoadingMessages(false);
      },
      (error) => {
        console.error("Firestore messages load error:", error);
        // Fallback mockup messages
        const mockMsgs: MessageItem[] = [
          {
            id: "m-01",
            senderId: "jUdmXzgPTSWTw4CH0J7oChxuf362",
            text: "Xin chào! Mình cần hỏi về tài liệu môn học.",
            timestamp: { seconds: Date.now() / 1000 - 3600 },
            seen: true
          },
          {
            id: "m-02",
            senderId: currentUser?.uid || "",
            text: "Chào bạn! Giáo trình đó mình vẫn còn nhé. Bạn học cơ sở nào?",
            timestamp: { seconds: Date.now() / 1000 - 1800 },
            seen: true
          }
        ];
        setMessages(mockMsgs);
        setLoadingMessages(false);
      }
    );

    return () => unsubscribe();
  }, [selectedChatId, currentUser]);

  // 4. Scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessageText.trim() || !selectedChatId || !currentUser) return;

    setIsSending(true);
    const textToSend = newMessageText.trim();
    setNewMessageText("");

    try {
      // 1. Add document to messages subcollection
      await addDoc(collection(db, "chats", selectedChatId, "messages"), {
        senderId: currentUser.uid,
        text: textToSend,
        timestamp: new Date(),
        seen: false
      });

      // 2. Update chat parent document metadata
      await updateDoc(doc(db, "chats", selectedChatId), {
        lastMessage: textToSend,
        lastSenderId: currentUser.uid,
        timestamp: new Date()
      });
    } catch (err) {
      console.error("Error sending message:", err);
      alert("Không thể gửi tin nhắn. Có lỗi kết nối Firestore.");
    } finally {
      setIsSending(false);
    }
  };

  const getOtherParticipantUid = (thread: ChatThread) => {
    return thread.participants.find(uid => uid !== currentUser?.uid) || "";
  };

  const activeOtherUid = selectedChatId 
    ? selectedChatId.split("_").find(uid => uid !== currentUser?.uid) || ""
    : "";

  const activeProfile = userProfiles[activeOtherUid];

  return (
    <div className="h-[calc(100vh-6rem)] md:h-[calc(100vh-4rem)] border border-[var(--border-primary)] rounded-2xl bg-[var(--bg-card)] overflow-hidden shadow-sm flex flex-row animate-fadeIn text-left">
      
      {/* ========================================================================= */}
      {/* 1. LEFT COLUMN: CHATS LIST */}
      {/* ========================================================================= */}
      <div className={`w-full md:w-[350px] border-r border-[var(--border-primary)] flex flex-col ${selectedChatId ? "hidden md:flex" : "flex"}`}>
        
        {/* Chats List Header */}
        <div className="p-4 border-b border-[var(--border-secondary)] bg-[var(--bg-inset)]/10">
          <h3 className="font-bold text-sm text-[var(--text-primary)]">Hộp thư tin nhắn</h3>
        </div>

        {/* Threads List */}
        <div className="flex-1 overflow-y-auto divide-y divide-[var(--border-secondary)]">
          {loadingThreads ? (
            <div className="py-12 text-center text-[var(--text-secondary)] text-xs">
              <span className="inline-block w-5 h-5 border-2 border-[var(--border-primary)] border-t-sky-500 rounded-full animate-spin mr-2" />
              Đang tải hộp thư...
            </div>
          ) : threads.length === 0 ? (
            <div className="py-12 text-center text-[var(--text-muted)] text-xs italic p-4">
              Hộp thư trống. Bắt đầu nhắn tin từ trang cá nhân của bạn học khác.
            </div>
          ) : (
            threads.map((thread) => {
              const otherUid = getOtherParticipantUid(thread);
              const profile = userProfiles[otherUid];
              const isSelected = selectedChatId === thread.id;
              const isUnread = thread.lastSenderId !== currentUser?.uid && thread.lastMessage; // Mock check for unread

              return (
                <div
                  key={thread.id}
                  onClick={() => setSelectedChatId(thread.id)}
                  className={`p-3.5 flex items-center justify-between cursor-pointer transition ${
                    isSelected 
                      ? "bg-slate-50 dark:bg-slate-900 border-l-4 border-sky-500" 
                      : "hover:bg-[var(--bg-inset)]/40"
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0 flex-1">
                    <div className="w-10 h-10 rounded-full overflow-hidden border border-[var(--border-primary)] bg-[var(--bg-inset)] flex items-center justify-center font-bold text-[var(--text-secondary)] text-sm">
                      {profile?.avatarUrl ? (
                        <img src={profile.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                      ) : (
                        profile?.name?.[0]?.toUpperCase() || "S"
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="text-xs font-bold text-[var(--text-primary)] truncate flex items-center gap-1.5">
                        {profile?.name || "Sinh viên UTH"}
                        {isUnread && <Circle size={6} className="fill-sky-500 text-sky-500 shrink-0" />}
                      </h4>
                      <p className={`text-[10px] truncate mt-0.5 ${isUnread ? "font-bold text-[var(--text-primary)]" : "text-[var(--text-muted)]"}`}>
                        {thread.lastMessage || "Chưa có tin nhắn nào"}
                      </p>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 2. RIGHT COLUMN: MESSAGE LIST & INPUT */}
      {/* ========================================================================= */}
      <div className={`flex-1 flex flex-col bg-[var(--bg-inset)]/5 ${!selectedChatId ? "hidden md:flex" : "flex"}`}>
        {selectedChatId ? (
          <>
            {/* Message Pane Header */}
            <div className="p-4 border-b border-[var(--border-secondary)] bg-[var(--bg-card)] flex items-center gap-3">
              {/* Back button on mobile */}
              <button 
                onClick={() => setSelectedChatId(null)}
                className="p-1.5 md:hidden hover:bg-[var(--bg-inset)] rounded-lg text-[var(--text-secondary)]"
              >
                <ArrowLeft size={16} />
              </button>

              <div className="w-9 h-9 rounded-full overflow-hidden border border-[var(--border-primary)] bg-[var(--bg-inset)] flex items-center justify-center font-bold text-[var(--text-secondary)] text-xs">
                {activeProfile?.avatarUrl ? (
                  <img src={activeProfile.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                ) : (
                  activeProfile?.name?.[0]?.toUpperCase() || "S"
                )}
              </div>
              <div>
                <h4 
                  onClick={() => onViewProfile && activeOtherUid && onViewProfile(activeOtherUid)}
                  className="text-xs font-bold text-[var(--text-primary)] hover:underline cursor-pointer"
                >
                  {activeProfile?.name || "Sinh viên UTH"}
                </h4>
                <span className="text-[9px] text-[var(--text-muted)]">Hoạt động trên UTH Social</span>
              </div>
            </div>

            {/* Messages Scroll Container */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3.5 bg-[var(--bg-card)]">
              {loadingMessages ? (
                <div className="py-8 text-center text-[var(--text-secondary)] text-xs">
                  Đang tải tin nhắn...
                </div>
              ) : messages.length === 0 ? (
                <div className="py-12 text-center text-[var(--text-muted)] text-xs italic">
                  Chưa có tin nhắn. Hãy bắt đầu chào hỏi bạn học!
                </div>
              ) : (
                messages.map((message) => {
                  const isMine = message.senderId === currentUser?.uid;

                  return (
                    <div 
                      key={message.id}
                      className={`flex ${isMine ? "justify-end" : "justify-start"} animate-fadeIn`}
                    >
                      <div 
                        className={`max-w-[70%] rounded-2xl px-4 py-2.5 text-xs ${
                          isMine 
                            ? "bg-sky-600 text-white rounded-br-none" 
                            : "bg-slate-100 dark:bg-slate-800 text-[var(--text-secondary)] rounded-bl-none"
                        }`}
                      >
                        <p className="leading-relaxed whitespace-pre-wrap">{message.text}</p>
                      </div>
                    </div>
                  );
                })
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input Bar */}
            <form 
              onSubmit={handleSendMessage}
              className="p-4 border-t border-[var(--border-secondary)] bg-[var(--bg-card)] flex gap-3"
            >
              <input
                type="text"
                value={newMessageText}
                onChange={(e) => setNewMessageText(e.target.value)}
                placeholder="Nhập tin nhắn..."
                className="flex-1 px-4 py-2.5 bg-[var(--bg-inset)] border border-[var(--border-primary)] rounded-xl text-xs text-[var(--text-primary)] placeholder-[var(--text-muted)] outline-none focus:border-sky-500 transition"
              />
              <button
                type="submit"
                disabled={!newMessageText.trim() || isSending}
                className="p-2.5 bg-sky-600 hover:bg-sky-500 text-white rounded-xl transition disabled:opacity-40"
              >
                <Send size={16} />
              </button>
            </form>
          </>
        ) : (
          /* Empty Inbox Panel */
          <div className="flex-1 flex flex-col items-center justify-center p-6 text-center">
            <div className="p-4 bg-sky-500/10 border border-sky-500/20 text-sky-600 rounded-full mb-3 animate-pulse">
              <MessageSquare size={36} />
            </div>
            <h4 className="text-sm font-bold text-[var(--text-primary)]">Tin nhắn của bạn</h4>
            <p className="text-xs text-[var(--text-secondary)] mt-1.5 max-w-[280px] leading-relaxed">
              Gửi tin nhắn riêng tư cho bạn học hoặc người bán hàng trong trường. Hãy bắt đầu từ việc xem trang cá nhân của họ.
            </p>
          </div>
        )}
      </div>

    </div>
  );
};
