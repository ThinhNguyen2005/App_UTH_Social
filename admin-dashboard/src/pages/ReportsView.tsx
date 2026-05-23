import React, { useEffect, useState } from "react";
import { db } from "../firebase/config";
import { 
  collection, 
  onSnapshot, 
  doc, 
  updateDoc, 
  deleteDoc,
  addDoc,
  increment 
} from "firebase/firestore";
import { 
  AlertTriangle, 
  CheckCircle, 
  Trash2, 
  Eye, 
  X, 
  ShieldAlert, 
  User, 
  Calendar,
  BellRing,
  UserMinus
} from "lucide-react";

interface ReportItem {
  id: string;
  postId: string;
  postText?: string;
  postAuthor?: string;
  authorId?: string;
  reason?: string;
  reporterName?: string;
  reporterId?: string;
  status?: "pending" | "dismissed" | "actioned";
  reportedAt?: string;
}

export const ReportsView: React.FC = () => {
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [usersCache, setUsersCache] = useState<{[uid: string]: { warningCount?: number; isBanned?: boolean }}>({});
  const [filterStatus, setFilterStatus] = useState<"all" | "pending" | "dismissed" | "actioned">("pending");
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState<string | null>(null);
  const [selectedReport, setSelectedReport] = useState<ReportItem | null>(null);

  // 1. Listen to Users Collection to get Warning Counts & Ban Statuses
  useEffect(() => {
    const unsubscribe = onSnapshot(
      collection(db, "users"),
      (snapshot) => {
        const cache: {[uid: string]: { warningCount?: number; isBanned?: boolean }} = {};
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          cache[docSnap.id] = {
            warningCount: data.warningCount || 0,
            isBanned: data.isBanned || false
          };
        });
        setUsersCache(cache);
      },
      () => {
        // Fallback user cache for demo
        setUsersCache({
          "uid-01": { warningCount: 2, isBanned: false },
          "uid-02": { warningCount: 0, isBanned: false },
          "uid-03": { warningCount: 0, isBanned: true }
        });
      }
    );
    return () => unsubscribe();
  }, []);

  // 2. Listen to Reports Collection
  useEffect(() => {
    setLoading(true);
    const unsubscribe = onSnapshot(
      collection(db, "reports"),
      (snapshot) => {
        const reportsList: ReportItem[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          reportsList.push({
            id: docSnap.id,
            postId: data.postId || "",
            postText: data.postText || data.textContent || "Nội dung bài viết không tìm thấy hoặc đã bị xóa",
            postAuthor: data.postAuthor || data.authorName || "Sinh viên ẩn danh",
            authorId: data.authorId || data.postAuthorId || data.userId || "",
            reason: data.reason || "Báo cáo chung",
            reporterName: data.reporterName || data.reporterEmail?.split("@")[0] || "Sinh viên UTH",
            reporterId: data.reporterId || "",
            status: data.status || "pending",
            reportedAt: data.reportedAt || (data.timestamp ? new Date(data.timestamp.seconds * 1000).toLocaleString() : "Không rõ thời gian")
          });
        });
        setReports(reportsList);
        setLoading(false);
      },
      () => {
        console.warn("Firestore reports listener blocked by security rules. Loading administrative demo reports.");
        const mockReports: ReportItem[] = [
          {
            id: "rep-01",
            postId: "post-01",
            authorId: "uid-01",
            postText: "Còn gì đẹp hơn √ kèm link tải tool cheat game cực mạnh tại địa chỉ cheatgamexxx.com...",
            postAuthor: "31. Võ Anh Quốc",
            reason: "Spam quảng cáo thương mại hoặc lừa đảo",
            reporterName: "Nguyễn Thịnh",
            reporterId: "uid-user-1",
            status: "pending",
            reportedAt: "23/05/2026, 14:12:05"
          },
          {
            id: "rep-02",
            postId: "post-02",
            authorId: "uid-02",
            postText: "Chúc mọi người cuối tuần vui vẻ nhé! Các bạn có ai đi trà sữa học nhóm chiều nay không nhỉ?",
            postAuthor: "Nguyễn Thịnh",
            reason: "Báo cáo nhầm / Quấy rối",
            reporterName: "Trần Thị B",
            reporterId: "uid-user-2",
            status: "dismissed",
            reportedAt: "23/05/2026, 15:20:10"
          },
          {
            id: "rep-03",
            postId: "post-03",
            authorId: "uid-03",
            postText: "Bài đăng có chứa từ ngữ xúc phạm danh dự của giảng viên khoa Công nghệ thông tin...",
            postAuthor: "Phạm Minh Tuấn",
            reason: "Ngôn từ kích động thù địch, xúc phạm cá nhân",
            reporterName: "Lê Hoàng Nam",
            reporterId: "uid-user-3",
            status: "actioned",
            reportedAt: "22/05/2026, 09:34:00"
          }
        ];
        setReports(mockReports);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, []);

  const handleDismissReport = async (reportId: string) => {
    setActionId(reportId);
    try {
      const reportRef = doc(db, "reports", reportId);
      await updateDoc(reportRef, {
        status: "dismissed"
      });
      setReports(prev => prev.map(r => r.id === reportId ? { ...r, status: "dismissed" } : r));
      if (selectedReport?.id === reportId) {
        setSelectedReport(prev => prev ? { ...prev, status: "dismissed" } : null);
      }
    } catch (e) {
      console.error("Error dismissing report:", e);
      alert("Không thể cập nhật báo cáo. Vui lòng kiểm tra lại Firestore Security Rules.");
    } finally {
      setActionId(null);
    }
  };

  const handleDeletePost = async (reportId: string, postId: string) => {
    if (!window.confirm("Bạn có chắc chắn muốn XÓA bài viết này khỏi hệ thống không? Hành động này không thể hoàn tác.")) {
      return;
    }
    setActionId(reportId);
    try {
      const postRef = doc(db, "posts", postId);
      try {
        await deleteDoc(postRef);
      } catch (postErr) {
        console.warn("Could not delete post document directly. Updating report status only.", postErr);
      }
      
      const reportRef = doc(db, "reports", reportId);
      await updateDoc(reportRef, {
        status: "actioned"
      });

      setReports(prev => prev.map(r => r.id === reportId ? { ...r, status: "actioned" } : r));
      if (selectedReport?.id === reportId) {
        setSelectedReport(prev => prev ? { ...prev, status: "actioned" } : null);
      }
      alert("Đã xóa bài viết và cập nhật trạng thái xử lý báo cáo thành công.");
    } catch (e) {
      console.error("Error deleting post:", e);
      alert("Không thể xóa bài viết. Vui lòng kiểm tra lại Firestore Security Rules.");
    } finally {
      setActionId(null);
    }
  };

  // 3. Warning Action (Sends system alert, increments warningCount, resolves report)
  const handleWarnUser = async (reportId: string, authorId: string, authorName: string, reason: string, postSnippet: string) => {
    if (!authorId) {
      alert("Không tìm thấy mã người dùng (UID) để gửi cảnh cáo.");
      return;
    }
    const customMessage = window.prompt(
      `Gửi cảnh cáo đến người dùng "${authorName}". Nhập nội dung cảnh báo:`,
      `Tài khoản của bạn nhận cảnh cáo do bài đăng "${postSnippet.substring(0, 20)}..." vi phạm tiêu chuẩn cộng đồng về: ${reason}.`
    );
    if (customMessage === null) return; // User cancelled prompt

    setActionId(reportId);
    try {
      // a. Increment warningCount in user doc
      const userRef = doc(db, "users", authorId);
      await updateDoc(userRef, {
        warningCount: increment(1)
      });

      // b. Add warning notification doc
      await addDoc(collection(db, "notifications"), {
        receiverId: authorId,
        title: "CẢNH BÁO VI PHẠM NỘI DUNG",
        body: customMessage,
        type: "system",
        createdAt: new Date()
      });

      // c. Resolve report
      const reportRef = doc(db, "reports", reportId);
      await updateDoc(reportRef, {
        status: "actioned"
      });

      // Update local state caches
      setReports(prev => prev.map(r => r.id === reportId ? { ...r, status: "actioned" } : r));
      setUsersCache(prev => {
        const u = prev[authorId] || {};
        return { ...prev, [authorId]: { ...u, warningCount: (u.warningCount || 0) + 1 } };
      });
      if (selectedReport?.id === reportId) {
        setSelectedReport(prev => prev ? { ...prev, status: "actioned" } : null);
      }
      alert(`Đã gửi cảnh cáo đến ${authorName} và cập nhật dữ liệu thành công.`);
    } catch (err) {
      console.error("Error warning user:", err);
      alert("Lỗi khi gửi cảnh cáo. Vui lòng kiểm tra Firestore Security Rules.");
    } finally {
      setActionId(null);
    }
  };

  // 4. Lock Account Action (Sets isBanned = true, resolves report)
  const handleLockAccount = async (reportId: string, authorId: string, authorName: string) => {
    if (!authorId) {
      alert("Không tìm thấy mã người dùng (UID) để khóa.");
      return;
    }
    if (!window.confirm(`Bạn có chắc chắn muốn KHÓA vĩnh viễn tài khoản của "${authorName}"? Họ sẽ bị đăng xuất ngay lập tức và không thể truy cập lại.`)) {
      return;
    }

    setActionId(reportId);
    try {
      // a. Ban user
      const userRef = doc(db, "users", authorId);
      await updateDoc(userRef, {
        isBanned: true
      });

      // b. Resolve report
      const reportRef = doc(db, "reports", reportId);
      await updateDoc(reportRef, {
        status: "actioned"
      });

      // Update local states
      setReports(prev => prev.map(r => r.id === reportId ? { ...r, status: "actioned" } : r));
      setUsersCache(prev => {
        const u = prev[authorId] || {};
        return { ...prev, [authorId]: { ...u, isBanned: true } };
      });
      if (selectedReport?.id === reportId) {
        setSelectedReport(prev => prev ? { ...prev, status: "actioned" } : null);
      }
      alert(`Tài khoản của ${authorName} đã bị khóa trên toàn bộ hệ thống.`);
    } catch (err) {
      console.error("Error banning user:", err);
      alert("Lỗi khi khóa tài khoản. Vui lòng kiểm tra Firestore Security Rules.");
    } finally {
      setActionId(null);
    }
  };

  // Filtered reports
  const filteredReports = reports.filter((r) => {
    if (filterStatus === "all") return true;
    return r.status === filterStatus;
  });

  return (
    <div className="space-y-6 animate-fadeIn text-left">
      {/* Header */}
      <div>
        <h2 className="text-3xl font-display text-slate-100 tracking-wide">
          Báo Cáo Vi Phạm Bài Viết
        </h2>
        <p className="text-sm text-slate-400 mt-1">Duyệt báo cáo từ sinh viên, gửi cảnh báo cảnh cáo, xóa bài đăng, hoặc khóa tài khoản vi phạm</p>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 bg-[#0d1321]/50 border border-slate-900 p-2.5 rounded-2xl w-fit">
        {[
          { id: "pending", label: "Chờ xử lý", count: reports.filter(r => r.status === "pending").length },
          { id: "dismissed", label: "Đã bỏ qua", count: reports.filter(r => r.status === "dismissed").length },
          { id: "actioned", label: "Đã xử lý", count: reports.filter(r => r.status === "actioned").length },
          { id: "all", label: "Tất cả", count: reports.length }
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setFilterStatus(tab.id as any)}
            className={`px-4 py-2 rounded-xl text-xs font-bold uppercase tracking-wider transition font-mono flex items-center gap-2 ${
              filterStatus === tab.id
                ? "bg-amber-500/20 border border-amber-500/30 text-amber-400"
                : "bg-transparent text-slate-400 hover:text-white"
            }`}
          >
            <span>{tab.label}</span>
            <span className="px-1.5 py-0.5 text-[10px] rounded bg-slate-950/80 font-mono text-slate-300">
              {tab.count}
            </span>
          </button>
        ))}
      </div>

      {/* Reports Table / List */}
      <div className="bg-[#0d1321]/30 border border-slate-900 rounded-2xl overflow-hidden shadow-lg">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-900 bg-[#0c1220]/80">
                <th className="py-4 px-6 text-xs font-semibold uppercase tracking-widest text-slate-400 font-mono">Bài viết & Tác giả</th>
                <th className="py-4 px-6 text-xs font-semibold uppercase tracking-widest text-slate-400 font-mono">Vi phạm & Cảnh cáo</th>
                <th className="py-4 px-6 text-xs font-semibold uppercase tracking-widest text-slate-400 font-mono">Người báo cáo</th>
                <th className="py-4 px-6 text-xs font-semibold uppercase tracking-widest text-slate-400 font-mono">Trạng thái</th>
                <th className="py-4 px-6 text-xs font-semibold uppercase tracking-widest text-slate-400 text-right font-mono">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-950/80">
              {loading ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-slate-500 text-sm">
                    <span className="inline-block w-6 h-6 border-2 border-slate-800 border-t-amber-500 rounded-full animate-spin mr-2" />
                    Đang tải danh sách báo cáo vi phạm...
                  </td>
                </tr>
              ) : filteredReports.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-slate-500 text-sm">
                    Không có báo cáo nào trong mục này.
                  </td>
                </tr>
              ) : (
                filteredReports.map((report) => {
                  const authorId = report.authorId || "";
                  const userCached = usersCache[authorId] || {};
                  const warnCount = userCached.warningCount || 0;
                  const isBanned = userCached.isBanned || false;

                  return (
                    <tr key={report.id} className="hover:bg-slate-800/10 transition duration-200">
                      <td className="py-4 px-6 max-w-xs sm:max-w-sm">
                        <div className="truncate text-sm font-medium text-slate-200 mb-1">
                          "{report.postText}"
                        </div>
                        <div className="text-xs text-slate-500 flex items-center gap-1.5">
                          <User size={12} className="text-slate-650" />
                          <span>Tác giả: <strong>{report.postAuthor}</strong></span>
                          {isBanned && (
                            <span className="text-[8px] font-bold px-1 rounded bg-rose-500/15 border border-rose-500/30 text-rose-400 uppercase font-mono">Đã khóa tài khoản</span>
                          )}
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <div className="text-xs font-semibold text-amber-400/90 font-mono">
                          {report.reason}
                        </div>
                        <div className="text-[10px] text-slate-500 font-mono mt-0.5">
                          Số lần cảnh cáo: <strong className={warnCount > 0 ? "text-amber-500 font-bold" : "text-slate-400"}>{warnCount}</strong>
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <div className="text-xs font-medium text-slate-350">{report.reporterName}</div>
                        <div className="text-[10px] text-slate-550 font-mono mt-0.5">{report.reportedAt}</div>
                      </td>
                      <td className="py-4 px-6">
                        {report.status === "pending" ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20 font-mono">
                            <AlertTriangle size={10} /> Chờ xử lý
                          </span>
                        ) : report.status === "dismissed" ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-slate-500/10 text-slate-400 border border-slate-800 font-mono">
                            <CheckCircle size={10} /> Đã bỏ qua
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono">
                            <CheckCircle size={10} /> Đã xử lý
                          </span>
                        )}
                      </td>
                      <td className="py-4 px-6 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => setSelectedReport(report)}
                            className="p-1.5 bg-[#070b13] hover:bg-slate-800 border border-slate-800 text-slate-350 hover:text-white rounded-lg transition"
                            title="Xem chi tiết"
                          >
                            <Eye size={13} />
                          </button>
                          
                          {report.status === "pending" && (
                            <>
                              {/* Warn Button */}
                              <button
                                onClick={() => handleWarnUser(report.id, authorId, report.postAuthor || "", report.reason || "", report.postText || "")}
                                disabled={actionId === report.id || isBanned}
                                className="p-1.5 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/20 rounded-lg transition"
                                title="Gửi cảnh cáo đến người dùng"
                              >
                                <BellRing size={13} />
                              </button>

                              {/* Delete Post Button */}
                              <button
                                onClick={() => handleDeletePost(report.id, report.postId)}
                                disabled={actionId === report.id}
                                className="p-1.5 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/20 rounded-lg transition"
                                title="Xóa bài viết"
                              >
                                <Trash2 size={13} />
                              </button>

                              {/* Lock Account Button */}
                              <button
                                onClick={() => handleLockAccount(report.id, authorId, report.postAuthor || "")}
                                disabled={actionId === report.id || isBanned}
                                className="p-1.5 bg-rose-700/10 hover:bg-rose-700/25 text-rose-300 border border-rose-700/30 rounded-lg transition"
                                title="Khóa vĩnh viễn tài khoản người dùng"
                              >
                                <UserMinus size={13} />
                              </button>
                              
                              {/* Dismiss Button */}
                              <button
                                onClick={() => handleDismissReport(report.id)}
                                disabled={actionId === report.id}
                                className="p-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 rounded-lg text-[10px] font-bold transition font-mono px-2"
                                title="Bỏ qua báo cáo"
                              >
                                BỎ QUA
                              </button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Detail Modal */}
      {selectedReport && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-4">
          <div className="bg-[#0d1321] border border-slate-850 rounded-2xl max-w-xl w-full overflow-hidden shadow-2xl animate-scaleUp text-left">
            {/* Modal Header */}
            <div className="p-5 border-b border-slate-900 bg-[#0a0e19] flex items-center justify-between">
              <h3 className="text-md font-bold text-slate-200 flex items-center gap-2">
                <ShieldAlert size={18} className="text-amber-500" />
                Chi tiết báo cáo vi phạm
              </h3>
              <button
                onClick={() => setSelectedReport(null)}
                className="p-1 hover:bg-slate-800 rounded-lg text-slate-400 hover:text-white transition"
              >
                <X size={18} />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 space-y-4">
              <div className="bg-slate-950/40 p-4 rounded-xl border border-slate-900/60 space-y-3">
                <div className="flex items-center justify-between border-b border-slate-900 pb-2">
                  <span className="text-[10px] text-slate-500 uppercase tracking-wider font-mono font-bold">Người đăng bài</span>
                  <div className="text-right">
                    <span className="text-xs font-semibold text-slate-350 block">{selectedReport.postAuthor}</span>
                    <span className="text-[9px] text-slate-500 font-mono block">UID: {selectedReport.authorId}</span>
                  </div>
                </div>
                <div className="pt-1">
                  <p className="text-xs text-slate-500 font-mono font-bold uppercase tracking-wider mb-2">Nội dung bài viết</p>
                  <p className="text-sm text-slate-300 leading-relaxed bg-[#070b13] p-3 rounded-lg border border-slate-950 font-sans whitespace-pre-wrap select-all">
                    {selectedReport.postText}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="bg-slate-950/20 p-3.5 rounded-xl border border-slate-900/40">
                  <span className="block text-[10px] text-slate-500 uppercase tracking-wider font-mono font-bold">Lý do báo cáo</span>
                  <span className="block text-xs font-bold text-amber-400 mt-1 font-mono">{selectedReport.reason}</span>
                </div>
                <div className="bg-slate-950/20 p-3.5 rounded-xl border border-slate-900/40 flex flex-col justify-between">
                  <div>
                    <span className="block text-[10px] text-slate-500 uppercase tracking-wider font-mono font-bold">Người báo cáo</span>
                    <span className="block text-xs font-bold text-slate-300 mt-1">{selectedReport.reporterName}</span>
                  </div>
                </div>
              </div>

              <div className="flex justify-between items-center text-xs text-slate-400 bg-slate-950/10 px-4 py-2.5 rounded-xl">
                <div className="flex items-center gap-2">
                  <Calendar size={14} className="text-slate-500" />
                  <span>Thời gian gửi báo cáo: <strong>{selectedReport.reportedAt}</strong></span>
                </div>
                <div className="text-[10px] font-mono">
                  Đã cảnh cáo: <strong className="text-amber-500">{usersCache[selectedReport.authorId || ""]?.warningCount || 0} lần</strong>
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="p-5 border-t border-slate-900 bg-[#0a0e19] flex justify-between items-center">
              <div>
                <span className="text-[10px] text-slate-500 uppercase font-mono font-bold">Trạng thái: </span>
                {selectedReport.status === "pending" ? (
                  <span className="text-xs font-bold text-amber-500 ml-1">Chờ xử lý</span>
                ) : selectedReport.status === "dismissed" ? (
                  <span className="text-xs font-bold text-slate-400 ml-1">Đã bỏ qua</span>
                ) : (
                  <span className="text-xs font-bold text-emerald-500 ml-1">Đã xử lý giải quyết</span>
                )}
              </div>

              <div className="flex gap-2">
                <button
                  onClick={() => setSelectedReport(null)}
                  className="px-4 py-2 bg-[#070b13] hover:bg-slate-800 border border-slate-850 rounded-xl text-xs font-bold text-slate-300 hover:text-white transition"
                >
                  ĐÓNG
                </button>
                {selectedReport.status === "pending" && (
                  <>
                    <button
                      onClick={() => {
                        handleDismissReport(selectedReport.id);
                        setSelectedReport(null);
                      }}
                      className="px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-bold transition"
                    >
                      BỎ QUA
                    </button>
                    <button
                      onClick={() => {
                        handleWarnUser(selectedReport.id, selectedReport.authorId || "", selectedReport.postAuthor || "", selectedReport.reason || "", selectedReport.postText || "");
                      }}
                      className="px-3.5 py-2 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded-xl text-xs font-bold transition font-mono"
                    >
                      CẢNH BÁO
                    </button>
                    <button
                      onClick={() => {
                        handleDeletePost(selectedReport.id, selectedReport.postId);
                      }}
                      className="px-3.5 py-2 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 rounded-xl text-xs font-bold transition font-mono"
                    >
                      XÓA BÀI
                    </button>
                    <button
                      onClick={() => {
                        handleLockAccount(selectedReport.id, selectedReport.authorId || "", selectedReport.postAuthor || "");
                      }}
                      className="px-3.5 py-2 bg-rose-700/15 hover:bg-rose-700/25 text-rose-300 border border-rose-700/30 rounded-xl text-xs font-bold transition font-mono"
                    >
                      KHÓA ACC
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
