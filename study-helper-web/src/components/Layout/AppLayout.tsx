import { useEffect, useState } from 'react';
import { Outlet, NavLink, useLocation } from 'react-router-dom';
import {
  Home,
  Calculator,
  MessageCircle,
  BookOpen,
  Scissors,
  Trophy,
  Heart,
  Menu,
  X,
} from 'lucide-react';
import { useAppStore } from '../../store/useAppStore';
import ModeSwitch from '../ModeSwitch';

interface NavItem {
  icon: React.ComponentType<{ size?: number; className?: string }>;
  title: string;
  route: string;
}

const navItems: NavItem[] = [
  { icon: Home, title: '首页', route: '/' },
  { icon: Calculator, title: 'AI导师', route: '/tutor' },
  { icon: MessageCircle, title: '智能对话', route: '/chat' },
  { icon: BookOpen, title: '知识探索', route: '/explore' },
  { icon: Scissors, title: '内容精炼', route: '/digest' },
  { icon: Trophy, title: '里程碑', route: '/milestone' },
  { icon: Heart, title: '学习关怀', route: '/care' },
];

const routeTitleMap: Record<string, string> = {
  '/': '首页',
  '/tutor': 'AI导师',
  '/chat': '智能对话',
  '/explore': '知识探索',
  '/digest': '内容精炼',
  '/milestone': '里程碑',
  '/care': '学习关怀',
};

function AppLayout() {
  const mode = useAppStore((s) => s.mode);
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    document.documentElement.setAttribute('data-mode', mode);
  }, [mode]);

  const currentTitle = routeTitleMap[location.pathname] || '学习助手';

  return (
    <div
      className="flex h-screen"
      style={{ backgroundColor: 'var(--color-bg)' }}
    >
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/40 z-20 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed lg:static inset-y-0 left-0 z-30 w-64 flex flex-col transition-transform duration-300 lg:translate-x-0
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}
        style={{ backgroundColor: 'var(--color-card-bg)' }}
      >
        {/* Logo area */}
        <div
          className="flex items-center justify-between px-5 py-4 border-b"
          style={{ borderColor: 'var(--color-primary)' }}
        >
          <div className="flex items-center gap-2">
            <span
              className="text-xl font-bold"
              style={{ color: 'var(--color-primary)' }}
            >
              📚 学习助手
            </span>
          </div>
          <button
            className="lg:hidden cursor-pointer"
            onClick={() => setSidebarOpen(false)}
            style={{ color: 'var(--color-text)' }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => (
            <NavLink
              key={item.route}
              to={item.route}
              end={item.route === '/'}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-[var(--border-radius)] transition-all duration-200
                ${isActive ? 'text-white font-semibold' : 'hover:opacity-80'}`
              }
              style={({ isActive }) => ({
                backgroundColor: isActive ? 'var(--color-primary)' : 'transparent',
                color: isActive ? '#FFFFFF' : 'var(--color-text)',
              })}
            >
              <item.icon size={20} />
              <span style={{ fontSize: 'var(--font-size-base)' }}>
                {item.title}
              </span>
            </NavLink>
          ))}
        </nav>

        {/* Mode switch */}
        <div className="px-3 py-4 border-t" style={{ borderColor: 'var(--color-primary)' }}>
          <ModeSwitch />
        </div>
      </aside>

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <header
          className="flex items-center justify-between px-6 py-4 border-b"
          style={{
            backgroundColor: 'var(--color-card-bg)',
            borderColor: 'var(--color-primary)',
          }}
        >
          <div className="flex items-center gap-3">
            <button
              className="lg:hidden cursor-pointer"
              onClick={() => setSidebarOpen(true)}
              style={{ color: 'var(--color-text)' }}
            >
              <Menu size={22} />
            </button>
            <h1
              className="font-semibold"
              style={{
                color: 'var(--color-text)',
                fontSize: 'var(--font-size-lg)',
              }}
            >
              {currentTitle}
            </h1>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default AppLayout;
