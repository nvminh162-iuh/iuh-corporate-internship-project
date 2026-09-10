import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import {
  ArrowLeft,
  UserCheck,
  CheckCircle2,
  Clock,
  Send,
  User,
  Phone,
  Mail,
  FileText,
  History,
  MessageSquare,
  AlertCircle,
  X,
  Package,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  getAdminSupportRequestById,
  receiveSupportRequest,
  assignSupportRequest,
  updateSupportRequestStatus,
  addSupportRequestHistory,
} from "@/services/support-admin.service";
import { getAdminUsers } from "@/services/admin-user.service";
import type {
  SupportRequestAdminDetail,
  SupportRequestStatus,
  UserSummaryInfo,
} from "@/types/support-admin.type";
import type { AdminUser } from "@/types/user.type";

const STATUS_CONFIG: Record<
  SupportRequestStatus,
  { label: string; bg: string; text: string; border: string }
> = {
  NEW: {
    label: "Mới",
    bg: "bg-blue-500/10",
    text: "text-blue-600 dark:text-blue-400",
    border: "border-blue-500/20",
  },
  RECEIVED: {
    label: "Đã tiếp nhận",
    bg: "bg-cyan-500/10",
    text: "text-cyan-600 dark:text-cyan-400",
    border: "border-cyan-500/20",
  },
  IN_PROGRESS: {
    label: "Đang xử lý",
    bg: "bg-amber-500/10",
    text: "text-amber-600 dark:text-amber-400",
    border: "border-amber-500/20",
  },
  WAITING_CUSTOMER: {
    label: "Chờ khách hàng",
    bg: "bg-purple-500/10",
    text: "text-purple-600 dark:text-purple-400",
    border: "border-purple-500/20",
  },
  COMPLETED: {
    label: "Hoàn tất",
    bg: "bg-emerald-500/10",
    text: "text-emerald-600 dark:text-emerald-400",
    border: "border-emerald-500/20",
  },
  CLOSED: {
    label: "Đóng",
    bg: "bg-zinc-500/10",
    text: "text-zinc-600 dark:text-zinc-400",
    border: "border-zinc-500/20",
  },
};

function getUserDisplayName(user?: UserSummaryInfo | AdminUser | null, fallback?: string): string {
  if (!user) return fallback || "N/A";
  if ("fullName" in user && user.fullName) return user.fullName;
  const name = [user.lastName, user.firstName].filter(Boolean).join(" ");
  if (name) return name;
  return user.username || fallback || "N/A";
}

