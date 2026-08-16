import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { ActiveView } from '../types';

interface UIState {
  activeView: ActiveView;
  activeProjectId: string;
  activeBoardId: string;
  selectedTaskId: string | null;
  searchQuery: string;
  isCreateProjectModalOpen: boolean;
  isCreateTaskModalOpen: boolean;
  isCreateBoardModalOpen: boolean;
  isSettingsModalOpen: boolean;
  isShortcutsModalOpen: boolean;
}

const initialState: UIState = {
  activeView: 'workspace',
  activeProjectId: 'proj-1',
  activeBoardId: 'board-1',
  selectedTaskId: null,
  searchQuery: '',
  isCreateProjectModalOpen: false,
  isCreateTaskModalOpen: false,
  isCreateBoardModalOpen: false,
  isSettingsModalOpen: false,
  isShortcutsModalOpen: false,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setActiveView: (state, action: PayloadAction<ActiveView>) => {
      state.activeView = action.payload;
    },
    setActiveProject: (state, action: PayloadAction<string>) => {
      state.activeProjectId = action.payload;
    },
    setActiveBoard: (state, action: PayloadAction<string>) => {
      state.activeBoardId = action.payload;
    },
    setSelectedTask: (state, action: PayloadAction<string | null>) => {
      state.selectedTaskId = action.payload;
    },
    setSearchQuery: (state, action: PayloadAction<string>) => {
      state.searchQuery = action.payload;
    },
    setCreateProjectModalOpen: (state, action: PayloadAction<boolean>) => {
      state.isCreateProjectModalOpen = action.payload;
    },
    setCreateTaskModalOpen: (state, action: PayloadAction<boolean>) => {
      state.isCreateTaskModalOpen = action.payload;
    },
    setCreateBoardModalOpen: (state, action: PayloadAction<boolean>) => {
      state.isCreateBoardModalOpen = action.payload;
    },
    setSettingsModalOpen: (state, action: PayloadAction<boolean>) => {
      state.isSettingsModalOpen = action.payload;
    },
    setShortcutsModalOpen: (state, action: PayloadAction<boolean>) => {
      state.isShortcutsModalOpen = action.payload;
    },
  },
});

export const {
  setActiveView,
  setActiveProject,
  setActiveBoard,
  setSelectedTask,
  setSearchQuery,
  setCreateProjectModalOpen,
  setCreateTaskModalOpen,
  setCreateBoardModalOpen,
  setSettingsModalOpen,
  setShortcutsModalOpen,
} = uiSlice.actions;

export default uiSlice.reducer;
