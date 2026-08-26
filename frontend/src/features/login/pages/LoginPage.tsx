import type { CSSObject } from '@emotion/react';
import Page from '../../../shared/ui/Page';
import chongchongCharacter from '../../../shared/assets/icons/header-icon.svg';
import { tokens, typography } from '../../../styles/global';
import KakaoLoginButton from '../components/KakaoLoginButton';
import { startKakaoLogin } from '../kakaoOAuth';

const contentStyle = {
  display: 'flex',
  width: '100%',
  maxWidth: '390px',
  minHeight: '100dvh',
  margin: '0 auto',
  padding: 'min(27.84dvh, 235px) 20px calc(34px + env(safe-area-inset-bottom, 0px))',
  flexDirection: 'column',
  alignItems: 'center',
} satisfies CSSObject;

const characterStyle = {
  width: '118px',
  height: '118px',
  objectFit: 'contain',
} satisfies CSSObject;

const titleStyle = {
  ...typography.headline,
  margin: '18px 0 0',
  color: tokens.text.primary,
  textAlign: 'center',
} satisfies CSSObject;

const descriptionStyle = {
  ...typography.body,
  margin: `${tokens.spacing[2]} 0 0`,
  color: tokens.text.secondary,
  textAlign: 'center',
} satisfies CSSObject;

const kakaoLoginButtonContainerStyle = {
  width: '100%',
  marginTop: 'auto',
} satisfies CSSObject;

const termsStyle = {
  ...typography.caption,
  maxWidth: '330px',
  margin: `${tokens.spacing[4]} 0 0`,
  color: tokens.text.secondary,
  textAlign: 'center',
} satisfies CSSObject;

export default function LoginPage() {
  return (
    <Page>
      <main css={contentStyle}>
        <img css={characterStyle} src={chongchongCharacter} alt="" />

        <h1 css={titleStyle}>총총에 오신 걸 환영해요</h1>
        <p css={descriptionStyle}>번거로운 스터디 운영, 이제 총총에게 맡기세요</p>

        <div css={kakaoLoginButtonContainerStyle}>
          <KakaoLoginButton onClick={startKakaoLogin} />
        </div>

        <p css={termsStyle}>계속하면 서비스 이용약관과 개인정보 처리방침에 동의하게 됩니다.</p>
      </main>
    </Page>
  );
}
