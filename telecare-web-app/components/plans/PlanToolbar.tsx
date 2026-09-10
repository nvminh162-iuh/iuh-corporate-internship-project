import React, { useState, useEffect } from "react";
import { Search, RotateCcw, SlidersHorizontal, ArrowUpDown } from "lucide-react";
import type { BillingCycle, PublicCategory } from "@/types/plan.type";

interface PlanToolbarProps {
  categories: PublicCategory[];
  keyword: string;
  categoryCode: string;
  minPrice?: number;
  maxPrice?: number;
  billingCycle?: BillingCycle | "";
  sort: string;
  onKeywordChange: (val: string) => void;
  onCategoryChange: (val: string) => void;
  onPriceRangeChange: (min?: number, max?: number) => void;
  onBillingCycleChange: (val: BillingCycle | "") => void;
  onSortChange: (val: string) => void;
  onResetFilters: () => void;
}

export default function PlanToolbar({
  categories,
  keyword,
  categoryCode,
  minPrice,
  maxPrice,
  billingCycle,
  sort,
  onKeywordChange,
  onCategoryChange,
  onPriceRangeChange,
  onBillingCycleChange,
  onSortChange,
  onResetFilters,
}: PlanToolbarProps) {
  const [searchInput, setSearchInput] = useState(keyword);
  const [minPriceInput, setMinPriceInput] = useState(minPrice?.toString() || "");
  const [maxPriceInput, setMaxPriceInput] = useState(maxPrice?.toString() || "");
  const [showAdvanced, setShowAdvanced] = useState(false);

  // Sync external props to local input state
  useEffect(() => {
    setSearchInput(keyword);
  }, [keyword]);

  useEffect(() => {
    setMinPriceInput(minPrice?.toString() || "");
    setMaxPriceInput(maxPrice?.toString() || "");
  }, [minPrice, maxPrice]);

  // Debounce search input change by 300ms
  useEffect(() => {
    const handler = setTimeout(() => {
      if (searchInput !== keyword) {
        onKeywordChange(searchInput);
      }
    }, 300);
    return () => clearTimeout(handler);
  }, [searchInput, keyword, onKeywordChange]);

  const handleApplyPriceRange = () => {
    const min = minPriceInput ? Number(minPriceInput) : undefined;
    const max = maxPriceInput ? Number(maxPriceInput) : undefined;
    onPriceRangeChange(min, max);
  };

  const hasActiveFilters = Boolean(
    keyword ||
      categoryCode ||
      minPrice != null ||
      maxPrice != null ||
      billingCycle ||
      sort !== "displayOrder,asc"
  );

  return (
    <div className="space-y-4">
      {/* 1. Top Bar: Search Input & Category Pills */}
      <div className="bg-card/90 dark:bg-card/40 rounded-3xl border border-border/80 p-4 sm:p-5 space-y-4 shadow-sm">
        <div className="flex flex-col md:flex-row items-center gap-3">
          {/* Search Box */}
          <div className="relative flex-1 w-full">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Tìm theo tên gói, mã gói hoặc từ khóa..."
              className="w-full h-11 pl-10 pr-4 bg-muted/50 focus:bg-background rounded-2xl border border-border focus:border-primary text-xs sm:text-sm text-foreground outline-none transition-all"
            />
          </div>

          {/* Sort Selector */}
          <div className="flex items-center gap-2 w-full md:w-auto">
            <div className="relative flex-1 md:w-48">
              <ArrowUpDown className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-muted-foreground" />
              <select
                value={sort}
                onChange={(e) => onSortChange(e.target.value)}
                className="w-full h-11 pl-9 pr-8 bg-muted/50 focus:bg-background rounded-2xl border border-border focus:border-primary text-xs font-semibold text-foreground outline-none transition-all cursor-pointer"
              >
                <option value="displayOrder,asc">Nổi bật & Thứ tự</option>
                <option value="price,asc">Giá tăng dần</option>
                <option value="price,desc">Giá giảm dần</option>
                <option value="createdAt,desc">Mới nhất</option>
              </select>
            </div>

            {/* Toggle Advanced Filters Button */}
            <button
              type="button"
              onClick={() => setShowAdvanced(!showAdvanced)}
              className={`h-11 px-4 rounded-2xl border text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${
                showAdvanced || minPrice != null || maxPrice != null || billingCycle
                  ? "bg-primary/10 border-primary/30 text-primary"
                  : "bg-muted/40 border-border text-foreground hover:bg-muted"
              }`}
            >
              <SlidersHorizontal className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Bộ lọc nâng cao</span>
            </button>

            {/* Reset Filters Button */}
            {hasActiveFilters && (
              <button
                type="button"
                onClick={() => {
                  setSearchInput("");
                  setMinPriceInput("");
                  setMaxPriceInput("");
                  onResetFilters();
                }}
                className="h-11 px-3.5 rounded-2xl bg-destructive/10 hover:bg-destructive/20 border border-destructive/20 text-destructive text-xs font-bold transition-all cursor-pointer flex items-center gap-1 shrink-0"
                title="Xóa tất cả bộ lọc"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Xóa lọc</span>
              </button>
            )}
          </div>
        </div>

        {/* Category Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pt-1">
          <button
            type="button"
            onClick={() => onCategoryChange("")}
            className={`h-9 px-4 rounded-xl text-xs font-bold transition-all cursor-pointer whitespace-nowrap shrink-0 ${
              !categoryCode
                ? "bg-primary text-primary-foreground shadow-sm shadow-primary/20"
                : "bg-muted/50 hover:bg-muted text-muted-foreground hover:text-foreground border border-border/60"
            }`}
          >
            Tất cả gói cước
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              type="button"
              onClick={() => onCategoryChange(cat.code)}
              className={`h-9 px-4 rounded-xl text-xs font-bold transition-all cursor-pointer whitespace-nowrap shrink-0 ${
                categoryCode.toUpperCase() === cat.code.toUpperCase()
                  ? "bg-primary text-primary-foreground shadow-sm shadow-primary/20"
                  : "bg-muted/50 hover:bg-muted text-muted-foreground hover:text-foreground border border-border/60"
              }`}
            >
              {cat.name}
            </button>
          ))}
        </div>
      </div>

      {/* 2. Advanced Filters Drawer */}
      {showAdvanced && (
        <div className="bg-card/90 dark:bg-card/40 rounded-3xl border border-border/80 p-4 sm:p-5 grid grid-cols-1 sm:grid-cols-3 gap-4 shadow-sm animate-in fade-in-50 duration-150">
          {/* Price Range Filter */}
          <div className="space-y-1.5 sm:col-span-2">
            <label className="block text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
              Khoảng giá (VNĐ)
            </label>
            <div className="flex items-center gap-2">
              <input
                type="number"
                value={minPriceInput}
                onChange={(e) => setMinPriceInput(e.target.value)}
                placeholder="Giá từ..."
                className="w-full h-10 px-3 bg-muted/50 rounded-xl border border-border focus:border-primary text-xs text-foreground outline-none"
              />
              <span className="text-muted-foreground font-bold">-</span>
              <input
                type="number"
                value={maxPriceInput}
                onChange={(e) => setMaxPriceInput(e.target.value)}
                placeholder="Đến giá..."
                className="w-full h-10 px-3 bg-muted/50 rounded-xl border border-border focus:border-primary text-xs text-foreground outline-none"
              />
              <button
                type="button"
                onClick={handleApplyPriceRange}
                className="h-10 px-4 rounded-xl bg-primary hover:bg-primary/90 text-primary-foreground text-xs font-bold cursor-pointer shrink-0"
              >
                Áp dụng
              </button>
            </div>
          </div>

          {/* Billing Cycle Selector */}
          <div className="space-y-1.5">
            <label className="block text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
              Chu kỳ thanh toán
            </label>
            <select
              value={billingCycle || ""}
              onChange={(e) => onBillingCycleChange((e.target.value || "") as BillingCycle | "")}
              className="w-full h-10 px-3 bg-muted/50 rounded-xl border border-border focus:border-primary text-xs text-foreground outline-none cursor-pointer"
            >
              <option value="">Tất cả chu kỳ</option>
              <option value="DAY">Theo ngày</option>
              <option value="MONTH">Theo tháng</option>
              <option value="YEAR">Theo năm</option>
              <option value="ONE_TIME">Một lần / Theo lượt</option>
            </select>
          </div>
        </div>
      )}
    </div>
  );
}
