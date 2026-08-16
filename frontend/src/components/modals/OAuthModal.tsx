import React from 'react';
import { useAppDispatch, useAppSelector } from '../../store';
import { loginWithOAuth, logoutUser, setOAuthModalOpen } from '../../store/authSlice';

export const OAuthModal: React.FC = () => {
  const dispatch = useAppDispatch();
  const { showOAuthModal, user, isAuthenticated, isLoading } = useAppSelector((state) => state.auth);

  if (!showOAuthModal) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
      <div className="bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] rounded-2xl shadow-2xl w-full max-w-md p-6 relative overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <button
          onClick={() => dispatch(setOAuthModalOpen(false))}
          className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] cursor-pointer"
        >
          <span className="material-symbols-outlined text-lg">close</span>
        </button>

        <div className="text-center mb-6">
          <div className="w-12 h-12 rounded-2xl bg-[var(--color-primary-fixed)] text-[var(--color-primary)] mx-auto flex items-center justify-center mb-3">
            <span className="material-symbols-outlined text-2xl font-bold">verified_user</span>
          </div>
          <h2 className="text-xl font-bold text-[var(--text-on-surface)]">
            {isAuthenticated ? 'OAuth User Session' : 'OAuth Authentication'}
          </h2>
          <p className="text-xs text-[var(--text-on-surface-variant)] mt-1">
            {isAuthenticated
              ? 'Your user session is active and securely authenticated.'
              : 'Sign in to access team workspaces, sync projects, and manage user sessions.'}
          </p>
        </div>

        {isAuthenticated && user ? (
          <div className="space-y-4">
            <div className="p-4 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] flex items-center gap-3">
              <img src={user.avatar} alt={user.name} className="w-12 h-12 rounded-full object-cover" />
              <div>
                <h4 className="font-bold text-sm text-[var(--text-on-surface)]">{user.name}</h4>
                <p className="text-xs text-[var(--text-on-surface-variant)]">{user.email}</p>
                <p className="text-[10px] text-emerald-600 dark:text-emerald-400 font-bold uppercase mt-1">
                  Provider: {user.provider} OAuth 2.0
                </p>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-[var(--bg-surface-container)] text-xs space-y-1.5 text-[var(--text-on-surface-variant)]">
              <div className="flex justify-between">
                <span>Access Token:</span>
                <span className="font-mono font-semibold text-[var(--text-on-surface)]">
                  {user.accessToken?.slice(0, 16)}...
                </span>
              </div>
              <div className="flex justify-between">
                <span>Session Expiry:</span>
                <span className="font-semibold text-[var(--text-on-surface)]">
                  {user.expiresAt ? new Date(user.expiresAt).toLocaleTimeString() : 'Active'}
                </span>
              </div>
              <div className="flex justify-between">
                <span>Role Permission:</span>
                <span className="font-semibold text-[var(--text-on-surface)]">{user.role || 'Admin'}</span>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => dispatch(logoutUser())}
                className="flex-1 py-2.5 rounded-xl bg-red-600 text-white font-semibold text-xs shadow-xs hover:bg-red-700 transition-colors cursor-pointer"
              >
                Sign Out Session
              </button>
              <button
                onClick={() => dispatch(setOAuthModalOpen(false))}
                className="flex-1 py-2.5 rounded-xl bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface)] font-semibold text-xs hover:bg-[var(--bg-surface-container-highest)] transition-colors cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            <button
              disabled={isLoading}
              onClick={() => dispatch(loginWithOAuth('google'))}
              className="w-full flex items-center justify-center gap-3 py-3 px-4 rounded-xl border border-[var(--border-outline-variant)] bg-[var(--bg-surface-container-lowest)] hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface)] font-semibold text-sm shadow-xs transition-all cursor-pointer"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                />
              </svg>
              <span>Continue with Google OAuth</span>
            </button>

            <button
              disabled={isLoading}
              onClick={() => dispatch(loginWithOAuth('github'))}
              className="w-full flex items-center justify-center gap-3 py-3 px-4 rounded-xl bg-slate-900 text-white hover:bg-slate-800 font-semibold text-sm shadow-xs transition-all cursor-pointer"
            >
              <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
              </svg>
              <span>Continue with GitHub OAuth</span>
            </button>

            <div className="pt-2 text-center text-[11px] text-[var(--text-on-surface-variant)]">
              By continuing, you accept task session management & local cache persistence.
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
