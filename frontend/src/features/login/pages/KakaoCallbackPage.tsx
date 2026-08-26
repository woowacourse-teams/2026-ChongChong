import { keyframes, type CSSObject } from '@emotion/react';
import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import chongchongCharacter from '../../../shared/assets/icons/header-icon.svg';
import Page from '../../../shared/ui/Page';
import { tokens, typography } from '../../../styles/global';
import { loginWithKakaoCode } from '../api';
import { consumeKakaoCallback } from '../kakaoOAuth';

const contentStyle = {
  display: 'flex',
  position: 'relative',
  width: '100%',
  maxWidth: '390px',
  minHeight: '100dvh',
  margin: '0 auto',
  padding: `64px ${tokens.spacing[5]} calc(34px + env(safe-area-inset-bottom, 0px))`,
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  overflow: 'hidden',
  isolation: 'isolate',
  background: tokens.bg.default,
} satisfies CSSObject;

const statusContentStyle = {
  display: 'flex',
  position: 'relative',
  zIndex: 1,
  width: '100%',
  maxWidth: '350px',
  padding: `0 ${tokens.spacing[4]}`,
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  textAlign: 'center',
} satisfies CSSObject;

const float = keyframes`
  0%, 100% { transform: translateY(0) rotate(-2deg); }
  50% { transform: translateY(-10px) rotate(2deg); }
`;

const pulse = keyframes`
  0%, 80%, 100% { opacity: 0.28; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
`;

const characterBackdropStyle = {
  display: 'flex',
  position: 'relative',
  width: '144px',
  height: '144px',
  borderRadius: tokens.radius.full,
  alignItems: 'center',
  justifyContent: 'center',
  background: tokens.bg.brandSubtle,
  boxShadow: 'inset 0 0 0 12px rgba(255, 255, 255, 0.55)',
} satisfies CSSObject;

const characterStyle = {
  width: '104px',
  height: '104px',
  objectFit: 'contain',
  animation: `${float} 1.8s ease-in-out infinite`,
  '@media (prefers-reduced-motion: reduce)': {
    animation: 'none',
  },
} satisfies CSSObject;

const errorBadgeStyle = {
  ...typography.bodyStrong,
  display: 'flex',
  position: 'absolute',
  right: '4px',
  bottom: '8px',
  width: '36px',
  height: '36px',
  border: '4px solid white',
  borderRadius: tokens.radius.full,
  alignItems: 'center',
  justifyContent: 'center',
  background: tokens.color.red500,
  color: tokens.text.onBrand,
} satisfies CSSObject;

const titleStyle = {
  ...typography.headline,
  margin: `${tokens.spacing[6]} 0 0`,
  color: tokens.text.primary,
} satisfies CSSObject;

const descriptionStyle = {
  ...typography.body,
  maxWidth: '270px',
  margin: `${tokens.spacing[2]} 0 0`,
  color: tokens.text.secondary,
  wordBreak: 'keep-all',
} satisfies CSSObject;

const loadingStatusStyle = {
  ...typography.caption,
  display: 'flex',
  marginTop: tokens.spacing[6],
  padding: `${tokens.spacing[2]} ${tokens.spacing[4]}`,
  borderRadius: tokens.radius.full,
  alignItems: 'center',
  gap: tokens.spacing[2],
  background: tokens.bg.subtle,
  color: tokens.text.muted,
} satisfies CSSObject;

const loadingDotsStyle = {
  display: 'flex',
  gap: tokens.spacing[1],
} satisfies CSSObject;

const loadingDotStyle = {
  width: '6px',
  height: '6px',
  borderRadius: tokens.radius.full,
  background: tokens.bg.brand,
  animation: `${pulse} 1.2s ease-in-out infinite`,
  '&:nth-of-type(2)': {
    animationDelay: '0.15s',
  },
  '&:nth-of-type(3)': {
    animationDelay: '0.3s',
  },
  '@media (prefers-reduced-motion: reduce)': {
    animation: 'none',
    opacity: 1,
  },
} satisfies CSSObject;

const retryLinkStyle = {
  ...typography.button,
  display: 'flex',
  width: '100%',
  height: '52px',
  marginTop: tokens.spacing[8],
  borderRadius: tokens.radius.md,
  alignItems: 'center',
  justifyContent: 'center',
  background: tokens.bg.brand,
  color: tokens.text.onBrand,
  textDecoration: 'none',
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

    void completeLogin();
  }, [location.search, navigate]);

  return (
    <Page>
      <main css={contentStyle} aria-live="polite">
        <section css={statusContentStyle}>
          <div css={characterBackdropStyle}>
            <img css={characterStyle} src={chongchongCharacter} alt="" />
            {errorMessage ? <span css={errorBadgeStyle}>!</span> : null}
          </div>

          {errorMessage ? (
            <>
              <h1 css={titleStyle}>로그인을 완료하지 못했어요</h1>
              <p css={descriptionStyle}>{errorMessage}</p>
              <Link css={retryLinkStyle} to="/login">
                다시 로그인하기
              </Link>
            </>
          ) : (
            <>
              <h1 css={titleStyle}>총총으로 이동 중이에요</h1>
              <p css={descriptionStyle}>카카오 계정을 확인하고 스터디를 준비하고 있어요.</p>
              <div css={loadingStatusStyle} role="status">
                <span css={loadingDotsStyle} aria-hidden="true">
                  <span css={loadingDotStyle} />
                  <span css={loadingDotStyle} />
                  <span css={loadingDotStyle} />
                </span>
                안전하게 로그인하는 중
              </div>
            </>
          )}
        </section>
      </main>
    </Page>
  );
}
