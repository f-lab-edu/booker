'use client';

import Link from 'next/link';
import { mainNavigation } from '@/config/navigation';
import { NavigationMenu } from '@/components/navigation/NavigationMenu';
import { NotificationDropdown } from '@/components/layout/NotificationDropdown';
import { useAuth } from '@/lib/auth/AuthContext';
import { useState } from 'react';

export function Header() {
  const { user, isAuthenticated, login, logout } = useAuth();
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoggingIn(true);
    try {
      await login(email, password);
      setShowLoginModal(false);
      setEmail('');
      setPassword('');
    } catch (error) {
      console.error('Login failed:', error);
    } finally {
      setIsLoggingIn(false);
    }
  };

  return (
    <>
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
            <span className="text-white/80 text-sm">{user?.name}</span>
            <button
              onClick={logout}
              className="px-8 py-2.5 rounded-full bg-white text-black font-medium text-sm transition-all duration-300 hover:bg-white/90"
            >
              Logout
            </button>
          </div>
        ) : (
          <button
            onClick={() => setShowLoginModal(true)}
            className="px-8 py-2.5 rounded-full bg-white text-black font-medium text-sm transition-all duration-300 hover:bg-white/90"
          >
            Login
          </button>
        )}
      </header>

      {/* Login Modal */}
      {showLoginModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div className="bg-gray-900 rounded-2xl p-8 w-full max-w-md border border-white/10">
            <h2 className="text-2xl font-medium text-white mb-6">Login</h2>
            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <label htmlFor="email" className="block text-sm text-white/80 mb-2">
                  Email
                </label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-4 py-3 rounded-lg bg-white/5 border border-white/10 text-white placeholder:text-white/30 focus:outline-none focus:ring-2 focus:ring-violet-500"
                  placeholder="your@email.com"
                  required
                />
              </div>
              <div>
                <label htmlFor="password" className="block text-sm text-white/80 mb-2">
                  Password
                </label>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full px-4 py-3 rounded-lg bg-white/5 border border-white/10 text-white placeholder:text-white/30 focus:outline-none focus:ring-2 focus:ring-violet-500"
                  placeholder="••••••••"
                  required
                />
              </div>
              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => {
                    setShowLoginModal(false);
                    setEmail('');
                    setPassword('');
                  }}
                  className="flex-1 px-6 py-3 rounded-lg bg-white/5 text-white font-medium transition-all hover:bg-white/10"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isLoggingIn}
                  className="flex-1 px-6 py-3 rounded-lg bg-white text-black font-medium transition-all hover:bg-white/90 disabled:opacity-50"
                >
                  {isLoggingIn ? 'Logging in...' : 'Login'}
                </button>
              </div>
            </form>
            <p className="mt-4 text-xs text-white/50 text-center">
              Note: Mock authentication for development
            </p>
          </div>
        </div>
      )}
    </>
  );
}
