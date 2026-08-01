import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { User, SignupRequest, LoginRequest } from '../types';
import { apiService } from '../services/apiService';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  signupMessage: string | null;
  verifyMessage: string | null;
  showOAuthModal: boolean;
}

const initialState: AuthState = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  signupMessage: null,
  verifyMessage: null,
  showOAuthModal: false,
};

export const fetchCurrentUser = createAsyncThunk(
  'auth/fetchCurrentUser',
  async () => {
    return await apiService.getCurrentUser();
  }
);

export const signupUser = createAsyncThunk(
  'auth/signupUser',
  async (data: SignupRequest) => {
    return await apiService.signup(data);
  }
);

export const loginUserApi = createAsyncThunk(
  'auth/loginUserApi',
  async (data: LoginRequest) => {
    const res = await apiService.loginApi(data);
    const user: User = {
      id: res.userId,
      name: res.username,
      username: res.username,
      email: res.email,
      avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80',
      role: res.role || 'USER',
      accessToken: res.accessToken,
      provider: 'email',
    };
    return user;
  }
);

export const verifyEmailToken = createAsyncThunk(
  'auth/verifyEmailToken',
  async (token: string) => {
    return await apiService.verifyEmail(token);
  }
);

export const loginWithOAuth = createAsyncThunk(
  'auth/loginWithOAuth',
  async (provider: 'google' | 'github') => {
    return await apiService.loginOAuth(provider);
  }
);

export const loginWithEmail = createAsyncThunk(
  'auth/loginWithEmail',
  async (email: string) => {
    return await apiService.loginEmail(email);
  }
);

export const logoutUser = createAsyncThunk(
  'auth/logoutUser',
  async () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    await apiService.logout();
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setOAuthModalOpen: (state, action: PayloadAction<boolean>) => {
      state.showOAuthModal = action.payload;
    },
    clearAuthMessages: (state) => {
      state.error = null;
      state.signupMessage = null;
      state.verifyMessage = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch User
      .addCase(fetchCurrentUser.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(fetchCurrentUser.fulfilled, (state, action) => {
        state.isLoading = false;
        state.user = action.payload;
        state.isAuthenticated = !!action.payload;
      })
      .addCase(fetchCurrentUser.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.error.message || 'Failed to fetch user';
      })
      // Signup User
      .addCase(signupUser.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.signupMessage = null;
      })
      .addCase(signupUser.fulfilled, (state, action) => {
        state.isLoading = false;
        state.signupMessage = action.payload.message;
        state.error = null;
      })
      .addCase(signupUser.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.error.message || 'Signup failed';
      })
      // Login User API
      .addCase(loginUserApi.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(loginUserApi.fulfilled, (state, action) => {
        state.isLoading = false;
        state.user = action.payload;
        state.isAuthenticated = true;
        state.error = null;
      })
      .addCase(loginUserApi.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.error.message || 'Login failed';
      })
      // Verify Email Token
      .addCase(verifyEmailToken.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.verifyMessage = null;
      })
      .addCase(verifyEmailToken.fulfilled, (state, action) => {
        state.isLoading = false;
        state.verifyMessage = action.payload.message;
        state.error = null;
      })
      .addCase(verifyEmailToken.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.error.message || 'Verification failed';
      })
      // OAuth Login
      .addCase(loginWithOAuth.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(loginWithOAuth.fulfilled, (state, action) => {
        state.isLoading = false;
        state.user = action.payload;
        state.isAuthenticated = true;
        state.showOAuthModal = false;
      })
      .addCase(loginWithOAuth.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.error.message || 'OAuth Login failed';
      })
      // Email Login
      .addCase(loginWithEmail.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(loginWithEmail.fulfilled, (state, action) => {
        state.isLoading = false;
        state.user = action.payload;
        state.isAuthenticated = true;
        state.error = null;
      })
      .addCase(loginWithEmail.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.error.message || 'Login failed';
      })
      // Logout
      .addCase(logoutUser.fulfilled, (state) => {
        state.user = null;
        state.isAuthenticated = false;
      });
  },
});

export const { setOAuthModalOpen, clearAuthMessages } = authSlice.actions;
export default authSlice.reducer;
