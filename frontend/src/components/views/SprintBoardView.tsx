import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  TouchSensor,
  useSensor,
  useSensors,
  closestCenter,
  DragStartEvent,
  DragOverEvent,
  DragEndEvent,
  DropAnimation,
  defaultDropAnimationSideEffects,
} from '@dnd-kit/core';
import {
  SortableContext,
  useSortable,
  horizontalListSortingStrategy,
  verticalListSortingStrategy,
  arrayMove,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useAppDispatch, useAppSelector } from '../../store';
import { fetchTasks } from '../../store/tasksSlice';
import { setCreateTaskModalOpen } from '../../store/uiSlice';
import { Task, Board, Column } from '../../types';
import { RoleBadge } from '../common/RoleBadge';
import { apiService } from '../../services/apiService';

interface SprintBoardViewProps {
  board?: Board;
}

// Float position calculator for optimistic insertions
function calculatePosition<T extends { position?: number }>(items: T[], targetIndex: number): number {
  if (items.length === 0) return 1000;
  if (targetIndex <= 0) {
    return (items[0].position ?? 1000) / 2;
  }
  if (targetIndex >= items.length) {
    return (items[items.length - 1].position ?? 1000) + 1000;
  }
  const prevPos = items[targetIndex - 1].position ?? 1000;
  const nextPos = items[targetIndex].position ?? 1000;
  return (prevPos + nextPos) / 2;
}

const dropAnimation: DropAnimation = {
  sideEffects: defaultDropAnimationSideEffects({
    styles: {
      active: {
        opacity: '0.4',
      },
    },
  }),
};