export default function SupportRequestDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [ticket, setTicket] = useState<SupportRequestAdminDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [staffUsers, setStaffUsers] = useState<AdminUser[]>([]);

  // Action Loading
  const [actionLoading, setActionLoading] = useState(false);

  // Modals state
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [assignStaffId, setAssignStaffId] = useState("");
  const [assignNote, setAssignNote] = useState("");

  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState<SupportRequestStatus>("IN_PROGRESS");
  const [statusNote, setStatusNote] = useState("");
  const [resolutionText, setResolutionText] = useState("");

  const [isHistoryModalOpen, setIsHistoryModalOpen] = useState(false);
  const [historyNote, setHistoryNote] = useState("");

  const fetchDetail = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await getAdminSupportRequestById(id);
      setTicket(data);
    } catch (error) {
      console.error("Failed to load ticket detail:", error);
      toast.error("Không thể tải thông tin chi tiết ticket!");
    } finally {
      setLoading(false);
    }
  }, [id]);

  const fetchStaff = useCallback(async () => {
    try {
      const res = await getAdminUsers(1, 100);
      setStaffUsers(res.result || []);
    } catch {
      // Fallback
    }
  }, []);

  useEffect(() => {
    fetchDetail();
    fetchStaff();
  }, [fetchDetail, fetchStaff]);

  if (loading) {
    return (
      <div className="p-6 space-y-6 animate-pulse">
        <div className="h-8 bg-muted rounded w-1/4"></div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 h-96 bg-muted rounded-xl"></div>
          <div className="h-96 bg-muted rounded-xl"></div>
        </div>
      </div>
    );
  }

  if (!ticket) {
    return (
      <div className="p-12 text-center space-y-4">
        <AlertCircle className="w-12 h-12 text-red-500 mx-auto" />
        <h2 className="text-xl font-bold">Không tìm thấy yêu cầu hỗ trợ</h2>
        <Button variant="outline" onClick={() => navigate("/admin/support-requests")}>
          Quay lại danh sách
        </Button>
      </div>
    );
  }

  const isClosed = ticket.status === "CLOSED";
  const statusInfo = STATUS_CONFIG[ticket.status];

  // Actions
  const handleReceive = async () => {
    if (!ticket) return;
    setActionLoading(true);
    try {
      const updated = await receiveSupportRequest(ticket.id);
      setTicket(updated);
      toast.success("Đã tiếp nhận ticket thành công!");
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Không thể tiếp nhận ticket!");
    } finally {
      setActionLoading(false);
    }
  };

  const handleAssignSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assignStaffId) {
      toast.error("Vui lòng chọn nhân viên xử lý!");
      return;
    }
    setActionLoading(true);
    try {
      const updated = await assignSupportRequest(ticket.id, {
        assignedTo: assignStaffId,
        note: assignNote,
      });
      setTicket(updated);
      toast.success("Phân công nhân viên thành công!");
      setIsAssignModalOpen(false);
      setAssignNote("");
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Phân công thất bại!");
    } finally {
      setActionLoading(false);
    }
  };

  const handleStatusSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (
      (targetStatus === "WAITING_CUSTOMER" || targetStatus === "COMPLETED") &&
      !statusNote.trim()
    ) {
      toast.error(`Vui lòng nhập nội dung ghi chú khi chuyển sang trạng thái ${targetStatus}!`);
      return;
    }

    setActionLoading(true);
    try {
      const updated = await updateSupportRequestStatus(ticket.id, {
        status: targetStatus,
        note: statusNote,
        resolution: targetStatus === "COMPLETED" ? resolutionText : undefined,
      });
      setTicket(updated);
      toast.success(`Đã chuyển trạng thái sang ${targetStatus}!`);
      setIsStatusModalOpen(false);
      setStatusNote("");
      setResolutionText("");
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Cập nhật trạng thái thất bại!");
    } finally {
      setActionLoading(false);
    }
  };

  const handleHistorySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!historyNote.trim()) {
      toast.error("Vui lòng nhập nội dung ghi chú!");
      return;
    }
    setActionLoading(true);
    try {
      await addSupportRequestHistory(ticket.id, { note: historyNote });
      toast.success("Đã thêm nội dung xử lý vào lịch sử!");
      setIsHistoryModalOpen(false);
      setHistoryNote("");
      fetchDetail();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Thêm ghi chú thất bại!");
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      {/* Navigation Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4">
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="icon"
            onClick={() => navigate("/admin/support-requests")}
            className="rounded-lg"
          >
            <ArrowLeft className="w-4 h-4" />
          </Button>
          <div>
            <div className="flex items-center gap-3">
              <span className="font-mono font-bold text-lg text-primary">
                {ticket.ticketCode}
              </span>
              <span
                className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${statusInfo.bg} ${statusInfo.text} ${statusInfo.border}`}
              >
                {statusInfo.label}
              </span>
            </div>
            <h1 className="text-xl font-bold text-foreground mt-0.5">{ticket.subject}</h1>
          </div>
        </div>

        {/* Action Buttons */}
        {!isClosed && (
          <div className="flex flex-wrap items-center gap-2">
            {ticket.status === "NEW" && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleReceive}
                disabled={actionLoading}
                className="border-cyan-500/30 text-cyan-600 hover:bg-cyan-50 dark:hover:bg-cyan-950/20 gap-1.5"
              >
                <CheckCircle2 className="w-4 h-4" />
                Tiếp nhận Ticket
              </Button>
            )}

            {ticket.status !== "NEW" && (
              <>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setAssignStaffId(ticket.assignedToId || ticket.assignedTo?.id || "");
                    setIsAssignModalOpen(true);
                  }}
                  disabled={actionLoading}
                  className="gap-1.5"
                >
                  <UserCheck className="w-4 h-4" />
                  Phân công
                </Button>

                <Button
                  variant="default"
                  size="sm"
                  onClick={() => {
                    setTargetStatus(ticket.status);
                    setIsStatusModalOpen(true);
                  }}
                  disabled={actionLoading}
                  className="gap-1.5"
                >
                  <Send className="w-4 h-4" />
                  Cập nhật trạng thái
                </Button>

                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setIsHistoryModalOpen(true)}
                  disabled={actionLoading}
                  className="gap-1.5"
                >
                  <MessageSquare className="w-4 h-4" />
                  Thêm nội dung xử lý
                </Button>
              </>
            )}
          </div>
        )}
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Detail Content & Timeline (2 cols) */}
        <div className="lg:col-span-2 space-y-6">
          {/* Main Info Card */}
          <div className="bg-card border border-border rounded-xl p-5 shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-primary font-semibold border-b border-border pb-3">
              <FileText className="w-5 h-5" />
              <span>Nội dung Yêu cầu Hỗ trợ</span>
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-muted-foreground text-xs block">Nhóm vấn đề</span>
                <span className="font-medium text-foreground">
                  {ticket.category?.name || ticket.categoryName || ticket.categoryCode}
                </span>
              </div>
              <div>
                <span className="text-muted-foreground text-xs block">Thời gian gửi</span>
                <span className="font-medium text-foreground">
                  {new Date(ticket.createdAt).toLocaleString("vi-VN")}
                </span>
              </div>
            </div>

            {/* Related Service Plan */}
            {ticket.servicePlan && (
              <div className="p-3 bg-muted/40 rounded-lg flex items-center gap-3 border border-border">
                <Package className="w-5 h-5 text-primary" />
                <div>
                  <span className="text-xs text-muted-foreground block">Gói cước liên quan</span>
                  <span className="text-sm font-bold text-foreground">
                    {ticket.servicePlan.name} ({ticket.servicePlan.code})
                  </span>
                </div>
              </div>
            )}

            {/* Detailed Content */}
            <div>
              <span className="text-muted-foreground text-xs block mb-1">Mô tả chi tiết từ khách hàng</span>
              <div className="p-4 bg-muted/30 border border-border rounded-lg text-sm leading-relaxed text-foreground whitespace-pre-wrap">
                {ticket.content}
              </div>
            </div>

            {/* Resolution text if completed */}
            {ticket.resolution && (
              <div className="p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-sm space-y-1">
                <span className="font-bold text-emerald-600 dark:text-emerald-400 block">
                  Kết quả xử lý (Resolution):
                </span>
                <p className="text-emerald-700 dark:text-emerald-300 font-medium whitespace-pre-wrap">
                  {ticket.resolution}
                </p>
              </div>
            )}

            {/* Milestone Timestamps */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-3 border-t border-border text-xs">
              <div>
                <span className="text-muted-foreground block">Tiếp nhận:</span>
                <span className="font-medium">
                  {ticket.receivedAt ? new Date(ticket.receivedAt).toLocaleString("vi-VN") : "--"}
                </span>
              </div>
              <div>
                <span className="text-muted-foreground block">Phân công:</span>
                <span className="font-medium">
                  {ticket.assignedAt ? new Date(ticket.assignedAt).toLocaleString("vi-VN") : "--"}
                </span>
              </div>
              <div>
                <span className="text-muted-foreground block">Hoàn tất:</span>
                <span className="font-medium">
                  {ticket.completedAt ? new Date(ticket.completedAt).toLocaleString("vi-VN") : "--"}
                </span>
              </div>
              <div>
                <span className="text-muted-foreground block">Đóng ticket:</span>
                <span className="font-medium">
                  {ticket.closedAt ? new Date(ticket.closedAt).toLocaleString("vi-VN") : "--"}
                </span>
              </div>
            </div>
          </div>

          {/* Timeline History Card */}
          <div className="bg-card border border-border rounded-xl p-5 shadow-sm space-y-4">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2 text-primary font-semibold">
                <History className="w-5 h-5" />
                <span>Lịch sử Xử lý & Nhật ký Timeline</span>
              </div>
              <span className="text-xs text-muted-foreground font-medium">
                {ticket.histories?.length || 0} nhật ký
              </span>
            </div>

            {!ticket.histories || ticket.histories.length === 0 ? (
              <p className="text-sm text-muted-foreground italic text-center py-6">
                Chưa có lịch sử xử lý nào.
              </p>
            ) : (
              <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-border">
                {ticket.histories.map((h) => {
                  const toStatusInfo = h.toStatus ? STATUS_CONFIG[h.toStatus] : null;
                  const actorDisplayName = getUserDisplayName(h.actor, h.actorName || h.actorId);
                  return (
                    <div key={h.id} className="relative group">
                      {/* Circle icon marker */}
                      <div className="absolute -left-6 top-0.5 w-4 h-4 rounded-full bg-primary/20 border-2 border-primary group-hover:scale-110 transition-transform"></div>

                      <div className="bg-muted/30 border border-border p-3.5 rounded-lg space-y-1.5">
                        <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-foreground">
                              {actorDisplayName}
                            </span>
                            {h.action && (
                              <span className="px-2 py-0.5 rounded bg-muted text-muted-foreground font-mono">
                                {h.action}
                              </span>
                            )}
                          </div>
                          <span className="text-muted-foreground flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            {new Date(h.createdAt).toLocaleString("vi-VN")}
                          </span>
                        </div>

                        {/* Status Change indicator */}
                        {h.fromStatus && h.toStatus && h.fromStatus !== h.toStatus && (
                          <div className="text-xs flex items-center gap-1.5 pt-1">
                            <span className="text-muted-foreground">{h.fromStatus}</span>
                            <span>→</span>
                            {toStatusInfo && (
                              <span
                                className={`px-2 py-0.5 rounded-full text-xs font-semibold ${toStatusInfo.bg} ${toStatusInfo.text}`}
                              >
                                {toStatusInfo.label}
                              </span>
                            )}
                          </div>
                        )}

                        {/* History Note */}
                        {h.note && (
                          <p className="text-sm text-foreground pt-1 leading-relaxed border-t border-border/50 mt-2">
                            {h.note}
                          </p>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Customer & Staff Info (1 col) */}
        <div className="space-y-6">
          {/* Customer Info Card */}
          <div className="bg-card border border-border rounded-xl p-5 shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-primary font-semibold border-b border-border pb-3">
              <User className="w-5 h-5" />
              <span>Thông tin Khách hàng</span>
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex items-start gap-2.5">
                <User className="w-4 h-4 text-muted-foreground mt-0.5" />
                <div>
                  <span className="text-xs text-muted-foreground block">Họ và tên</span>
                  <span className="font-semibold text-foreground">
                    {getUserDisplayName(ticket.customer, ticket.customerName || "Khách hàng")}
                  </span>
                </div>
              </div>

              <div className="flex items-start gap-2.5">
                <Phone className="w-4 h-4 text-muted-foreground mt-0.5" />
                <div>
                  <span className="text-xs text-muted-foreground block">Số điện thoại liên hệ</span>
                  <span className="font-mono text-foreground">
                    {ticket.contactPhone || ticket.customerPhone || ticket.customer?.phone || "Chưa cung cấp"}
                  </span>
                </div>
              </div>

              <div className="flex items-start gap-2.5">
                <Mail className="w-4 h-4 text-muted-foreground mt-0.5" />
                <div>
                  <span className="text-xs text-muted-foreground block">Email liên hệ</span>
                  <span className="font-mono text-foreground truncate block">
                    {ticket.contactEmail || ticket.customerEmail || ticket.customer?.email || "Chưa cung cấp"}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Staff Info Card */}
          <div className="bg-card border border-border rounded-xl p-5 shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-primary font-semibold border-b border-border pb-3">
              <UserCheck className="w-5 h-5" />
              <span>Nhân viên Xử lý</span>
            </div>

            {ticket.assignedTo || ticket.assignedToName ? (
              <div className="space-y-3 text-sm">
                <div>
                  <span className="text-xs text-muted-foreground block">Họ tên nhân viên</span>
                  <span className="font-bold text-foreground">
                    {getUserDisplayName(ticket.assignedTo, ticket.assignedToName)}
                  </span>
                </div>
                {ticket.assignedTo?.email && (
                  <div>
                    <span className="text-xs text-muted-foreground block">Email nhân viên</span>
                    <span className="font-mono text-xs text-foreground">
                      {ticket.assignedTo.email}
                    </span>
                  </div>
                )}
                {ticket.assignedAt && (
                  <div>
                    <span className="text-xs text-muted-foreground block">Thời gian phân công</span>
                    <span className="text-xs text-foreground">
                      {new Date(ticket.assignedAt).toLocaleString("vi-VN")}
                    </span>
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center py-4 text-muted-foreground text-sm space-y-2">
                <p className="italic">Ticket chưa được phân công nhân viên xử lý.</p>
                {ticket.status !== "NEW" && !isClosed && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setIsAssignModalOpen(true)}
                    className="w-full text-xs"
                  >
                    Phân công ngay
                  </Button>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Modal 1: Phân công nhân viên */}
      {isAssignModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-card border border-border rounded-xl w-full max-w-md p-6 space-y-4 shadow-xl">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-bold text-lg text-foreground">Phân công Nhân viên</h3>
              <button
                onClick={() => setIsAssignModalOpen(false)}
                className="text-muted-foreground hover:text-foreground"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAssignSubmit} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-foreground block mb-1">
                  Chọn nhân viên hỗ trợ <span className="text-red-500">*</span>
                </label>
                <select
                  value={assignStaffId}
                  onChange={(e) => setAssignStaffId(e.target.value)}
                  className="w-full px-3 py-2 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                  required
                >
                  <option value="">-- Chọn nhân viên --</option>
                  {staffUsers.map((user) => (
                    <option key={user.id} value={user.id}>
                      {getUserDisplayName(user)} ({user.email})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs font-semibold text-foreground block mb-1">
                  Ghi chú phân công (tùy chọn)
                </label>
                <textarea
                  rows={3}
                  value={assignNote}
                  onChange={(e) => setAssignNote(e.target.value)}
                  placeholder="Nhập ghi chú cho nhân viên..."
                  className="w-full p-3 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-border">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setIsAssignModalOpen(false)}
                >
                  Hủy
                </Button>
                <Button type="submit" size="sm" disabled={actionLoading}>
                  Xác nhận Phân công
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 2: Cập nhật trạng thái */}
      {isStatusModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-card border border-border rounded-xl w-full max-w-lg p-6 space-y-4 shadow-xl">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-bold text-lg text-foreground">Cập nhật Trạng thái Ticket</h3>
              <button
                onClick={() => setIsStatusModalOpen(false)}
                className="text-muted-foreground hover:text-foreground"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleStatusSubmit} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-foreground block mb-1">
                  Trạng thái mới <span className="text-red-500">*</span>
                </label>
                <select
                  value={targetStatus}
                  onChange={(e) => setTargetStatus(e.target.value as SupportRequestStatus)}
                  className="w-full px-3 py-2 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                  required
                >
                  <option value="IN_PROGRESS">Đang xử lý (IN_PROGRESS)</option>
                  <option value="WAITING_CUSTOMER">Chờ khách hàng (WAITING_CUSTOMER)</option>
                  <option value="COMPLETED">Hoàn tất (COMPLETED)</option>
                  <option value="CLOSED">Đóng ticket (CLOSED)</option>
                </select>
              </div>

              {targetStatus === "COMPLETED" && (
                <div>
                  <label className="text-xs font-semibold text-foreground block mb-1">
                    Kết quả xử lý (Resolution) <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    rows={3}
                    value={resolutionText}
                    onChange={(e) => setResolutionText(e.target.value)}
                    placeholder="Mô tả kết quả xử lý sự cố cho khách hàng..."
                    className="w-full p-3 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                    required
                  />
                </div>
              )}

              <div>
                <label className="text-xs font-semibold text-foreground block mb-1">
                  Ghi chú lịch sử{" "}
                  {(targetStatus === "WAITING_CUSTOMER" || targetStatus === "COMPLETED") && (
                    <span className="text-red-500">* (Bắt buộc)</span>
                  )}
                </label>
                <textarea
                  rows={3}
                  value={statusNote}
                  onChange={(e) => setStatusNote(e.target.value)}
                  placeholder={
                    targetStatus === "WAITING_CUSTOMER"
                      ? "Nhập lý do / nội dung yêu cầu khách hàng bổ sung..."
                      : "Nhập ghi chú cập nhật..."
                  }
                  className="w-full p-3 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                  required={targetStatus === "WAITING_CUSTOMER" || targetStatus === "COMPLETED"}
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-border">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setIsStatusModalOpen(false)}
                >
                  Hủy
                </Button>
                <Button type="submit" size="sm" disabled={actionLoading}>
                  Lưu thay đổi
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 3: Thêm ghi chú lịch sử */}
      {isHistoryModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-card border border-border rounded-xl w-full max-w-md p-6 space-y-4 shadow-xl">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-bold text-lg text-foreground">Thêm Nội dung Xử lý</h3>
              <button
                onClick={() => setIsHistoryModalOpen(false)}
                className="text-muted-foreground hover:text-foreground"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleHistorySubmit} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-foreground block mb-1">
                  Nội dung ghi chú / kết quả liên hệ <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={4}
                  value={historyNote}
                  onChange={(e) => setHistoryNote(e.target.value)}
                  placeholder="Nhập chi tiết quá trình làm việc, liên hệ với khách hàng..."
                  className="w-full p-3 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-border">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setIsHistoryModalOpen(false)}
                >
                  Hủy
                </Button>
                <Button type="submit" size="sm" disabled={actionLoading}>
                  Thêm vào Lịch sử
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
