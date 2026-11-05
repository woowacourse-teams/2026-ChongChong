import { ReactNode } from 'react';
import { CSSObject } from '@emotion/react';
import { tokens } from '../../styles/global';

interface FloatingProps {
  icon: ReactNode;
}

const floatingPositionStyle = {
  position: 'fixed',
  right: `max(${tokens.layout.gutter}, calc((100% - ${tokens.screenSize.default}) / 2 + ${tokens.layout.gutter}))`,
  bottom: `calc(${tokens.layout.safeBottom} + ${tokens.spacing[5]})`,
} satisfies CSSObject;

const floatingStyle = {
  ...floatingPositionStyle,
  zIndex: 110,
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  width: 56,
  height: 56,
  padding: 0,
  border: 0,
  borderRadius: tokens.radius.full,
  background: tokens.bg.brand,
  color: tokens.text.onBrand,
  boxShadow: tokens.shadow[3],
  cursor: 'pointer',
  '&:focus-visible': { outline: `2px solid ${tokens.text.default}`, outlineOffset: '4px' },
  '&:disabled': { cursor: 'not-allowed', opacity: 0.5 },
  '&[hidden]': { display: 'none' },
} satisfies CSSObject;

export default function Floating({ icon, ...props }: FloatingProps) {
  return (
    <button type="button" css={floatingStyle} {...props}>
      <span
        aria-hidden="true"
        css={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
      >
        {icon}
      </span>
    </button>
  );
}