export const SprintBoardView: React.FC<SprintBoardViewProps> = ({ board }) => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { workspaceSlug = 'main-workspace', projectSlug = 'brand-refresh', boardSlug = 'sprint-board' } = useParams<{
    workspaceSlug: string;
    projectSlug: string;
    boardSlug: string;
  }>();

  const { projects, boards } = useAppSelector((state) => state.projects);
  const { tasks } = useAppSelector((state) => state.tasks);

  const currentProject = projects.find((p) => p.slug === projectSlug || p.id === projectSlug) || projects[0];
  const currentBoard = board || boards.find((b) => b.slug === boardSlug || b.id === boardSlug) || boards.find((b) => b.type === 'Sprint') || boards[0];

  // Local state for optimistic UI updates
  const [columnsState, setColumnsState] = useState<Column[]>([]);
  const [tasksState, setTasksState] = useState<Task[]>([]);

  // Dragging state
  const [activeId, setActiveId] = useState<string | null>(null);
  const [activeType, setActiveType] = useState<'card' | 'column' | null>(null);

  useEffect(() => {
    dispatch(fetchTasks({ projectId: currentProject?.id, boardId: currentBoard?.id }));
  }, [dispatch, currentProject, currentBoard]);

  useEffect(() => {
    apiService.getColumns(currentBoard?.id).then((cols) => {
      setColumnsState(cols.sort((a, b) => a.position - b.position));
    });
  }, [currentBoard]);

  useEffect(() => {
    if (tasks && tasks.length > 0) {
      setTasksState([...tasks].sort((a, b) => (a.position || 0) - (b.position || 0)));
    }
  }, [tasks]);

  // Configure sensors per prompt requirements
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 8 },
    }),
    useSensor(TouchSensor, {
      activationConstraint: { delay: 150, tolerance: 5 },
    })
  );

  // Map cards by column ID
  const cardsByColumn = useMemo(() => {
    const map: Record<string, Task[]> = {};
    columnsState.forEach((col) => {
      map[col.id] = [];
    });

    tasksState.forEach((task) => {
      const colId = task.column_id || 'col-todo';
      if (!map[colId]) {
        map[colId] = [];
      }
      map[colId].push(task);
    });

    // Sort cards in each column by position
    Object.keys(map).forEach((colId) => {
      map[colId].sort((a, b) => (a.position || 0) - (b.position || 0));
    });

    return map;
  }, [columnsState, tasksState]);

  // Active items for DragOverlay
  const activeCard = useMemo(() => {
    if (activeType !== 'card' || !activeId) return null;
    return tasksState.find((t) => t.id === activeId) || null;
  }, [activeType, activeId, tasksState]);

  const activeColumn = useMemo(() => {
    if (activeType !== 'column' || !activeId) return null;
    return columnsState.find((c) => c.id === activeId) || null;
  }, [activeType, activeId, columnsState]);

  // Calculate Sprint metrics
  const totalCommittedPoints = tasksState.reduce((sum, t) => sum + (t.storyPoints || 1), 0);
  const completedPoints = tasksState
    .filter((t) => t.status === 'Done' || t.status === 'Completed' || t.column_id === 'col-done')
    .reduce((sum, t) => sum + (t.storyPoints || 1), 0);
  const remainingPoints = totalCommittedPoints - completedPoints;
  const progressPercent = totalCommittedPoints > 0 ? Math.round((completedPoints / totalCommittedPoints) * 100) : 0;

  const handleCardClick = (task: Task) => {
    const todoIdentifier = task.display_id || task.slug || task.id;
    if (workspaceSlug && projectSlug && boardSlug) {
      navigate(`/${workspaceSlug}/${projectSlug}/${boardSlug}/todo/${todoIdentifier}`);
    } else {
      navigate(`todo/${todoIdentifier}`);
    }
  };

  // Drag handlers
  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event;
    const type = active.data.current?.type as 'card' | 'column';
    setActiveId(active.id as string);
    setActiveType(type);
  };

  const handleDragOver = (event: DragOverEvent) => {
    const { active, over } = event;
    if (!over) return;

    const activeType = active.data.current?.type;
    if (activeType !== 'card') return;

    const activeCardId = active.id as string;
    const overId = over.id as string;

    const activeTask = tasksState.find((t) => t.id === activeCardId);
    if (!activeTask) return;

    // Determine target column and position
    let targetColumnId: string | null = null;
    const overIsColumn = columnsState.some((c) => c.id === overId);
    const overCard = tasksState.find((t) => t.id === overId);

    if (overIsColumn) {
      targetColumnId = overId;
    } else if (overCard) {
      targetColumnId = overCard.column_id || 'col-todo';
    }

    if (!targetColumnId) return;

    const currentColumnId = activeTask.column_id || 'col-todo';

    // Move card locally if column changed
    if (currentColumnId !== targetColumnId) {
      setTasksState((prevTasks) => {
        const updated = prevTasks.map((t) => {
          if (t.id === activeCardId) {
            const targetColObj = columnsState.find((c) => c.id === targetColumnId);
            return {
              ...t,
              column_id: targetColumnId,
              status: targetColObj ? (targetColObj.name as any) : t.status,
            };
          }
          return t;
        });
        return updated;
      });
    }
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveId(null);
    setActiveType(null);

    if (!over) return;

    const activeType = active.data.current?.type;

    if (activeType === 'column') {
      const activeColId = active.id as string;
      const overColId = over.id as string;

      if (activeColId === overColId) return;

      const oldIdx = columnsState.findIndex((c) => c.id === activeColId);
      const newIdx = columnsState.findIndex((c) => c.id === overColId);

      if (oldIdx !== -1 && newIdx !== -1) {
        const reordered: Column[] = arrayMove(columnsState, oldIdx, newIdx);
        // Calculate new float position
        const newPos = calculatePosition(
          reordered.filter((c) => c.id !== activeColId),
          newIdx
        );

        const updatedCols = reordered.map((c) => (c.id === activeColId ? { ...c, position: newPos } : c));
        const previousCols = [...columnsState];

        // Optimistic local update
        setColumnsState(updatedCols.sort((a, b) => a.position - b.position));

        // Persist to backend
        try {
          await apiService.moveColumn(activeColId, newPos);
        } catch (err) {
          console.error('Failed to move column:', err);
          // Rollback on failure
          setColumnsState(previousCols);
        }
      }
    } else if (activeType === 'card') {
      const activeCardId = active.id as string;
      const overId = over.id as string;

      const activeTask = tasksState.find((t) => t.id === activeCardId);
      if (!activeTask) return;

      // Identify target column
      let targetColumnId: string = activeTask.column_id || 'col-todo';
      const overIsColumn = columnsState.some((c) => c.id === overId);
      const overCard = tasksState.find((t) => t.id === overId);

      if (overIsColumn) {
        targetColumnId = overId;
      } else if (overCard) {
        targetColumnId = overCard.column_id || 'col-todo';
      }

      // Cards in target column (excluding dragged card)
      const targetCards = (cardsByColumn[targetColumnId] || []).filter((c) => c.id !== activeCardId);

      let targetIndex = targetCards.length;
      if (overCard && overCard.id !== activeCardId) {
        const overIdxInCol = targetCards.findIndex((c) => c.id === overCard.id);
        if (overIdxInCol !== -1) {
          targetIndex = overIdxInCol;
        }
      }

      const newPosition = calculatePosition(targetCards, targetIndex);
      const targetColObj = columnsState.find((c) => c.id === targetColumnId);

      const previousTasks = [...tasksState];

      // Optimistic local update
      setTasksState((prev) => {
        return prev
          .map((t) => {
            if (t.id === activeCardId) {
              return {
                ...t,
                column_id: targetColumnId,
                status: targetColObj ? (targetColObj.name as any) : t.status,
                position: newPosition,
              };
            }
            return t;
          })
          .sort((a, b) => (a.position || 0) - (b.position || 0));
      });

      // Persist to backend
      try {
        await apiService.moveCard(activeCardId, targetColumnId, newPosition);
      } catch (err) {
        console.error('Failed to move card:', err);
        // Rollback on failure
        setTasksState(previousTasks);
      }
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-xs text-[var(--text-on-surface-variant)]">
        <button onClick={() => navigate(`/${workspaceSlug}`)} className="hover:text-[var(--color-primary)] cursor-pointer">
          Workspace
        </button>
        <span className="material-symbols-outlined text-xs">chevron_right</span>
        <button onClick={() => navigate(`/${workspaceSlug}/${projectSlug}`)} className="hover:text-[var(--color-primary)] cursor-pointer">
          {currentProject?.name}
        </button>
        <span className="material-symbols-outlined text-xs">chevron_right</span>
        <span className="font-semibold text-[var(--text-on-surface)]">{currentBoard?.sprintName || currentBoard?.title}</span>
      </div>

      {/* Header Banner */}
      <div className="p-6 rounded-2xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)]">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="px-2.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/30">
                Sprint Board
              </span>
            </div>
            <h1 className="text-2xl font-extrabold text-[var(--text-on-surface)]">
              {currentBoard?.sprintName || currentBoard?.title}
            </h1>
            <p className="text-xs text-[var(--text-on-surface-variant)] mt-1 max-w-xl">
              {currentBoard?.goal || 'Sprint execution board with drag-and-drop & story point metrics.'}
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => dispatch(setCreateTaskModalOpen(true))}
              className="px-4 py-2.5 rounded-xl bg-[var(--color-primary)] text-white font-semibold text-xs shadow-xs hover:opacity-90 active:scale-95 transition-all cursor-pointer flex items-center gap-1.5"
            >
              <span className="material-symbols-outlined text-base">add</span>
              <span>Add Task</span>
            </button>
          </div>
        </div>

        {/* Sprint Stats Bar */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] text-xs">
          <div>
            <span className="text-[10px] font-bold uppercase text-[var(--text-on-surface-variant)]">Committed Points</span>
            <p className="text-lg font-extrabold text-[var(--text-on-surface)] mt-0.5">{totalCommittedPoints} pts</p>
          </div>
          <div>
            <span className="text-[10px] font-bold uppercase text-emerald-600 dark:text-emerald-400">Completed Points</span>
            <p className="text-lg font-extrabold text-emerald-600 dark:text-emerald-400 mt-0.5">{completedPoints} pts</p>
          </div>
          <div>
            <span className="text-[10px] font-bold uppercase text-amber-600 dark:text-amber-400">Remaining Points</span>
            <p className="text-lg font-extrabold text-amber-600 dark:text-amber-400 mt-0.5">{remainingPoints} pts</p>
          </div>
          <div>
            <span className="text-[10px] font-bold uppercase text-[var(--color-primary)]">Progress</span>
            <p className="text-lg font-extrabold text-[var(--color-primary)] mt-0.5">{progressPercent}%</p>
          </div>
        </div>
      </div>

      {/* DND Context wrapping columns */}
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragStart={handleDragStart}
        onDragOver={handleDragOver}
        onDragEnd={handleDragEnd}
      >
        <SortableContext items={columnsState.map((c) => c.id)} strategy={horizontalListSortingStrategy}>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 items-start">
            {columnsState.map((col) => (
              <SortableColumn
                key={col.id}
                column={col}
                cards={cardsByColumn[col.id] || []}
                onCardClick={handleCardClick}
                onAddTask={() => dispatch(setCreateTaskModalOpen(true))}
              />
            ))}
          </div>
        </SortableContext>

        {/* Drag Overlay for smooth visual feedback */}
        <DragOverlay dropAnimation={dropAnimation}>
          {activeType === 'column' && activeColumn && (
            <div className="opacity-95 rotate-1 scale-102 shadow-2xl">
              <SortableColumnOverlay
                column={activeColumn}
                cards={cardsByColumn[activeColumn.id] || []}
              />
            </div>
          )}
          {activeType === 'card' && activeCard && (
            <div className="opacity-95 rotate-2 scale-105 shadow-2xl">
              <CardItem task={activeCard} isOverlay />
            </div>
          )}
        </DragOverlay>
      </DndContext>
    </div>
  );
};

