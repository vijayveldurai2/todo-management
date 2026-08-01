import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { ThemeColor, ThemeMode } from '../types';

interface ThemeState {
  mode: ThemeMode;
  colorTheme: ThemeColor;
  isOnline: boolean;
}

const getInitialMode = (): ThemeMode => {
  const saved = localStorage.getItem('taskflow_theme_mode');
  if (saved === 'dark' || saved === 'light') return saved;
  return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

const getInitialColor = (): ThemeColor => {
  const saved = localStorage.getItem('taskflow_theme_color');
  if (saved && ['indigo', 'emerald', 'rose', 'amber', 'cyan', 'violet'].includes(saved)) {
    return saved as ThemeColor;
  }
  return 'indigo';
};

const initialState: ThemeState = {
  mode: getInitialMode(),
  colorTheme: getInitialColor(),
  isOnline: typeof navigator !== 'undefined' ? navigator.onLine : true,
};

const themeSlice = createSlice({
  name: 'theme',
  initialState,
  reducers: {
    toggleDarkMode: (state) => {
      state.mode = state.mode === 'light' ? 'dark' : 'light';
      localStorage.setItem('taskflow_theme_mode', state.mode);
    },
    setDarkMode: (state, action: PayloadAction<ThemeMode>) => {
      state.mode = action.payload;
      localStorage.setItem('taskflow_theme_mode', state.mode);
    },
    setColorTheme: (state, action: PayloadAction<ThemeColor>) => {
      state.colorTheme = action.payload;
      localStorage.setItem('taskflow_theme_color', action.payload);
    },
    setOnlineStatus: (state, action: PayloadAction<boolean>) => {
      state.isOnline = action.payload;
    },
  },
});

export const { toggleDarkMode, setDarkMode, setColorTheme, setOnlineStatus } = themeSlice.actions;
export default themeSlice.reducer;
