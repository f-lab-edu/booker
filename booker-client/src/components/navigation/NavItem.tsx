'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

interface NavItemProps {
  label: string;
  href: string;
  className?: string;
}

export function NavItem({ label, href, className = '' }: NavItemProps) {
  const pathname = usePathname();
  const isActive = pathname === href;

  return (
    <Link
      href={href}
      className={`
        relative px-5 py-2.5 rounded-lg text-base font-medium transition-all duration-200
        ${isActive
          ? 'bg-white/10 text-white'
          : 'text-white/80 hover:text-white hover:bg-white/5'
        }
        ${className}
      `}
    >
      {label}
    </Link>
  );
}
