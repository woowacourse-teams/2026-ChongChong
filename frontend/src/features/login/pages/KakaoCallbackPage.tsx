import type { CSSObject } from '@emotion/react';
import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import Page from '../../../shared/ui/Page';
import { tokens, typography } from '../../../styles/global';
import { loginWithKakaoCode } from '../api';
import { consumeKakaoCallback } from '../kakaoOAuth';

const contentStyle = {
  display: 'flex',
  minHeight: '100dvh',
  padding: tokens.spacing[5],
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  gap: tokens.spacing[4],
  color: tokens.text.secondary,
  textAlign: 'center',
} satisfies CSSObject;

const retryLinkStyle = {
  ...typography.button,
  color: tokens.text.brand,
} satisfies CSSObject;

export default function KakaoCallbackPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const startedRef = useRef(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    const completeLogin = async () => {
      try {
        const authorizationCode = consumeKakaoCallback(location.search);
        await loginWithKakaoCode(authorizationCode);
        navigate('/studies', { replace: true });
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : '카카오 로그인에 실패했습니다.');
      }
    };

    completeLogin();
  }, [location.search, navigate]);

  return (
    <Page>
      <main css={contentStyle} aria-live="polite">
        {errorMessage ? (
          <>
            <p>{errorMessage}</p>
            <Link css={retryLinkStyle} to="/login">
              로그인 화면으로 돌아가기
            </Link>
          </>
        ) : (
          <p>카카오 로그인을 완료하고 있어요.</p>
        )}
      </main>
    </Page>
  );
}
