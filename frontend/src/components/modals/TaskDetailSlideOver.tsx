import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../../store';
import { setSelectedTask } from '../../store/uiSlice';
import { toggleTaskSubtask, addTaskSubtask, addTaskComment, updateTaskStatus, updateTaskDetails } from '../../store/tasksSlice';
import { TaskStatus } from '../../types';
import { RoleBadge } from '../common/RoleBadge';

export const TaskDetailSlideOver: React.FC = () => {
  const dispatch = useAppDispatch();
  const { selectedTaskId } = useAppSelector((state) => state.ui);
  const { tasks } = useAppSelector((state) => state.tasks);
  const { boards } = useAppSelector((state) => state.projects);
  const { user } = useAppSelector((state) => state.auth);

  const [newSubtaskTitle, setNewSubtaskTitle] = useState('');
  const [newCommentText, setNewCommentText] = useState('');

  const task = tasks.find((t) => t.id === selectedTaskId);
  const sprintBoards = boards.filter((b) => b.projectId === task?.projectId && b.type === 'Sprint');

  if (!selectedTaskId || !task) return null;

  const completedSubtasks = task.subtasks.filter((st) => st.completed).length;

  const handleToggleSubtask = (subtaskId: string) => {
    dispatch(toggleTaskSubtask({ taskId: task.id, subtaskId }));
  };

  const handleAddSubtask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSubtaskTitle.trim()) return;
    dispatch(addTaskSubtask({ taskId: task.id, title: newSubtaskTitle.trim() }));
    setNewSubtaskTitle('');
  };

  const handleAddComment = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCommentText.trim()) return;
    dispatch(
      addTaskComment({
        taskId: task.id,
        content: newCommentText.trim(),
        user: user || {
          id: 'u-guest',
          name: 'Guest User',
          email: 'guest@example.com',
          avatar: '',
        },
      })
    );
    setNewCommentText('');
  };

  const handleStatusChange = (status: TaskStatus) => {
    dispatch(updateTaskStatus({ taskId: task.id, status }));
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/50 backdrop-blur-xs transition-opacity animate-in fade-in duration-200">
      <div className="w-full max-w-2xl bg-[var(--bg-surface-container-lowest)] border-l border-[var(--border-outline-variant)] shadow-2xl h-full flex flex-col justify-between overflow-hidden animate-in slide-in-from-right duration-300">
        {/* Drawer Header */}
        <div className="p-4 md:p-6 border-b border-[var(--border-outline-variant)] flex items-center justify-between gap-4 bg-[var(--bg-surface-container-low)]">
          <div>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-[var(--color-primary-fixed)] text-[var(--color-primary)]">
              {task.category}
            </span>
            <h2 className="text-xl font-bold text-[var(--text-on-surface)] mt-1">{task.title}</h2>
          </div>
          <button
            onClick={() => dispatch(setSelectedTask(null))}
            className="p-2 rounded-full hover:bg-[var(--bg-surface-container-high)] text-[var(--text-on-surface-variant)] transition-colors cursor-pointer"
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
                onChange={(e) =>
                  dispatch(
                    updateTaskDetails({
                      taskId: task.id,
                      updates: { storyPoints: e.target.value ? Number(e.target.value) : undefined },
                    })
                  )
                }
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

            <div>
              <p className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Assigned Sprint
              </p>
              <select
                value={task.sprintId || ''}
                onChange={(e) =>
                  dispatch(
                    updateTaskDetails({
                      taskId: task.id,
                      updates: { sprintId: e.target.value || undefined },
                    })
                  )
                }
                className="bg-[var(--bg-surface-container-highest)] text-[var(--text-on-surface)] font-bold px-2 py-1 rounded-lg border-none outline-none cursor-pointer text-[11px]"
              >
                <option value="">No Sprint (Backlog)</option>
                {sprintBoards.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.sprintName || s.title}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-span-2 sm:col-span-1">
              <p className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-1">
                Assignees & Roles
              </p>
              <div className="flex flex-wrap gap-1.5">
                {task.assignees.map((a, i) => (
                  <div
                    key={i}
                    title={a.name}
                    className="flex items-center gap-1.5 px-2 py-0.5 rounded-lg bg-[var(--bg-surface-container-lowest)] border border-[var(--border-outline-variant)] shrink-0"
                  >
                    <div className="w-5 h-5 rounded-full overflow-hidden bg-[var(--color-primary)] text-white text-[8px] font-bold flex items-center justify-center shrink-0">
                      {a.avatar ? (
                        <img src={a.avatar} alt={a.name} className="w-full h-full object-cover" />
                      ) : (
                        a.initials || a.name.slice(0, 2).toUpperCase()
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

          {/* Subtask Checklist */}
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

            {/* Progress Bar */}
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
                    className="w-4 h-4 rounded text-[var(--color-primary)] focus:ring-0 cursor-pointer"
                  />
                  <span className="text-xs font-medium text-[var(--text-on-surface)]">{st.title}</span>
                </label>
              ))}
            </div>

            {/* Add Subtask form */}
            <form onSubmit={handleAddSubtask} className="flex gap-2">
              <input
                type="text"
                value={newSubtaskTitle}
                onChange={(e) => setNewSubtaskTitle(e.target.value)}
                placeholder="Add subtask item..."
                className="flex-1 px-3 py-2 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)]"
              />
              <button
                type="submit"
                className="px-3 py-2 rounded-xl bg-[var(--color-primary)] text-white text-xs font-semibold cursor-pointer hover:opacity-90"
              >
                Add
              </button>
            </form>
          </div>

          {/* Attachments */}
          {task.attachments.length > 0 && (
            <div>
              <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-2 flex items-center gap-1.5">
                <span className="material-symbols-outlined text-sm">attach_file</span>
                <span>Attachments ({task.attachments.length})</span>
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {task.attachments.map((att) => (
                  <div
                    key={att.id}
                    className="p-3 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] flex items-center gap-3"
                  >
                    {att.url.endsWith('.png') || att.url.endsWith('.jpg') ? (
                      <img src={att.url} alt={att.fileName} className="w-10 h-10 rounded-lg object-cover border" />
                    ) : (
                      <div className="w-10 h-10 rounded-lg bg-[var(--color-primary-fixed)] text-[var(--color-primary)] flex items-center justify-center font-bold text-xs">
                        PDF
                      </div>
                    )}
                    <div className="overflow-hidden flex-1">
                      <p className="font-semibold text-xs truncate text-[var(--text-on-surface)]">{att.fileName}</p>
                      <p className="text-[10px] text-[var(--text-on-surface-variant)]">{att.fileSize}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Activity / Comments */}
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--text-on-surface-variant)] mb-3 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-sm">forum</span>
              <span>Activity & Discussion ({task.comments.length})</span>
            </h3>

            <div className="space-y-3 mb-4">
              {task.comments.map((c) => (
                <div key={c.id} className="p-3 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs space-y-1">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className="w-5 h-5 rounded-full overflow-hidden bg-[var(--color-primary)] text-white text-[9px] font-bold flex items-center justify-center">
                        {c.authorAvatar ? (
                          <img src={c.authorAvatar} alt={c.authorName} className="w-full h-full object-cover" />
                        ) : (
                          c.authorName.slice(0, 2).toUpperCase()
                        )}
                      </div>
                      <span className="font-bold text-[var(--text-on-surface)]">{c.authorName}</span>
                    </div>
                    <span className="text-[10px] text-[var(--text-on-surface-variant)]">{c.createdAt}</span>
                  </div>
                  <p className="text-[var(--text-on-surface-variant)] pl-7">{c.content}</p>
                </div>
              ))}
            </div>

            {/* Comment Box */}
            <form onSubmit={handleAddComment} className="flex gap-2">
              <textarea
                rows={2}
                value={newCommentText}
                onChange={(e) => setNewCommentText(e.target.value)}
                placeholder="Write a comment..."
                className="flex-1 px-3 py-2 rounded-xl bg-[var(--bg-surface-container-low)] border border-[var(--border-outline-variant)] text-xs text-[var(--text-on-surface)] outline-none focus:border-[var(--color-primary)] custom-scrollbar"
              />
              <button
                type="submit"
                className="px-4 py-2 rounded-xl bg-[var(--color-primary)] text-white text-xs font-semibold self-end cursor-pointer hover:opacity-90"
              >
                Comment
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};
