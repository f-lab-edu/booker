'use client';

import { useEffect } from 'react';
import { useGoogleOneTapLogin } from '@react-oauth/google';
import { useAuth } from '@/lib/auth/AuthContext';

/**
 * Google One Tap 자동 로그인 컴포넌트
 * 페이지 로드 시 자동으로 Google 로그인 팝업이 나타납니다
 */
export function GoogleOneTap() {
  const { loginWithGoogle, isAuthenticated } = useAuth();

  useGoogleOneTapLogin({
    onSuccess: async (credentialResponse) => {
      await loginWithGoogle(credentialResponse);
    },
    onError: () => {
      console.log('Login Failed');
    },
    // One Tap이 자동으로 나타나지 않도록 비활성화 (명시적으로 트리거)
    disabled: isAuthenticated,
  });

  return null; // UI를 렌더링하지 않음 (One Tap은 자동으로 나타남)
}
