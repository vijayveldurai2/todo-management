import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAppSelector } from '../../store';

export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAppSelector((state) => state.auth);
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[var(--bg-surface)] flex flex-col items-center justify-center p-6 text-[var(--text-on-surface)]">
        <div className="flex items-center gap-3 text-xs font-semibold text-[var(--text-on-surface-variant)]">
          <div className="w-5 h-5 border-2 border-[var(--color-primary)] border-t-transparent rounded-full animate-spin"></div>
          <span>Checking authorization...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
};
