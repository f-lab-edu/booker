'use client';

import Link from 'next/link';
import { mainNavigation } from '@/config/navigation';
import { NavigationMenu } from '@/components/navigation/NavigationMenu';
import { NotificationDropdown } from '@/components/layout/NotificationDropdown';
import { useAuth } from '@/lib/auth/AuthContext';
import { GoogleLogin } from '@react-oauth/google';

export function Header() {
  const { user, isAuthenticated, loginWithGoogle, logout } = useAuth();

  return (
    <header className="fixed top-0 left-0 right-0 z-50 flex items-center justify-between px-12 py-5 backdrop-blur-md bg-black/10">
      {/* Logo */}
      <Link href="/" className="flex items-center">
        <div className="text-xl font-semibold tracking-tight text-white">
          BOOKER
        </div>
      </Link>

      {/* Center: Notification + Navigation Menu */}
      <div className="flex items-center gap-8">
        {/* Notification Bell */}
        <NotificationDropdown />

        {/* Navigation Menu */}
        <NavigationMenu items={mainNavigation} />
      </div>

      {/* Right Side: Auth Button */}
      {isAuthenticated ? (
        <div className="flex items-center gap-4">
          {user?.picture && (
            <img
              src={user.picture}
              alt={user.name}
              className="w-8 h-8 rounded-full"
            />
          )}
          <span className="text-white/80 text-sm">{user?.name}</span>
          <button
            onClick={logout}
            className="px-8 py-2.5 rounded-full bg-white text-black font-medium text-sm transition-all duration-300 hover:bg-white/90"
          >
            Logout
          </button>
        </div>
      ) : (
        <div className="flex items-center">
          <GoogleLogin
            onSuccess={async (credentialResponse) => {
              await loginWithGoogle(credentialResponse);
            }}
            onError={() => {
              console.error('Login Failed');
            }}
            text="signin"
            shape="rectangular"
            theme="filled_blue"
            size="large"
          />
        </div>
      )}
    </header>
  );
}
