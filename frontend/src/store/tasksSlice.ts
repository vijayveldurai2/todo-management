import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { Task, User, TaskStatus } from '../types';
import { apiService } from '../services/apiService';

interface TasksState {
  tasks: Task[];
  isLoading: boolean;
  error: string | null;
}

const initialState: TasksState = {
  tasks: [],
  isLoading: false,
  error: null,
};

export const fetchTasks = createAsyncThunk(
  'tasks/fetchTasks',
  async (params?: { projectId?: string; boardId?: string }) => {
    return await apiService.getTasks(params?.projectId, params?.boardId);
  }
);

export const createNewTask = createAsyncThunk(
  'tasks/createNewTask',
  async (taskData: Partial<Task>) => {
    return await apiService.createTask(taskData);
  }
);

export const updateTaskStatus = createAsyncThunk(
  'tasks/updateTaskStatus',
  async ({ taskId, status }: { taskId: string; status: TaskStatus }) => {
    return await apiService.updateTask(taskId, { status });
  }
);

export const updateTaskDetails = createAsyncThunk(
  'tasks/updateTaskDetails',
  async ({ taskId, updates }: { taskId: string; updates: Partial<Task> }) => {
    return await apiService.updateTask(taskId, updates);
  }
);

export const toggleTaskSubtask = createAsyncThunk(
  'tasks/toggleTaskSubtask',
  async ({ taskId, subtaskId }: { taskId: string; subtaskId: string }) => {
    return await apiService.toggleSubtask(taskId, subtaskId);
  }
);

export const addTaskSubtask = createAsyncThunk(
  'tasks/addTaskSubtask',
  async ({ taskId, title }: { taskId: string; title: string }) => {
    return await apiService.addSubtask(taskId, title);
  }
);

export const addTaskComment = createAsyncThunk(
  'tasks/addTaskComment',
  async ({ taskId, content, user }: { taskId: string; content: string; user: User }) => {
    return await apiService.addComment(taskId, content, user);
  }
);

const tasksSlice = createSlice({
  name: 'tasks',
  initialState,
  reducers: {
    // Optimistic local update helpers
    localToggleSubtask: (state, action: PayloadAction<{ taskId: string; subtaskId: string }>) => {
      const task = state.tasks.find((t) => t.id === action.payload.taskId);
      if (task) {
        const subtask = task.subtasks.find((st) => st.id === action.payload.subtaskId);
        if (subtask) {
          subtask.completed = !subtask.completed;
        }
      }
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch
      .addCase(fetchTasks.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(fetchTasks.fulfilled, (state, action) => {
        state.isLoading = false;
        state.tasks = action.payload;
      })
      .addCase(fetchTasks.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.error.message || 'Failed to fetch tasks';
      })
      // Create
      .addCase(createNewTask.fulfilled, (state, action) => {
        state.tasks.unshift(action.payload);
      })
      // Update status
      .addCase(updateTaskStatus.fulfilled, (state, action) => {
        const index = state.tasks.findIndex((t) => t.id === action.payload.id);
        if (index !== -1) {
          state.tasks[index] = action.payload;
        }
      })
      // Update details
      .addCase(updateTaskDetails.fulfilled, (state, action) => {
        const index = state.tasks.findIndex((t) => t.id === action.payload.id);
        if (index !== -1) {
          state.tasks[index] = action.payload;
        }
      })
      // Toggle Subtask
      .addCase(toggleTaskSubtask.fulfilled, (state, action) => {
        const index = state.tasks.findIndex((t) => t.id === action.payload.id);
        if (index !== -1) {
          state.tasks[index] = action.payload;
        }
      })
      // Add Subtask
      .addCase(addTaskSubtask.fulfilled, (state, action) => {
        const index = state.tasks.findIndex((t) => t.id === action.payload.id);
        if (index !== -1) {
          state.tasks[index] = action.payload;
        }
      })
      // Add Comment
      .addCase(addTaskComment.fulfilled, (state, action) => {
        const index = state.tasks.findIndex((t) => t.id === action.payload.id);
        if (index !== -1) {
          state.tasks[index] = action.payload;
        }
      });
  },
});

export const { localToggleSubtask } = tasksSlice.actions;
export default tasksSlice.reducer;
