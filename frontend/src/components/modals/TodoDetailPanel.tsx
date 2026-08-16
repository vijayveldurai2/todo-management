import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { toggleTaskSubtask, addTaskSubtask, addTaskComment, updateTaskStatus, updateTaskDetails } from '../../store/tasksSlice';
import { TaskStatus, Task } from '../../types';
import { RoleBadge } from '../common/RoleBadge';
import { NotFoundPage } from '../views/NotFoundPage';
import { apiService } from '../../services/apiService';

export const TodoDetailPanel: React.FC = () => {
  const { workspaceSlug, projectSlug, boardSlug, todoSlug, todoDisplayId } = useParams<{
    workspaceSlug?: string;
    projectSlug?: string;
    boardSlug?: string;
    todoSlug?: string;
    todoDisplayId?: string;
  }>();

  const targetIdOrSlug = todoDisplayId || todoSlug;

  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { boards } = useAppSelector((state) => state.projects);
  const { user } = useAppSelector((state) => state.auth);

  const [task, setTask] = useState<Task | null>(null);
  const [isNotFound, setIsNotFound] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const [newSubtaskTitle, setNewSubtaskTitle] = useState('');
  const [newCommentText, setNewCommentText] = useState('');

  useEffect(() => {
    let isMounted = true;
    if (!targetIdOrSlug) return;

    setIsLoading(true);
    setIsNotFound(false);

    if (workspaceSlug && projectSlug) {
      // Resolve task by display ID or slug within the project
      apiService
        .getTaskByDisplayId(workspaceSlug, projectSlug, targetIdOrSlug)
        .then((t) => {
          if (isMounted) {
            setTask(t);
            setIsLoading(false);
          }
        })
        .catch(() => {
          // Fallback if boardSlug provided
          if (boardSlug) {
            return apiService.getTaskBySlug(workspaceSlug, projectSlug, boardSlug, targetIdOrSlug);
          }
          throw new Error('not_found');
        })
        .then((t) => {
          if (t && isMounted) {
            setTask(t);
            setIsLoading(false);
          }
        })
        .catch((err) => {
          if (isMounted) {
            console.warn('Todo not found:', err);
            setIsNotFound(true);
            setIsLoading(false);
          }
        });
    } else {
      // Fallback lookup for static routes (/tasks/todo/:todoSlug, /kanban/todo/:todoSlug, etc.)
      apiService
        .getTasks()
        .then((allTasks) => {
          const target = targetIdOrSlug.toLowerCase();
          const found = allTasks.find(
            (t) =>
              t.display_id?.toLowerCase() === target ||
              t.slug?.toLowerCase() === target ||
              t.id.toLowerCase() === target
          );
          if (found && isMounted) {
            setTask(found);
            setIsLoading(false);
          } else if (isMounted) {
            setIsNotFound(true);
            setIsLoading(false);
          }
        })
        .catch((err) => {
          if (isMounted) {
            console.warn('Static task lookup failed:', err);
            setIsNotFound(true);
            setIsLoading(false);
          }
        });
    }

    return () => {
      isMounted = false;
    };
  }, [workspaceSlug, projectSlug, boardSlug, targetIdOrSlug]);

  const handleClose = () => {
    if (workspaceSlug && projectSlug && boardSlug) {
      navigate(`/${workspaceSlug}/${projectSlug}/${boardSlug}`);
    } else if (workspaceSlug && projectSlug) {
      navigate(`/${workspaceSlug}/${projectSlug}`);
    } else {
      navigate(-1);
    }
  };

  if (isLoading) {
    return (
      <div className="fixed inset-0 z-50 flex justify-end bg-black/50 backdrop-blur-xs">
        <div className="w-full max-w-2xl bg-[var(--bg-surface-container-lowest)] h-full flex items-center justify-center text-xs text-[var(--text-on-surface-variant)] font-semibold">
          <div className="w-5 h-5 border-2 border-[var(--color-primary)] border-t-transparent rounded-full animate-spin mr-2"></div>
          <span>Loading task details...</span>
        </div>
      </div>
    );
  }

  if (isNotFound || !task) {
    return (
      <div className="fixed inset-0 z-50 flex justify-end bg-black/50 backdrop-blur-xs">
        <div className="w-full max-w-2xl bg-[var(--bg-surface)] h-full overflow-y-auto">
          <div className="p-4 flex justify-between items-center border-b border-[var(--border-outline-variant)]">
            <span className="font-bold text-xs">Task Not Found</span>
            <button
              onClick={handleClose}
              className="p-1 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] cursor-pointer"
            >
              <span className="material-symbols-outlined text-xl">close</span>
            </button>
          </div>
          <NotFoundPage type="todo" />
        </div>
      </div>
    );
  }

  const sprintBoards = boards.filter((b) => b.projectId === task.projectId && b.type === 'Sprint');
  const completedSubtasks = task.subtasks ? task.subtasks.filter((st) => st.completed).length : 0;

  const handleToggleSubtask = (subtaskId: string) => {
    const updatedSubtasks = task.subtasks.map((st) =>
      st.id === subtaskId ? { ...st, completed: !st.completed } : st
    );
    setTask({ ...task, subtasks: updatedSubtasks });
    dispatch(toggleTaskSubtask({ taskId: task.id, subtaskId }));
  };

  const handleAddSubtask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSubtaskTitle.trim()) return;
    const newSt = { id: `st-${Date.now()}`, title: newSubtaskTitle.trim(), completed: false };
    const updatedSubtasks = [...task.subtasks, newSt];
    setTask({ ...task, subtasks: updatedSubtasks });
    dispatch(addTaskSubtask({ taskId: task.id, title: newSubtaskTitle.trim() }));
    setNewSubtaskTitle('');
  };

  const handleAddComment = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCommentText.trim()) return;
    const authorUser = user || {
      id: 'u-1',
      name: 'Vijay Kumar',
      email: 'vijaykumar.veldurai2@gmail.com',
      avatar: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDTOPP6Xfg9jOtaHJcbQZ0JqaIREWx-kV-vgYnDzdGK2hUfaE4vxqoAs79u0vJAyZYqn3eeQPCe-KxO9PLgoqoRYEtD7W2XOafyVmXh5mTTFqcHJ9FHruxk-Yij8tOx3xN9j1q7q-Fnuk4wNYczkIDad5kWKozaZW0g3_1wMDS7BcrmP2Q1Uy8vzX8XFRerFP_xJuE6E3tOGhmzhA3tSjMcHqVhc6gzvzzhpA-fUf-rnK2py5sk4y05uFBpyADjHnksJ-vzES0S5L0m',
    };
    const newComment = {
      id: `c-${Date.now()}`,
      authorName: authorUser.name,
      authorAvatar: authorUser.avatar,
      content: newCommentText.trim(),
      createdAt: 'Just now',
    };
    setTask({ ...task, comments: [...task.comments, newComment] });
    dispatch(
      addTaskComment({
        taskId: task.id,
        content: newCommentText.trim(),
        user: authorUser,
      })
    );
    setNewCommentText('');
  };

  const handleStatusChange = (status: TaskStatus) => {
    setTask({ ...task, status });
    dispatch(updateTaskStatus({ taskId: task.id, status }));
  };

  return (
    <div
      onClick={handleClose}
      className="fixed inset-0 z-50 flex justify-end bg-black/50 backdrop-blur-xs transition-opacity animate-in fade-in duration-200"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-2xl bg-[var(--bg-surface-container-lowest)] border-l border-[var(--border-outline-variant)] shadow-2xl h-full flex flex-col justify-between overflow-hidden animate-in slide-in-from-right duration-300"
      >
        {/* Drawer Header */}
        <div className="p-4 md:p-6 border-b border-[var(--border-outline-variant)] flex items-center justify-between gap-4 bg-[var(--bg-surface-container-low)]">
          <div className="flex-1 overflow-hidden">
            <div className="flex items-center gap-2 mb-1 flex-wrap">
              {task.display_id && (
                <span className="px-2 py-0.5 rounded text-[11px] font-mono font-bold tracking-wider bg-[var(--bg-surface-container-highest)] text-[var(--text-on-surface)] border border-[var(--border-outline-variant)] shadow-2xs">
                  {task.display_id}
                </span>
              )}
              <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-[var(--color-primary-fixed)] text-[var(--color-primary)]">
                {task.category}
              </span>
            </div>
            <h2 className="text-xl font-bold text-[var(--text-on-surface)] truncate">{task.title}</h2>
          </div>
          <button
            onClick={handleClose}
            className="p-2 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] transition-colors cursor-pointer shrink-0"
          >
            <span className="material-symbols-outlined text-xl">close</span>
          </button>
        </div>

        {/* Drawer Content Body */}
        <div className="p-4 md:p-6 space-y-6 overflow-y-auto custom-scrollbar flex-1">
          {/* Metadata Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 p-4 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Status
              </p>
              <select
                value={task.status}
                onChange={(e) => handleStatusChange(e.target.value as TaskStatus)}
                className="bg-[var(--bg-surface-container-highest)] text-[var(--text-on-surface)] font-bold px-2 py-1 rounded-lg border-none outline-none cursor-pointer"
              >
                <option value="To Do">To Do</option>
                <option value="In Progress">In Progress</option>
                <option value="In Review">In Review</option>
                <option value="Done">Done</option>
                <option value="Completed">Completed</option>
              </select>
            </div>

            <div>
              <p className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Priority
              </p>
              <span
                className={`inline-block px-2 py-1 rounded-md font-bold text-[10px] ${
                  task.priority === 'Urgent' || task.priority === 'High'
                    ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400'
                    : 'bg-amber-500/10 text-amber-600 dark:text-amber-400'
                }`}
              >
                {task.priority}
              </span>
            </div>

            <div>
              <p className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Due Date
              </p>
              <p className="font-semibold text-[var(--text-on-surface)] flex items-center gap-1">
                <span className="material-symbols-outlined text-xs">calendar_today</span>
                <span>{task.dueDate}</span>
              </p>
            </div>

            <div>
              <p className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Story Points
              </p>
              <select
                value={task.storyPoints || ''}
                onChange={(e) => {
                  const pts = e.target.value ? Number(e.target.value) : undefined;
                  setTask({ ...task, storyPoints: pts });
                  dispatch(updateTaskDetails({ taskId: task.id, updates: { storyPoints: pts } }));
                }}
                className="bg-[var(--bg-surface-container-highest)] text-[var(--text-on-surface)] font-bold px-2 py-1 rounded-lg border-none outline-none cursor-pointer"
              >
                <option value="">None</option>
                <option value="1">1 pt</option>
                <option value="2">2 pts</option>
                <option value="3">3 pts</option>
                <option value="5">5 pts</option>
                <option value="8">8 pts</option>
                <option value="13">13 pts</option>
              </select>
            </div>

            <div className="col-span-2 sm:col-span-1">
              <p className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Assignees & Roles
              </p>
              <div className="flex flex-wrap gap-1.5">
                {(task.assignees || []).map((a, i) => (
                  <div
                    key={i}
                    title={a.name}
                    className="flex items-center gap-1.5 px-2 py-0.5 rounded-lg bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] shrink-0"
                  >
                    <div className="w-5 h-5 rounded-full overflow-hidden bg-[var(--color-primary)] text-white text-[8px] font-bold flex items-center justify-center shrink-0">
                      {a.avatar ? (
                        <img src={a.avatar} alt={a.name} className="w-full h-full object-cover" />
                      ) : (
                        a.initials || a.name?.slice(0, 2).toUpperCase()
                      )}
                    </div>
                    <span className="font-semibold text-[10px] text-[var(--text-on-surface)]">{a.name}</span>
                    <RoleBadge role={a.role || 'Dev'} size="xs" />
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Description */}
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-2 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-sm">description</span>
              <span>Description</span>
            </h3>
            <div className="p-4 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs leading-relaxed text-[var(--text-on-surface)] whitespace-pre-wrap">
              {task.description || 'No description provided.'}
            </div>
          </div>

          {/* Subtasks */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] flex items-center gap-1.5">
                <span className="material-symbols-outlined text-sm">checklist</span>
                <span>Subtasks & Checklist</span>
              </h3>
              {task.subtasks.length > 0 && (
                <span className="text-[10px] font-bold text-[var(--color-primary)]">
                  {completedSubtasks} / {task.subtasks.length} Completed
                </span>
              )}
            </div>

            {task.subtasks.length > 0 && (
              <div className="w-full h-1.5 bg-[var(--bg-surface-container-high)] rounded-full overflow-hidden mb-3">
                <div
                  className="h-full bg-[var(--color-primary)] transition-all duration-300"
                  style={{
                    width: `${Math.round((completedSubtasks / task.subtasks.length) * 100)}%`,
                  }}
                />
              </div>
            )}

            <div className="space-y-2 mb-3">
              {task.subtasks.map((st) => (
                <label
                  key={st.id}
                  className={`flex items-center gap-3 p-2.5 rounded-xl border border-[var(--border-outline-variant)] cursor-pointer transition-colors ${
                    st.completed
                      ? 'bg-[var(--bg-surface-container-low)] opacity-70 line-through'
                      : 'bg-[var(--bg-surface-container-lowest)] hover:bg-[var(--bg-surface-container-low)]'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={st.completed}
                    onChange={() => handleToggleSubtask(st.id)}
                    className="w-4 h-4 rounded text-[var(--color-primary)] accent-[var(--color-primary)] cursor-pointer"
                  />
                  <span className="text-xs font-medium text-[var(--text-on-surface)]">{st.title}</span>
                </label>
              ))}
            </div>

            <form onSubmit={handleAddSubtask} className="flex gap-2">
              <input
                type="text"
                placeholder="Add new subtask..."
                value={newSubtaskTitle}
                onChange={(e) => setNewSubtaskTitle(e.target.value)}
                className="flex-1 px-3 py-2 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs text-[var(--text-on-surface)] focus:outline-none focus:border-[var(--color-primary)]"
              />
              <button
                type="submit"
                className="px-3 py-2 rounded-xl bg-[var(--bg-surface-container-high)] hover:bg-[var(--color-primary)] hover:text-white text-[var(--text-on-surface)] font-semibold text-xs transition-colors cursor-pointer"
              >
                Add
              </button>
            </form>
          </div>

          {/* Activity / Comments */}
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-3 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-sm">chat</span>
              <span>Comments & Discussion</span>
            </h3>

            <div className="space-y-3 mb-4">
              {task.comments.length === 0 ? (
                <p className="text-xs text-[var(--text-on-surface-variant)] italic">No comments yet.</p>
              ) : (
                task.comments.map((c) => (
                  <div key={c.id} className="p-3 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-[var(--text-on-surface)]">{c.authorName}</span>
                      <span className="text-[10px] text-[var(--text-on-surface-variant)]">{c.createdAt}</span>
                    </div>
                    <p className="text-[var(--text-on-surface-variant)] leading-relaxed">{c.content}</p>
                  </div>
                ))
              )}
            </div>

            <form onSubmit={handleAddComment} className="flex gap-2">
              <input
                type="text"
                placeholder="Write a comment..."
                value={newCommentText}
                onChange={(e) => setNewCommentText(e.target.value)}
                className="flex-1 px-3 py-2 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs text-[var(--text-on-surface)] focus:outline-none focus:border-[var(--color-primary)]"
              />
              <button
                type="submit"
                className="px-4 py-2 rounded-xl bg-[var(--color-primary)] text-white font-semibold text-xs cursor-pointer hover:opacity-90 transition-all"
              >
                Post
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};
