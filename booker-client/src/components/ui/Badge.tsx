import { ReactNode } from 'react';

interface BadgeProps {
  children: ReactNode;
  icon?: ReactNode;
  variant?: 'default' | 'outline';
  className?: string;
}

export function Badge({
  children,
  icon,
  variant = 'default',
  className = ''
}: BadgeProps) {
  const baseStyles = 'inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-all';

  const variantStyles = {
    default: 'bg-white/10 backdrop-blur-md text-white/90 border border-white/20',
    outline: 'bg-transparent text-white/80 border border-white/30',
  };

  return (
    <div className={`${baseStyles} ${variantStyles[variant]} ${className}`}>
      {icon && <span className="text-white/70">{icon}</span>}
      <span>{children}</span>
    </div>
  );
}
