import { useState, useEffect, useCallback, useMemo } from "react";
import userService from "@/services/user.service";
import type { UserPermissions } from "@/types/permission.type";
import { useAuth } from "@/features/auth/useAuth";

let cachedPermissions: UserPermissions | null = null;
let pendingPromise: Promise<UserPermissions> | null = null;

export function usePermissions() {
  const { authenticated } = useAuth();
  const [data, setData] = useState<UserPermissions | null>(cachedPermissions);
  const [loading, setLoading] = useState<boolean>(!cachedPermissions && authenticated);

  const fetchPermissions = useCallback(async (force = false) => {
    if (!authenticated) {
      cachedPermissions = null;
      return;
    }

    if (cachedPermissions && !force) {
      return;
    }

    if (!pendingPromise || force) {
      pendingPromise = userService
        .getMyPermissions()
        .then((res) => {
          cachedPermissions = res;
          return res;
        })
        .finally(() => {
          pendingPromise = null;
        });
    }

    try {
      const res = await pendingPromise;
      setData(res);
    } catch {
      // Fallback empty permissions on error
      setData({ userId: "", username: "", roles: [], permissions: [] });
    } finally {
      setLoading(false);
    }
  }, [authenticated]);

  useEffect(() => {
    if (!authenticated) {
      cachedPermissions = null;
      return;
    }

    const timer = setTimeout(() => {
      fetchPermissions();
    }, 0);

    return () => clearTimeout(timer);
  }, [authenticated, fetchPermissions]);

  const roles = useMemo(() => data?.roles || [], [data]);
  const permissions = useMemo(() => data?.permissions || [], [data]);
  const isAdmin = useMemo(() => roles.includes("ADMIN"), [roles]);

  const hasPermission = useCallback(
    (required: string | string[]): boolean => {
      if (isAdmin) return true;
      if (Array.isArray(required)) {
        return required.some((p) => permissions.includes(p));
      }
      return permissions.includes(required);
    },
    [isAdmin, permissions],
  );

  const hasRole = useCallback(
    (required: string | string[]): boolean => {
      if (Array.isArray(required)) {
        return required.some((r) => roles.includes(r));
      }
      return roles.includes(required);
    },
    [roles],
  );

  return {
    roles,
    permissions,
    isAdmin,
    loading,
    hasPermission,
    hasRole,
    can: hasPermission,
    refetch: () => fetchPermissions(true),
  };
}
