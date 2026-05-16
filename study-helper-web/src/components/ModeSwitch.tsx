import { Baby, User } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';

function ModeSwitch() {
  const { mode, toggleMode } = useAppStore();

  return (
    <button
      onClick={toggleMode}
      className="flex items-center gap-2 px-3 py-2 rounded-[var(--border-radius)] transition-all duration-300 cursor-pointer hover:opacity-80"
      style={{
        backgroundColor:
          mode === 'adult'
            ? 'var(--color-primary-light)'
            : 'var(--color-primary-light)',
        color: '#FFFFFF',
      }}
    >
      {mode === 'adult' ? (
        <>
          <User size={18} />
          <span className="text-sm font-medium">成人模式</span>
        </>
      ) : (
        <>
          <Baby size={18} />
          <span className="text-sm font-medium">儿童模式</span>
        </>
      )}
    </button>
  );
}

export default ModeSwitch;
