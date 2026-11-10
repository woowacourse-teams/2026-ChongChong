import { keyframes } from '@emotion/react';
import { tokens } from '../../styles/global';

interface Props {
  size?: number;
  label?: string;
}

const spin = keyframes`
  to {
    transform: rotate(360deg);
  }
`;

export default function Spinner({ size = 32, label = '로딩 중' }: Props) {
  return (
    <div
      role="status"
      aria-label={label}
      css={{
        display: 'flex',
        flex: 1,
        width: '100%',
        justifyContent: 'center',
        alignItems: 'center',
        padding: `${tokens.spacing[8]} 0`,
      }}
    >
      <span
        aria-hidden="true"
        css={{
          width: size,
          height: size,
          flexShrink: 0,
          boxSizing: 'border-box',
          border: `3px solid ${tokens.bg.brandSubtle}`,
          borderTopColor: tokens.bg.brand,
          borderRadius: tokens.radius.full,
          animation: `${spin} 0.8s linear infinite`,
          '@media (prefers-reduced-motion: reduce)': {
            animation: 'none',
          },
        }}
      />
    </div>
  );
}
