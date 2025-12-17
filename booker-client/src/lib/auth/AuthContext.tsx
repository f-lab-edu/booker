'use client';

import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { CredentialResponse, GoogleOAuthProvider } from '@react-oauth/google';

interface User {
  id: string;
  name: string;
  email: string;
  picture?: string;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  loginWithGoogle: (credentialResponse: CredentialResponse) => Promise<void>;
  logout: () => void;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check for existing user data in localStorage
    const userData = localStorage.getItem('user_data');
    const googleToken = localStorage.getItem('google_token');

    if (userData && googleToken) {
      setUser(JSON.parse(userData));
    }

    setIsLoading(false);
  }, []);

  const loginWithGoogle = async (credentialResponse: CredentialResponse) => {
    if (!credentialResponse.credential) {
      console.error('No credential in response');
      return;
    }

    try {
      // Google ID Token을 백엔드로 보내서 검증
      const apiUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8084';
      const response = await fetch(`${apiUrl}/api/v1/auth/google/verify`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          idToken: credentialResponse.credential,
        }),
      });

      const data = await response.json();

      if (data.authenticated) {
        const userData: User = {
          id: data.userId,
          name: data.name,
          email: data.email,
          picture: data.picture,
        };

        // Store in localStorage
        localStorage.setItem('google_token', credentialResponse.credential);
        localStorage.setItem('user_data', JSON.stringify(userData));

        // Update state
        setUser(userData);

        console.log('Login successful:', userData);
      } else {
        console.error('Authentication failed');
      }
    } catch (error) {
      console.error('Failed to verify Google token:', error);
    }
  };

  const logout = () => {
    localStorage.removeItem('google_token');
    localStorage.removeItem('user_data');
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        loginWithGoogle,
        logout,
        isLoading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

// Wrapper component with GoogleOAuthProvider
export function GoogleAuthProvider({ children }: { children: ReactNode }) {
  const clientId = process.env.GOOGLE_CLIENT_ID || '';

  if (!clientId) {
    console.warn('GOOGLE_CLIENT_ID is not set');
  }

  return (
    <GoogleOAuthProvider clientId={clientId}>
      <AuthProvider>{children}</AuthProvider>
    </GoogleOAuthProvider>
  );
}
