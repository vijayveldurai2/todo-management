import React, { useEffect, useState } from 'react';
import { Outlet, useParams } from 'react-router-dom';
import { KanbanBoardView } from './KanbanBoardView';
import { TaskListView } from './TaskListView';
import { CalendarView } from './CalendarView';
import { SprintBoardView } from './SprintBoardView';
import { NotFoundPage } from './NotFoundPage';
import { apiService } from '../../services/apiService';
import { Board } from '../../types';

export const BoardView: React.FC = () => {
  const { workspaceSlug, projectSlug, boardSlug } = useParams<{
    workspaceSlug: string;
    projectSlug: string;
    boardSlug: string;
  }>();

  const [board, setBoard] = useState<Board | null>(null);
  const [isNotFound, setIsNotFound] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    if (!workspaceSlug || !projectSlug || !boardSlug) return;

    setIsLoading(true);
    setIsNotFound(false);

    apiService
      .getBoardBySlug(workspaceSlug, projectSlug, boardSlug)
      .then((b) => {
        if (isMounted) {
          setBoard(b);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          console.warn('Board not found:', err);
          setIsNotFound(true);
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [workspaceSlug, projectSlug, boardSlug]);

  if (isLoading) {
    return (
      <div className="py-12 flex justify-center items-center text-xs text-[var(--text-on-surface-variant)]">
        <div className="w-5 h-5 border-2 border-[var(--color-primary)] border-t-transparent rounded-full animate-spin mr-2"></div>
        <span>Loading board...</span>
      </div>
    );
  }

  if (isNotFound || !board) {
    return <NotFoundPage type="board" />;
  }

  const renderBoardContent = () => {
    switch (board.type) {
      case 'Sprint':
        return <SprintBoardView board={board} />;
      case 'List':
        return <TaskListView board={board} />;
      case 'Calendar':
        return <CalendarView board={board} />;
      case 'Kanban':
      default:
        return <KanbanBoardView board={board} />;
    }
  };

  return (
    <div className="relative">
      {renderBoardContent()}

      {/* Outlet for nested todo panel overlay: /:workspaceSlug/:projectSlug/:boardSlug/todo/:todoSlug */}
      <Outlet context={{ board }} />
    </div>
  );
};
