import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store';
import {
  setCreateTaskModalOpen,
  setCreateProjectModalOpen,
  setCreateBoardModalOpen,
  setShortcutsModalOpen,
  setSettingsModalOpen,
  setActiveView,
} from '../store/uiSlice';

export interface ShortcutItem {
  key: string;
  description: string;
  category: 'Actions' | 'Navigation' | 'System';
}

export const SHORTCUT_LIST: ShortcutItem[] = [
  { key: 'C or N', description: 'Quickly create a new task', category: 'Actions' },
  { key: 'P', description: 'Create a new project', category: 'Actions' },
  { key: 'B', description: 'Create a new board', category: 'Actions' },
  { key: '1 or K', description: 'Navigate to Kanban Board', category: 'Navigation' },
  { key: '2 or T', description: 'Navigate to Task List View', category: 'Navigation' },
  { key: '3 or S', description: 'Navigate to Sprint Board', category: 'Navigation' },
  { key: '4 or A', description: 'Navigate to Calendar View', category: 'Navigation' },
  { key: '0 or W', description: 'Navigate to Workspace Overview', category: 'Navigation' },
  { key: '? or Cmd+/', description: 'Show Keyboard Shortcuts helper', category: 'System' },
  { key: 'Esc', description: 'Close current modal or panel', category: 'System' },
];

export function useKeyboardShortcuts() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { workspaceSlug = 'main-workspace' } = useParams<{ workspaceSlug?: string }>();
  const uiState = useAppSelector((state) => state.ui);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const activeElement = document.activeElement as HTMLElement | null;
      const isTyping =
        activeElement &&
        (activeElement.tagName === 'INPUT' ||
          activeElement.tagName === 'TEXTAREA' ||
          activeElement.tagName === 'SELECT' ||
          activeElement.isContentEditable);

      const isCmdOrCtrl = event.metaKey || event.ctrlKey;
      const key = event.key.toLowerCase();

      // System Escape key always works to close open modals
      if (key === 'escape') {
        if (uiState.isShortcutsModalOpen) dispatch(setShortcutsModalOpen(false));
        if (uiState.isCreateTaskModalOpen) dispatch(setCreateTaskModalOpen(false));
        if (uiState.isCreateProjectModalOpen) dispatch(setCreateProjectModalOpen(false));
        if (uiState.isCreateBoardModalOpen) dispatch(setCreateBoardModalOpen(false));
        if (uiState.isSettingsModalOpen) dispatch(setSettingsModalOpen(false));
        return;
      }

      // Cmd/Ctrl + / or ? triggers shortcut modal
      if ((isCmdOrCtrl && key === '/') || (event.key === '?' && !isTyping)) {
        event.preventDefault();
        dispatch(setShortcutsModalOpen(!uiState.isShortcutsModalOpen));
        return;
      }

      // Ignore standard single-key shortcuts when typing in inputs/textareas
      if (isTyping) return;

      // Single-key or modifier triggers
      if (key === 'c' || key === 'n') {
        event.preventDefault();
        dispatch(setCreateTaskModalOpen(true));
      } else if (key === 'p') {
        event.preventDefault();
        dispatch(setCreateProjectModalOpen(true));
      } else if (key === 'b') {
        event.preventDefault();
        dispatch(setCreateBoardModalOpen(true));
      } else if (key === '1' || key === 'k') {
        event.preventDefault();
        dispatch(setActiveView('board'));
        navigate('/kanban');
      } else if (key === '2' || key === 't') {
        event.preventDefault();
        dispatch(setActiveView('list'));
        navigate('/tasks');
      } else if (key === '3' || key === 's') {
        event.preventDefault();
        dispatch(setActiveView('sprint'));
        navigate('/sprint');
      } else if (key === '4' || key === 'a') {
        event.preventDefault();
        dispatch(setActiveView('calendar'));
        navigate('/calendar');
      } else if (key === '0' || key === 'w') {
        event.preventDefault();
        dispatch(setActiveView('workspace'));
        navigate(`/${workspaceSlug}`);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [dispatch, navigate, workspaceSlug, uiState]);
}
