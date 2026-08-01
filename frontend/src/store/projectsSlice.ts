import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { Project, Board } from '../types';
import { apiService } from '../services/apiService';

interface ProjectsState {
  projects: Project[];
  boards: Board[];
  isLoading: boolean;
  error: string | null;
}

const initialState: ProjectsState = {
  projects: [],
  boards: [],
  isLoading: false,
  error: null,
};

export const fetchProjects = createAsyncThunk(
  'projects/fetchProjects',
  async () => {
    return await apiService.getProjects();
  }
);

export const createNewProject = createAsyncThunk(
  'projects/createNewProject',
  async (projectData: Partial<Project>) => {
    return await apiService.createProject(projectData);
  }
);

export const fetchBoards = createAsyncThunk(
  'projects/fetchBoards',
  async (projectId?: string) => {
    return await apiService.getBoards(projectId);
  }
);

export const createNewBoard = createAsyncThunk(
  'projects/createNewBoard',
  async (boardData: Partial<Board>) => {
    return await apiService.createBoard(boardData);
  }
);

const projectsSlice = createSlice({
  name: 'projects',
  initialState,
  reducers: {
    updateProjectLocally: (state, action: PayloadAction<{ id: string; name: string }>) => {
      const proj = state.projects.find((p) => p.id === action.payload.id);
      if (proj) {
        proj.name = action.payload.name;
      }
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch Projects
      .addCase(fetchProjects.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(fetchProjects.fulfilled, (state, action) => {
        state.isLoading = false;
        state.projects = action.payload;
      })
      .addCase(fetchProjects.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.error.message || 'Failed to fetch projects';
      })
      // Create Project
      .addCase(createNewProject.fulfilled, (state, action) => {
        state.projects.unshift(action.payload);
      })
      // Fetch Boards
      .addCase(fetchBoards.fulfilled, (state, action) => {
        state.boards = action.payload;
      })
      // Create Board
      .addCase(createNewBoard.fulfilled, (state, action) => {
        state.boards.push(action.payload);
      });
  },
});

export const { updateProjectLocally } = projectsSlice.actions;
export default projectsSlice.reducer;
