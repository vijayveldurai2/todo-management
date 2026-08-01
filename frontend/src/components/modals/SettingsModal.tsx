import React from 'react';
import { useAppDispatch, useAppSelector } from '../../store';
import { setSettingsModalOpen } from '../../store/uiSlice';
import { toggleDarkMode, setColorTheme } from '../../store/themeSlice';
import { setOAuthModalOpen } from '../../store/authSlice';
import { apiService } from '../../services/apiService';
import { fetchProjects } from '../../store/projectsSlice';
import { fetchTasks } from '../../store/tasksSlice';
import { ThemeColor } from '../../types';

export const SettingsModal: React.FC = () => {
  const dispatch = useAppDispatch();
  const { isSettingsModalOpen } = useAppSelector((state) => state.ui);
  const { mode, colorTheme, isOnline } = useAppSelector((state) => state.theme);
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);

  if (!isSettingsModalOpen) return null;

  const themes: { id: ThemeColor; name: string; hex: string }[] = [
    { id: 'indigo', name: 'Royal Indigo', hex: '#3525cd' },
    { id: 'emerald', name: 'Emerald', hex: '#059669' },
    { id: 'rose', name: 'Rose Red', hex: '#e11d48' },
    { id: 'amber', name: 'Warm Amber', hex: '#d97706' },
    { id: 'cyan', name: 'Sky Cyan', hex: '#0284c7' },
    { id: 'violet', name: 'Violet', hex: '#7c3aed' },
  ];

  const handleResetData = async () => {
    if (confirm('Reset application state and static mock data cache?')) {
      await apiService.resetToDefaults();
      await dispatch(fetchProjects());
      await dispatch(fetchTasks(undefined));
      dispatch(setSettingsModalOpen(false));
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
      <div className="bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] rounded-2xl shadow-2xl w-full max-w-md p-6 relative animate-in fade-in zoom-in-95 duration-200">
        <button
          onClick={() => dispatch(setSettingsModalOpen(false))}
          className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] cursor-pointer"
        >
          <span className="material-symbols-outlined text-lg">close</span>
        </button>

        <div className="flex items-center gap-3 mb-5">
          <div className="w-10 h-10 rounded-xl bg-[var(--color-primary-fixed)] text-[var(--color-primary)] flex items-center justify-center font-bold">
            <span className="material-symbols-outlined text-xl">settings</span>
          </div>
          <div>
            <h2 className="text-lg font-bold text-[var(--text-on-surface)]">Preferences & Settings</h2>
            <p className="text-xs text-[var(--text-on-surface-variant)]">Theme, dark mode, offline engine & session</p>
          </div>
        </div>

        <div className="space-y-5 text-xs">
          {/* Appearance */}
          <div>
            <label className="block font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-2">
              Appearance & Theme
            </label>
            <div className="flex items-center justify-between p-3 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] mb-3">
              <span className="font-semibold text-[var(--text-on-surface)]">Dark Mode</span>
              <button
                onClick={() => dispatch(toggleDarkMode())}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors cursor-pointer ${
                  mode === 'dark' ? 'bg-[var(--color-primary)]' : 'bg-slate-300'
                }`}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                    mode === 'dark' ? 'translate-x-6' : 'translate-x-1'
                  }`}
                />
              </button>
            </div>

            <div className="grid grid-cols-3 gap-2">
              {themes.map((t) => (
                <button
                  key={t.id}
                  onClick={() => dispatch(setColorTheme(t.id))}
                  className={`flex items-center gap-2 p-2 rounded-xl border text-left cursor-pointer transition-colors ${
                    colorTheme === t.id
                      ? 'border-[var(--color-primary)] bg-[var(--color-primary-fixed)] font-bold text-[var(--color-primary)]'
                      : 'border-[var(--border-outline-variant)] bg-[var(--bg-surface-container-low)] text-[var(--text-on-surface-variant)] hover:bg-[var(--bg-surface-container-high)]'
                  }`}
                >
                  <span className="w-3.5 h-3.5 rounded-full shrink-0" style={{ backgroundColor: t.hex }} />
                  <span className="truncate">{t.name}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Session */}
          <div>
            <label className="block font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-2">
              OAuth Session Status
            </label>
            <div className="p-3 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] flex items-center justify-between">
              <div>
                <p className="font-semibold text-[var(--text-on-surface)]">
                  {isAuthenticated && user ? user.email : 'Not signed in'}
                </p>
                <p className="text-[10px] text-[var(--text-on-surface-variant)]">
                  {isAuthenticated ? `OAuth token active (${user?.provider})` : 'Guest session active'}
                </p>
              </div>
              <button
                onClick={() => {
                  dispatch(setSettingsModalOpen(false));
                  dispatch(setOAuthModalOpen(true));
                }}
                className="px-3 py-1.5 rounded-lg bg-[var(--color-primary)] text-white font-semibold cursor-pointer"
              >
                Manage
              </button>
            </div>
          </div>

          {/* Offline Sync & Data Reset */}
          <div>
            <label className="block font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-2">
              Offline & Static Cache Engine
            </label>
            <div className="p-3 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-[var(--text-on-surface)]">Network Connection:</span>
                <span
                  className={`font-bold px-2 py-0.5 rounded text-[10px] ${
                    isOnline
                      ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
                      : 'bg-amber-500/10 text-amber-600 dark:text-amber-400'
                  }`}
                >
                  {isOnline ? 'Online Sync' : 'Offline Mode'}
                </span>
              </div>
              <button
                onClick={handleResetData}
                className="w-full py-2 rounded-lg bg-red-500/10 text-red-600 dark:text-red-400 font-semibold border border-red-500/20 hover:bg-red-500/20 transition-colors cursor-pointer"
              >
                Reset Static Data to Defaults
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
