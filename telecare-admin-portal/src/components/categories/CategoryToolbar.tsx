import { useState, useEffect } from "react";
import { Search, RotateCw, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

interface CategoryToolbarProps {
  searchQuery: string;
  loading: boolean;
  canCreate: boolean;
  onSearchChange: (query: string) => void;
  onRefresh: () => void;
  onCreateClick: () => void;
}

export default function CategoryToolbar({
  searchQuery,
  loading,
  canCreate,
  onSearchChange,
  onRefresh,
  onCreateClick,
}: CategoryToolbarProps) {
  const [localSearch, setLocalSearch] = useState(searchQuery);

  useEffect(() => {
    setLocalSearch(searchQuery);
  }, [searchQuery]);

  useEffect(() => {
    const handler = setTimeout(() => {
      if (localSearch !== searchQuery) {
        onSearchChange(localSearch);
      }
    }, 300);
    return () => clearTimeout(handler);
  }, [localSearch, searchQuery, onSearchChange]);

  return (
    <div className="p-4 border-b border-border flex flex-col sm:flex-row items-center justify-between gap-4">
      {/* Search Bar */}
      <div className="relative flex-1 min-w-[200px] max-w-md w-full">
        <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Tìm nhóm dịch vụ theo tên hoặc mã..."
          value={localSearch}
          onChange={(e) => setLocalSearch(e.target.value)}
          className="pl-9 pr-4 h-9 rounded-xl bg-muted/30 border-border text-xs focus:bg-background transition-colors"
        />
      </div>

      {/* Buttons */}
      <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
        <Button
          variant="outline"
          size="sm"
          onClick={onRefresh}
          disabled={loading}
          className="rounded-xl gap-2 cursor-pointer text-xs"
        >
          <RotateCw className={`w-3.5 h-3.5 ${loading ? "animate-spin" : ""}`} />
          <span>Làm mới</span>
        </Button>

        {canCreate && (
          <Button
            size="sm"
            onClick={onCreateClick}
            className="rounded-xl gap-2 cursor-pointer text-xs font-semibold"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo nhóm dịch vụ</span>
          </Button>
        )}
      </div>
    </div>
  );
}
