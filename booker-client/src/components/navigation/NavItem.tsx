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
        relative px-7 py-2.5 rounded-lg text-xl font-normal transition-all duration-200
        ${isActive
          ? 'text-white bg-white/10'
          : 'text-white/70 hover:text-green-400 hover:bg-white/5'
        }
        ${className}
      `}
    >
      {label}
    </Link>
  );
}
