import { Heading, Paragraph } from '../../../shared/ui/Typography';
import type { CSSObject } from '@emotion/react';
import Page from '../../../shared/ui/Page';
import chongchongCharacter from '../../../shared/assets/icons/header-icon.svg?width=360&as=webp';
import { tokens } from '../../../styles/global';
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
  margin: '18px 0 0',
  color: tokens.text.primary,
  textAlign: 'center',
} satisfies CSSObject;

const descriptionStyle = {
  margin: `${tokens.spacing[2]} 0 0`,
  color: tokens.text.secondary,
  textAlign: 'center',
} satisfies CSSObject;

const kakaoLoginButtonContainerStyle = {
  width: '100%',
  marginTop: 'auto',
} satisfies CSSObject;

const termsStyle = {
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

        <Heading variant="headline" level={1} css={titleStyle}>
          총총에 오신 걸 환영해요
        </Heading>
        <Paragraph variant="body" css={descriptionStyle}>
          번거로운 스터디 운영, 이제 총총에게 맡기세요
        </Paragraph>

        <div css={kakaoLoginButtonContainerStyle}>
          <KakaoLoginButton onClick={startKakaoLogin} />
        </div>

        <Paragraph variant="caption" css={termsStyle}>
          계속하면 서비스 이용약관과 개인정보 처리방침에 동의하게 됩니다.
        </Paragraph>
      </main>
    </Page>
  );
}
