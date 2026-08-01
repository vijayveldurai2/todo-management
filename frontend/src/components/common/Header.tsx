import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { setActiveView, setSearchQuery, setSettingsModalOpen, setShortcutsModalOpen } from '../../store/uiSlice';
import { toggleDarkMode, setColorTheme } from '../../store/themeSlice';
import { setOAuthModalOpen, logoutUser } from '../../store/authSlice';
import { ThemeColor, ActiveView } from '../../types';

export const Header: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { mode, colorTheme, isOnline } = useAppSelector((state) => state.theme);
  const { activeView, searchQuery } = useAppSelector((state) => state.ui);
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);

  const [isThemeMenuOpen, setIsThemeMenuOpen] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);

  const themes: { id: ThemeColor; name: string; colorHex: string }[] = [
    { id: 'indigo', name: 'Royal Indigo', colorHex: '#3525cd' },
    { id: 'emerald', name: 'Emerald Green', colorHex: '#059669' },
    { id: 'rose', name: 'Rose Crimson', colorHex: '#e11d48' },
    { id: 'amber', name: 'Warm Amber', colorHex: '#d97706' },
    { id: 'cyan', name: 'Sky Cyan', colorHex: '#0284c7' },
    { id: 'violet', name: 'Deep Violet', colorHex: '#7c3aed' },
  ];

  const handleNavClick = (view: ActiveView) => {
    dispatch(setActiveView(view));
  };

  return (
    <header className="fixed top-0 w-full z-50 flex justify-between items-center px-4 md:px-6 h-16 bg-[var(--bg-surface)] border-b border-[var(--border-outline-variant)] shadow-xs transition-colors">
      {/* Left: Brand & Nav Links */}
      <div className="flex items-center gap-4 md:gap-8">
        <button
          onClick={() => handleNavClick('workspace')}
          className="font-bold text-xl text-[var(--color-primary)] dark:text-indigo-400 hover:opacity-90 flex items-center gap-2 cursor-pointer"
        >
          <span>TaskFlow</span>
          {!isOnline && (
            <span className="text-[10px] px-1.5 py-0.5 bg-amber-500/20 text-amber-600 dark:text-amber-400 rounded font-semibold border border-amber-500/30">
              Offline
            </span>
          )}
        </button>

        <nav className="hidden md:flex items-center gap-6 font-medium text-sm">
          <button
            onClick={() => handleNavClick('workspace')}
            className={`py-1 cursor-pointer transition-colors ${
              activeView === 'workspace'
                ? 'text-[var(--color-primary)] font-semibold border-b-2 border-[var(--color-primary)]'
                : 'text-[var(--text-on-surface-variant)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            Workspace
          </button>
          <button
            onClick={() => handleNavClick('projects')}
            className={`py-1 cursor-pointer transition-colors ${
              activeView === 'projects' || activeView === 'board'
                ? 'text-[var(--color-primary)] font-semibold border-b-2 border-[var(--color-primary)]'
                : 'text-[var(--text-on-surface-variant)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            Projects
          </button>
          <button
            onClick={() => handleNavClick('my-tasks')}
            className={`py-1 cursor-pointer transition-colors ${
              activeView === 'my-tasks' || activeView === 'list'
                ? 'text-[var(--color-primary)] font-semibold border-b-2 border-[var(--color-primary)]'
                : 'text-[var(--text-on-surface-variant)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            My Tasks
          </button>
          <button
            onClick={() => handleNavClick('calendar')}
            className={`py-1 cursor-pointer transition-colors ${
              activeView === 'calendar'
                ? 'text-[var(--color-primary)] font-semibold border-b-2 border-[var(--color-primary)]'
                : 'text-[var(--text-on-surface-variant)] hover:text-[var(--text-on-surface)]'
            }`}
          >
            Calendar
          </button>
        </nav>
      </div>

      {/* Right: Search, Theme, Auth */}
      <div className="flex items-center gap-2 md:gap-4">
        {/* Search Field */}
        <div className="hidden sm:flex items-center bg-[var(--bg-surface-container-low)] px-3 py-1.5 rounded-full border border-[var(--border-outline-variant)]">
          <span className="material-symbols-outlined text-[var(--text-on-surface-variant)] text-lg mr-2">
            search
          </span>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => dispatch(setSearchQuery(e.target.value))}
            placeholder="Search tasks or projects..."
            className="bg-transparent border-none outline-none focus:ring-0 text-sm w-44 lg:w-60 text-[var(--text-on-surface)]"
          />
        </div>

        {/* Color Theme Selector Dropdown */}
        <div className="relative">
          <button
            onClick={() => setIsThemeMenuOpen(!isThemeMenuOpen)}
            title="Change Color Palette Theme"
            className="p-2 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] transition-colors cursor-pointer flex items-center justify-center"
          >
            <span className="material-symbols-outlined text-xl" style={{ color: 'var(--color-primary)' }}>
              palette
            </span>
          </button>

          {isThemeMenuOpen && (
            <div className="absolute right-0 mt-2 w-52 bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] rounded-xl shadow-xl p-3 z-50">
              <p className="text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-2 px-1">
                Color Accent Theme
              </p>
              <div className="grid grid-cols-2 gap-1.5">
                {themes.map((t) => (
                  <button
                    key={t.id}
                    onClick={() => {
                      dispatch(setColorTheme(t.id));
                      setIsThemeMenuOpen(false);
                    }}
                    className={`flex items-center gap-2 px-2 py-1.5 rounded-lg text-xs font-medium cursor-pointer transition-colors ${
                      colorTheme === t.id
                        ? 'bg-[var(--bg-surface-container-high)] font-bold text-[var(--text-on-surface)]'
                        : 'hover:bg-[var(--bg-surface-container)] text-[var(--text-on-surface-variant)]'
                    }`}
                  >
                    <span
                      className="w-3.5 h-3.5 rounded-full border border-white/20"
                      style={{ backgroundColor: t.colorHex }}
                    />
                    <span className="truncate">{t.name.split(' ')[0]}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Dark Mode Toggle */}
        <button
          onClick={() => dispatch(toggleDarkMode())}
          title={mode === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
          className="p-2 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] transition-colors cursor-pointer flex items-center justify-center"
        >
          <span className="material-symbols-outlined text-xl">
            {mode === 'dark' ? 'light_mode' : 'dark_mode'}
          </span>
        </button>

        {/* Keyboard Shortcuts button */}
        <button
          onClick={() => dispatch(setShortcutsModalOpen(true))}
          title="Keyboard Shortcuts (? or Cmd+/)"
          className="p-2 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] transition-colors cursor-pointer flex items-center justify-center hidden sm:flex"
        >
          <span className="material-symbols-outlined text-xl">keyboard</span>
        </button>

        {/* Settings button */}
        <button
          onClick={() => dispatch(setSettingsModalOpen(true))}
          title="App Settings"
          className="p-2 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] transition-colors cursor-pointer flex items-center justify-center hidden sm:flex"
        >
          <span className="material-symbols-outlined text-xl">settings</span>
        </button>

        {/* Notifications */}
        <button
          title="Notifications"
          className="p-2 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] transition-colors cursor-pointer relative"
        >
          <span className="material-symbols-outlined text-xl">notifications</span>
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-600 rounded-full"></span>
        </button>

        {/* User Profile / OAuth Trigger */}
        <div className="relative">
          {isAuthenticated && user ? (
            <button
              onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
              className="w-8 h-8 rounded-full overflow-hidden border border-[var(--border-outline-variant)] cursor-pointer active:scale-95 transition-transform"
            >
              {user.avatar ? (
                <img src={user.avatar} alt={user.name} className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full bg-[var(--color-primary)] text-white font-bold flex items-center justify-center text-xs">
                  {user.name.slice(0, 2).toUpperCase()}
                </div>
              )}
            </button>
          ) : (
            <button
              onClick={() => {
                dispatch(setActiveView('login'));
                navigate('/login');
              }}
              className="px-3 py-1.5 rounded-lg bg-[var(--color-primary)] text-white font-semibold text-xs shadow-xs hover:opacity-90 active:scale-95 transition-all cursor-pointer"
            >
              Sign In
            </button>
          )}

          {isUserMenuOpen && isAuthenticated && user && (
            <div className="absolute right-0 mt-2 w-64 bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] rounded-xl shadow-xl p-3 z-50">
              <div className="flex items-center gap-3 p-2 border-b border-[var(--border-outline-variant)] pb-3 mb-2">
                <img src={user.avatar} alt={user.name} className="w-10 h-10 rounded-full object-cover" />
                <div className="overflow-hidden">
                  <p className="font-semibold text-sm truncate text-[var(--text-on-surface)]">{user.name}</p>
                  <p className="text-xs text-[var(--text-on-surface-variant)] truncate">{user.email}</p>
                  <span className="inline-block mt-1 px-1.5 py-0.2 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 text-[10px] font-bold rounded uppercase">
                    OAuth Verified ({user.provider})
                  </span>
                </div>
              </div>

              <div className="space-y-1 text-xs text-[var(--text-on-surface-variant)]">
                <button
                  onClick={() => {
                    dispatch(setActiveView('login'));
                    navigate('/login');
                    setIsUserMenuOpen(false);
                  }}
                  className="w-full text-left px-2 py-1.5 rounded-lg hover:bg-[var(--bg-surface-container)] cursor-pointer flex items-center gap-2"
                >
                  <span className="material-symbols-outlined text-sm">login</span>
                  <span>View Login Screen</span>
                </button>

                <button
                  onClick={() => {
                    dispatch(setSettingsModalOpen(true));
                    setIsUserMenuOpen(false);
                  }}
                  className="w-full text-left px-2 py-1.5 rounded-lg hover:bg-[var(--bg-surface-container)] cursor-pointer flex items-center gap-2"
                >
                  <span className="material-symbols-outlined text-sm">tune</span>
                  <span>Preferences & Settings</span>
                </button>

                <button
                  onClick={() => {
                    dispatch(setOAuthModalOpen(true));
                    setIsUserMenuOpen(false);
                  }}
                  className="w-full text-left px-2 py-1.5 rounded-lg hover:bg-[var(--bg-surface-container)] cursor-pointer flex items-center gap-2"
                >
                  <span className="material-symbols-outlined text-sm">switch_account</span>
                  <span>Manage OAuth Session</span>
                </button>

                <button
                  onClick={() => {
                    dispatch(logoutUser());
                    dispatch(setActiveView('login'));
                    navigate('/login');
                    setIsUserMenuOpen(false);
                  }}
                  className="w-full text-left px-2 py-1.5 rounded-lg hover:bg-red-500/10 text-red-600 dark:text-red-400 font-semibold cursor-pointer flex items-center gap-2"
                >
                  <span className="material-symbols-outlined text-sm">logout</span>
                  <span>Sign Out</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