// --- SORTABLE COLUMN COMPONENT ---
interface SortableColumnProps {
  column: Column;
  cards: Task[];
  onCardClick: (task: Task) => void;
  onAddTask: () => void;
}

const SortableColumn: React.FC<SortableColumnProps> = ({ column, cards, onCardClick, onAddTask }) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: column.id,
    data: {
      type: 'column',
      column,
    },
  });

  const style: React.CSSProperties = {
    transform: CSS.Translate.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`p-3 rounded-2xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] min-h-[500px] flex flex-col justify-between transition-shadow ${
        isDragging ? 'opacity-30 border-dashed border-[var(--color-primary)]' : ''
      }`}
    >
      <div>
        {/* Column Header (Drag handle for column reordering) */}
        <div className="flex items-center justify-between pb-3 mb-3 border-b border-[var(--border-outline-variant)] px-1">
          <div className="flex items-center gap-2">
            {/* Header Drag Handle Icon */}
            <button
              {...attributes}
              {...listeners}
              className="p-1 rounded text-[var(--text-on-surface-variant)] hover:text-[var(--text-on-surface)] cursor-grab active:cursor-grabbing hover:bg-[var(--bg-surface-container-high)] transition-colors"
              title="Drag to reorder column"
            >
              <span className="material-symbols-outlined text-base">drag_indicator</span>
            </button>

            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: column.colorHex || '#3b82f6' }} />
            <h3 className="font-bold text-xs uppercase tracking-wider text-[var(--text-on-surface)]">
              {column.name}
            </h3>
            <span className="px-2 py-0.5 rounded-full bg-[var(--bg-surface-container-high)] text-[10px] font-extrabold text-[var(--text-on-surface-variant)]">
              {cards.length}
            </span>
          </div>

          <button
            onClick={onAddTask}
            className="p-1 rounded hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] cursor-pointer"
          >
            <span className="material-symbols-outlined text-base">add</span>
          </button>
        </div>

        {/* Cards Sortable Container */}
        <SortableContext items={cards.map((c) => c.id)} strategy={verticalListSortingStrategy}>
          <div className="space-y-3 min-h-[120px]">
            {cards.map((card) => (
              <SortableCard key={card.id} task={card} onClick={() => onCardClick(card)} />
            ))}

            {cards.length === 0 && (
              <div className="p-8 border-2 border-dashed border-[var(--border-outline-variant)] rounded-xl text-center text-xs text-[var(--text-on-surface-variant)] bg-[var(--bg-surface-container-lowest)]/50">
                Drop tasks here
              </div>
            )}
          </div>
        </SortableContext>
      </div>

      {/* Bottom Quick Add */}
      <button
        onClick={onAddTask}
        className="w-full mt-3 py-2 rounded-xl border border-dashed border-[var(--border-outline-variant)] hover:border-[var(--color-primary)] text-xs font-semibold text-[var(--text-on-surface-variant)] hover:text-[var(--color-primary)] transition-colors cursor-pointer flex items-center justify-center gap-1.5"
      >
        <span className="material-symbols-outlined text-sm">add</span>
        <span>Add Task</span>
      </button>
    </div>
  );
};

