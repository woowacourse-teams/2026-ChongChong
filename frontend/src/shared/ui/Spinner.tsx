import { keyframes, type CSSObject } from '@emotion/react';
import { tokens } from '../../styles/global';

interface Props {
  size?: number;
  ariaLabel?: string;
}

const spin = keyframes`
  to {
    transform: rotate(360deg);
  }
`;

const containerStyle = {
  display: 'flex',
  flex: 1,
  width: '100%',
  justifyContent: 'center',
  alignItems: 'center',
  padding: `${tokens.spacing[8]} 0`,
} satisfies CSSObject;

const spinnerStyle = {
  flexShrink: 0,
  boxSizing: 'border-box',
  border: `3px solid ${tokens.bg.brandSubtle}`,
  borderTopColor: tokens.bg.brand,
  borderRadius: tokens.radius.full,
  animation: `${spin} 0.8s linear infinite`,
  '@media (prefers-reduced-motion: reduce)': {
    animation: 'none',
  },
} satisfies CSSObject;

export default function Spinner({ size = 32, ariaLabel = '로딩 중' }: Props) {
  return (
    <div role="status" aria-label={ariaLabel} css={containerStyle}>
      <span aria-hidden="true" css={[spinnerStyle, { width: size, height: size }]} />
    </div>
  );
}
