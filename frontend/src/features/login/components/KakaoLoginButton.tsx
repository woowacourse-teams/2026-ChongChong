import type { CSSObject } from '@emotion/react';
import kakaoIcon from '../../../shared/assets/Social Login/Icon/kakao.svg';
import { tokens, typography } from '../../../styles/global';

const buttonStyle = {
  ...typography.button,
  display: 'flex',
  width: '100%',
  height: '56px',
  padding: `0 ${tokens.spacing[6]}`,
  border: 0,
  borderRadius: tokens.radius.md,
  alignItems: 'center',
  justifyContent: 'center',
  gap: tokens.spacing[2],
  background: tokens.color.socialKakao,
  color: tokens.text.primary,
  cursor: 'pointer',
} satisfies CSSObject;

const iconStyle = {
  width: '20px',
  height: '20px',
  objectFit: 'contain',
} satisfies CSSObject;

type Props = React.ComponentProps<'button'>;

export default function KakaoLoginButton(props: Props) {
  return (
    <button css={buttonStyle} type="button" {...props}>
      <img css={iconStyle} src={kakaoIcon} alt="" />
      <span>카카오로 계속하기</span>
    </button>
  );
}