// --- COLUMN OVERLAY COMPONENT ---
const SortableColumnOverlay: React.FC<{ column: Column; cards: Task[] }> = ({ column, cards }) => {
  return (
    <div className="p-3 rounded-2xl bg-[var(--bg-surface-container-low)] border-2 border-[var(--color-primary)] shadow-2xl min-h-[400px] w-72 flex flex-col justify-between">
      <div>
        <div className="flex items-center gap-2 pb-3 mb-3 border-b border-[var(--border-outline-variant)] px-1">
          <span className="material-symbols-outlined text-base text-[var(--color-primary)]">drag_indicator</span>
          <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: column.colorHex || '#3b82f6' }} />
          <h3 className="font-bold text-xs uppercase tracking-wider text-[var(--text-on-surface)]">{column.name}</h3>
          <span className="px-2 py-0.5 rounded-full bg-[var(--bg-surface-container-high)] text-[10px] font-extrabold text-[var(--text-on-surface-variant)]">
            {cards.length}
          </span>
        </div>

        <div className="space-y-3">
          {cards.slice(0, 3).map((card) => (
            <CardItem key={card.id} task={card} />
          ))}
          {cards.length > 3 && (
            <div className="text-center text-[10px] text-[var(--text-on-surface-variant)] font-semibold">
              +{cards.length - 3} more cards
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// --- SORTABLE CARD COMPONENT ---
interface SortableCardProps {
  task: Task;
  onClick: () => void;
}

const SortableCard: React.FC<SortableCardProps> = ({ task, onClick }) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
    data: {
      type: 'card',
      card: task,
      columnId: task.column_id,
    },
  });

  const style: React.CSSProperties = {
    transform: CSS.Translate.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      onClick={onClick}
      className={`${isDragging ? 'opacity-20 border-dashed border-2 border-[var(--color-primary)]' : ''}`}
    >
      <CardItem task={task} />
    </div>
  );
};

