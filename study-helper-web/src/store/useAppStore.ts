import { create } from 'zustand';

type Mode = 'child' | 'adult';

interface AppState {
  mode: Mode;
  toggleMode: () => void;
  setMode: (mode: Mode) => void;
}

export const useAppStore = create<AppState>((set) => ({
  mode: 'adult',
  toggleMode: () =>
    set((state) => ({ mode: state.mode === 'adult' ? 'child' : 'adult' })),
  setMode: (mode) => set({ mode }),
}));
