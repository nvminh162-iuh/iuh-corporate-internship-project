import { Routes, Route, Navigate } from "react-router-dom";
import AdminLayout from "@/layouts/AdminLayout";
import AuthGuard from "@/features/auth/AuthGuard";

// Pages
import LoginPage from "@/pages/login/LoginPage";
import DashboardPage from "@/pages/dashboard/DashboardPage";
import UsersPage from "@/pages/users/UsersPage";
import RolesPage from "@/pages/users/RolesPage";
import PermissionsPage from "@/pages/users/PermissionsPage";
import SettingsPage from "@/pages/settings/SettingsPage";
import NotFoundPage from "@/pages/not-found/NotFoundPage";

export default function AppRouter() {
  return (
    <Routes>
      {/* Public Login Route */}
      <Route path="/" element={<LoginPage />} />
      <Route path="/login" element={<Navigate to="/" replace />} />

      {/* Protected Admin Routes */}
      <Route
        element={
          <AuthGuard>
            <AdminLayout />
          </AuthGuard>
        }
      >
        {/* Dashboard Route */}
        <Route path="/dashboard" element={<DashboardPage />} />

        {/* User Management Routes */}
        <Route path="users">
          <Route index element={<UsersPage />} />
          <Route path="roles" element={<RolesPage />} />
          <Route path="permissions" element={<PermissionsPage />} />
        </Route>

        {/* Support /dashboard/users aliases */}
        <Route path="/dashboard/users" element={<Navigate to="/users" replace />} />
        <Route path="/dashboard/users/roles" element={<Navigate to="/users/roles" replace />} />
        <Route path="/dashboard/users/permissions" element={<Navigate to="/users/permissions" replace />} />

        {/* Settings */}
        <Route path="settings" element={<SettingsPage />} />

        {/* 404 Inside Admin Layout */}
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
