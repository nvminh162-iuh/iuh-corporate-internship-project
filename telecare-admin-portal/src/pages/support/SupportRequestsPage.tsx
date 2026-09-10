import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import {
  LifeBuoy,
  Search,
  RefreshCw,
  Eye,
  CheckCircle2,
  UserCheck,
  Calendar,
  Filter,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import UserPagination from "@/components/users/UserPagination";
import {
  getAdminSupportRequests,
  getSupportCategories,
  receiveSupportRequest,
} from "@/services/support-admin.service";
import { getAdminUsers } from "@/services/admin-user.service";
import type {
  SupportRequestAdminSummary,
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

export default function SupportRequestsPage() {
  const navigate = useNavigate();

  // Data & Pagination
  const [tickets, setTickets] = useState<SupportRequestAdminSummary[]>([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  // Options for Dropdowns
  const [categories, setCategories] = useState<{ code: string; name: string }[]>([]);
  const [staffUsers, setStaffUsers] = useState<AdminUser[]>([]);

  // Filters
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<SupportRequestStatus | "">("");
  const [categoryCode, setCategoryCode] = useState("");
  const [assignedTo, setAssignedTo] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

  // Action Loading
  const [receivingId, setReceivingId] = useState<string | null>(null);

  // Fetch Options
  const fetchOptions = useCallback(async () => {
    try {
      const [cats, usersRes] = await Promise.all([
        getSupportCategories().catch(() => []),
        getAdminUsers(1, 100).catch(() => ({ result: [] })),
      ]);
      setCategories(cats);
      setStaffUsers(usersRes.result || []);
    } catch {
      // Ignore fallback
    }
  }, []);

  // Fetch Tickets
  const fetchTickets = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAdminSupportRequests({
        page,
        size,
        keyword,
        status: status || undefined,
        categoryCode: categoryCode || undefined,
        assignedTo: assignedTo || undefined,
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
      });
      setTickets(data.result || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error("Failed to fetch support requests:", error);
      toast.error("Không thể tải danh sách yêu cầu hỗ trợ!");
    } finally {
      setLoading(false);
    }
  }, [page, size, keyword, status, categoryCode, assignedTo, fromDate, toDate]);

  useEffect(() => {
    fetchOptions();
  }, [fetchOptions]);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  // Handle Quick Receive
  const handleReceive = async (e: React.MouseEvent, id: string, code: string) => {
    e.stopPropagation();
    setReceivingId(id);
    try {
      await receiveSupportRequest(id);
      toast.success(`Đã tiếp nhận ticket ${code} thành công!`);
      fetchTickets();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Không thể tiếp nhận ticket!");
    } finally {
      setReceivingId(null);
    }
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <LifeBuoy className="w-7 h-7 text-primary" />
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              Quản lý Yêu cầu Hỗ trợ
            </h1>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Theo dõi, tiếp nhận, phân công và xử lý các ticket yêu cầu hỗ trợ từ khách hàng.
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => fetchTickets()}
          disabled={loading}
          className="self-start md:self-auto gap-2"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
          Làm mới
        </Button>
      </div>

      {/* Toolbar / Filters */}
      <div className="bg-card border border-border rounded-xl p-4 space-y-4 shadow-sm">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Keyword Search */}
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <input
              type="text"
              placeholder="Mã ticket, tiêu đề, SĐT..."
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPage(1);
              }}
              className="w-full pl-9 pr-3 py-2 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>

          {/* Status Filter */}
          <div className="relative">
            <select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as SupportRequestStatus | "");
                setPage(1);
              }}
              className="w-full px-3 py-2 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 cursor-pointer"
            >
              <option value="">-- Tất cả trạng thái --</option>
              <option value="NEW">Mới (NEW)</option>
              <option value="RECEIVED">Đã tiếp nhận (RECEIVED)</option>
              <option value="IN_PROGRESS">Đang xử lý (IN_PROGRESS)</option>
              <option value="WAITING_CUSTOMER">Chờ khách hàng (WAITING_CUSTOMER)</option>
              <option value="COMPLETED">Hoàn tất (COMPLETED)</option>
              <option value="CLOSED">Đóng (CLOSED)</option>
            </select>
          </div>

          {/* Category Filter */}
          <div className="relative">
            <select
              value={categoryCode}
              onChange={(e) => {
                setCategoryCode(e.target.value);
                setPage(1);
              }}
              className="w-full px-3 py-2 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 cursor-pointer"
            >
              <option value="">-- Tất cả nhóm vấn đề --</option>
              {categories.map((cat) => (
                <option key={cat.code} value={cat.code}>
                  {cat.name} ({cat.code})
                </option>
              ))}
            </select>
          </div>

          {/* Staff Filter */}
          <div className="relative">
            <select
              value={assignedTo}
              onChange={(e) => {
                setAssignedTo(e.target.value);
                setPage(1);
              }}
              className="w-full px-3 py-2 text-sm bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 cursor-pointer"
            >
              <option value="">-- Tất cả nhân viên --</option>
              {staffUsers.map((user) => (
                <option key={user.id} value={user.id}>
                  {getUserDisplayName(user)}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Date Filter */}
        <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-border text-xs text-muted-foreground">
          <div className="flex items-center gap-1.5 font-medium">
            <Filter className="w-3.5 h-3.5" />
            <span>Thời gian tạo từ:</span>
          </div>
          <input
            type="date"
            value={fromDate}
            onChange={(e) => {
              setFromDate(e.target.value);
              setPage(1);
            }}
            className="px-2.5 py-1 bg-background border border-border rounded-md text-foreground text-xs"
          />
          <span>đến:</span>
          <input
            type="date"
            value={toDate}
            onChange={(e) => {
              setToDate(e.target.value);
              setPage(1);
            }}
            className="px-2.5 py-1 bg-background border border-border rounded-md text-foreground text-xs"
          />
          {(keyword || status || categoryCode || assignedTo || fromDate || toDate) && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setKeyword("");
                setStatus("");
                setCategoryCode("");
                setAssignedTo("");
                setFromDate("");
                setToDate("");
                setPage(1);
              }}
              className="h-7 text-xs text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/20 ml-auto"
            >
              Xóa bộ lọc
            </Button>
          )}
        </div>
      </div>

      {/* Main Table */}
      <div className="bg-card border border-border rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-muted/50 text-muted-foreground font-semibold text-xs border-b border-border">
              <tr>
                <th className="px-4 py-3">MÃ TICKET</th>
                <th className="px-4 py-3">KHÁCH HÀNG</th>
                <th className="px-4 py-3">NHÓM VẤN ĐỀ</th>
                <th className="px-4 py-3">TIÊU ĐỀ</th>
                <th className="px-4 py-3 text-center">TRẠNG THÁI</th>
                <th className="px-4 py-3">PHÂN CÔNG</th>
                <th className="px-4 py-3">NGÀY TẠO</th>
                <th className="px-4 py-3 text-right">THAO TÁC</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {loading ? (
                Array.from({ length: 5 }).map((_, idx) => (
                  <tr key={idx} className="animate-pulse">
                    <td colSpan={8} className="px-4 py-4">
                      <div className="h-4 bg-muted rounded w-3/4"></div>
                    </td>
                  </tr>
                ))
              ) : tickets.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-12 text-center text-muted-foreground">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <LifeBuoy className="w-10 h-10 text-muted-foreground/40" />
                      <p className="font-medium text-base">Không tìm thấy yêu cầu hỗ trợ nào</p>
                      <p className="text-xs">Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm</p>
                    </div>
                  </td>
                </tr>
              ) : (
                tickets.map((item) => {
                  const statusInfo = STATUS_CONFIG[item.status];
                  const customerName = getUserDisplayName(item.customer, item.customerName || "Khách hàng");
                  const assignedName = getUserDisplayName(item.assignedTo, item.assignedToName);

                  return (
                    <tr
                      key={item.id}
                      onClick={() => navigate(`/admin/support-requests/${item.id}`)}
                      className="hover:bg-muted/30 transition-colors cursor-pointer"
                    >
                      {/* Ticket Code */}
                      <td className="px-4 py-3.5 font-mono font-semibold text-primary">
                        {item.ticketCode}
                      </td>

                      {/* Customer */}
                      <td className="px-4 py-3.5">
                        <div className="font-medium text-foreground">
                          {customerName}
                        </div>
                        <div className="text-xs text-muted-foreground">
                          {item.contactPhone || item.customerPhone || item.contactEmail || item.customerEmail || item.customerId}
                        </div>
                      </td>

                      {/* Category */}
                      <td className="px-4 py-3.5">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-muted text-muted-foreground">
                          {item.category?.name || item.categoryName || item.categoryCode}
                        </span>
                      </td>

                      {/* Subject */}
                      <td className="px-4 py-3.5 max-w-[240px]">
                        <div className="truncate font-medium text-foreground" title={item.subject}>
                          {item.subject}
                        </div>
                      </td>

                      {/* Status Badge */}
                      <td className="px-4 py-3.5 text-center">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${statusInfo.bg} ${statusInfo.text} ${statusInfo.border}`}
                        >
                          {statusInfo.label}
                        </span>
                      </td>

                      {/* Assignee */}
                      <td className="px-4 py-3.5">
                        {item.assignedTo || item.assignedToName ? (
                          <div className="flex items-center gap-1.5 text-xs text-foreground font-medium">
                            <UserCheck className="w-3.5 h-3.5 text-emerald-500" />
                            <span>{assignedName}</span>
                          </div>
                        ) : (
                          <span className="text-xs text-muted-foreground italic">Chưa phân công</span>
                        )}
                      </td>

                      {/* Created At */}
                      <td className="px-4 py-3.5 text-xs text-muted-foreground whitespace-nowrap">
                        <div className="flex items-center gap-1">
                          <Calendar className="w-3.5 h-3.5" />
                          <span>{new Date(item.createdAt).toLocaleDateString("vi-VN")}</span>
                        </div>
                      </td>

                      {/* Actions */}
                      <td className="px-4 py-3.5 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end gap-1.5">
                          {item.status === "NEW" && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={(e) => handleReceive(e, item.id, item.ticketCode)}
                              disabled={receivingId === item.id}
                              className="h-8 text-xs gap-1 border-cyan-500/30 text-cyan-600 hover:bg-cyan-50 dark:hover:bg-cyan-950/20"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              Tiếp nhận
                            </Button>
                          )}

                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              navigate(`/admin/support-requests/${item.id}`);
                            }}
                            className="h-8 text-xs gap-1 text-primary hover:bg-primary/10"
                          >
                            <Eye className="w-3.5 h-3.5" />
                            Chi tiết
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <UserPagination
          page={page}
          size={size}
          totalPages={totalPages}
          totalElements={totalElements}
          loading={loading}
          itemLabel="yêu cầu hỗ trợ"
          onPageChange={(p) => setPage(p)}
          onSizeChange={(s) => setSize(s)}
        />
      </div>
    </div>
  );
}
