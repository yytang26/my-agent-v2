import {
  Calculator,
  MessageCircle,
  BookOpen,
  Scissors,
  Trophy,
  Heart,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAppStore } from '../../store/useAppStore';

interface FeatureCard {
  icon: React.ComponentType<{ size?: number; className?: string }>;
  title: string;
  description: string;
  route: string;
}

const features: FeatureCard[] = [
  {
    icon: Calculator,
    title: 'AI导师',
    description: '输入数学题，获取分步解题过程',
    route: '/tutor',
  },
  {
    icon: MessageCircle,
    title: '智能对话',
    description: '苏格拉底式引导学习',
    route: '/chat',
  },
  {
    icon: BookOpen,
    title: '知识探索',
    description: '古诗词学习 + 历史故事',
    route: '/explore',
  },
  {
    icon: Scissors,
    title: '内容精炼',
    description: '长文章变简洁摘要',
    route: '/digest',
  },
  {
    icon: Trophy,
    title: '里程碑',
    description: '目标设定与进度追踪',
    route: '/milestone',
  },
  {
    icon: Heart,
    title: '学习关怀',
    description: '每日鼓励与心情记录',
    route: '/care',
  },
];

function Home() {
  const navigate = useNavigate();
  const mode = useAppStore((s) => s.mode);
  const isChild = mode === 'child';

  return (
    <div className="p-6">
      <h1
        className="font-bold mb-6 text-[var(--color-text)]"
        style={{ fontSize: 'var(--font-size-xl)' }}
      >
        欢迎使用学习助手
      </h1>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {features.map((feature) => (
          <button
            key={feature.route}
            onClick={() => navigate(feature.route)}
            className={`group flex flex-col items-center gap-3 p-6 text-left transition-all duration-300 cursor-pointer border border-transparent hover:shadow-lg
              ${isChild ? 'hover:scale-[1.03] active:scale-95' : 'hover:-translate-y-1'}`}
            style={{
              backgroundColor: 'var(--color-card-bg)',
              borderRadius: 'var(--border-radius)',
              borderColor: 'var(--color-primary)',
              borderWidth: '1px',
              borderStyle: 'solid',
              opacity: 1,
            }}
          >
            <div
              className={`flex items-center justify-center w-14 h-14 rounded-[var(--border-radius)] transition-all duration-300
                ${isChild ? 'group-hover:animate-[bounce_0.5s_ease-in-out]' : ''}`}
              style={{
                backgroundColor: 'var(--color-primary)',
              }}
            >
              <feature.icon
                size={28}
                className="text-white transition-transform duration-300 group-hover:scale-110"
              />
            </div>
            <h2
              className="font-semibold text-[var(--color-text)]"
              style={{ fontSize: 'var(--font-size-lg)' }}
            >
              {feature.title}
            </h2>
            <p
              className="text-[var(--color-text-secondary)] text-center"
              style={{ fontSize: 'var(--font-size-base)' }}
            >
              {feature.description}
            </p>
          </button>
        ))}
      </div>
    </div>
  );
}

export default Home;
