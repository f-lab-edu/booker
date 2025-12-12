'use client';

import { NavigationItem } from '@/config/navigation';
import { NavItem } from './NavItem';

interface NavigationMenuProps {
  items: NavigationItem[];
  className?: string;
}

export function NavigationMenu({ items, className = '' }: NavigationMenuProps) {
  return (
    <nav className={`flex items-center gap-8 ${className}`}>
      {items.map((item) => (
        <NavItem
          key={item.href}
          label={item.label}
          href={item.href}
        />
      ))}
    </nav>
  );
}