// --- CARD ITEM UI COMPONENT ---
const CardItem: React.FC<{ task: Task; isOverlay?: boolean }> = ({ task, isOverlay }) => {
  const completedSubtasks = task.subtasks ? task.subtasks.filter((st) => st.completed).length : 0;

  return (
    <div
      className={`p-4 rounded-xl bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] hover:border-[var(--color-primary)] transition-all cursor-grab active:cursor-grabbing group space-y-3 ${
        isOverlay ? 'shadow-2xl border-[var(--color-primary)] bg-[var(--bg-surface-container-lowest)]' : 'shadow-2xs hover:shadow-md'
      }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          {task.display_id && (
            <span className="px-1.5 py-0.5 rounded text-[10px] font-mono font-bold tracking-wider bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface)] border border-[var(--border-outline-variant)]">
              {task.display_id}
            </span>
          )}
          <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-[var(--color-primary-fixed)] text-[var(--color-primary)]">
            {task.category}
          </span>
        </div>

        <div className="flex items-center gap-1.5">
          {task.storyPoints !== undefined && (
            <span className="px-1.5 py-0.5 rounded text-[10px] font-extrabold bg-[var(--color-primary)] text-white flex items-center gap-0.5">
              <span className="material-symbols-outlined text-[10px]">bolt</span>
              <span>{task.storyPoints} pts</span>
            </span>
          )}

          <span
            className={`px-2 py-0.5 rounded text-[10px] font-bold ${
              task.priority === 'Urgent' || task.priority === 'High'
                ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400'
                : 'bg-amber-500/10 text-amber-600 dark:text-amber-400'
            }`}
          >
            {task.priority}
          </span>
        </div>
      </div>

      <h4 className="text-xs font-bold text-[var(--text-on-surface)] group-hover:text-[var(--color-primary)] transition-colors line-clamp-2">
        {task.title}
      </h4>

      {task.subtasks && task.subtasks.length > 0 && (
        <div className="space-y-1">
          <div className="flex justify-between items-center text-[10px] text-[var(--text-on-surface-variant)] font-semibold">
            <span>Subtasks</span>
            <span>
              {completedSubtasks}/{task.subtasks.length}
            </span>
          </div>
          <div className="w-full h-1 bg-[var(--bg-surface-container-high)] rounded-full overflow-hidden">
            <div
              className="h-full bg-[var(--color-primary)] transition-all duration-300"
              style={{ width: `${Math.round((completedSubtasks / task.subtasks.length) * 100)}%` }}
            />
          </div>
        </div>
      )}

      <div className="flex items-center justify-between pt-2 border-t border-[var(--border-outline-variant)] text-[10px] text-[var(--text-on-surface-variant)]">
        <div className="flex items-center gap-1">
          <span className="material-symbols-outlined text-xs">calendar_today</span>
          <span>{task.dueDate}</span>
        </div>

        <div className="flex -space-x-1.5">
          {(task.assignees || []).map((a, i) => (
            <div
              key={i}
              title={a.name}
              className="w-5 h-5 rounded-full border border-[var(--bg-surface-container-lowest)] bg-[var(--color-primary)] text-white text-[8px] font-bold flex items-center justify-center overflow-hidden"
            >
              {a.avatar ? (
                <img src={a.avatar} alt={a.name} className="w-full h-full object-cover" />
              ) : (
                a.initials || a.name?.slice(0, 2).toUpperCase()
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
