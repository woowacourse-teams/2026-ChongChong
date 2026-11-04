import { ComponentPropsWithRef, CSSProperties, ElementType, ReactNode } from 'react';
import { CSSObject } from '@emotion/react';
import { tokens } from '../../styles/global';
import { FLOATING_SIZE, floatingPositionStyle } from './floatingStyles';

type FloatingProps<T extends ElementType> = {
  as?: T;
  icon: ReactNode;
  'aria-label': string;
  bottom?: CSSProperties['bottom'];
} & Omit<ComponentPropsWithRef<T>, 'as' | 'icon' | 'children' | 'aria-label' | 'bottom'>;

const floatingStyle = {
  ...floatingPositionStyle,
  zIndex: 110,
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  width: FLOATING_SIZE,
  height: FLOATING_SIZE,
  padding: 0,
  border: 0,
  borderRadius: tokens.radius.full,
  background: tokens.bg.brand,
  color: tokens.text.onBrand,
  boxShadow: tokens.shadow[3],
  cursor: 'pointer',
  textDecoration: 'none',
  WebkitTapHighlightColor: 'transparent',
  '&:focus-visible': { outline: `2px solid ${tokens.text.default}`, outlineOffset: '4px' },
  '&:disabled': { cursor: 'not-allowed', opacity: 0.5 },
  '&[hidden]': { display: 'none' },
} satisfies CSSObject;

/** 동작은 as와 네이티브 props로 조합하고, Floating은 배치와 아이콘 표현만 담당한다. */
export default function Floating<T extends ElementType = 'button'>({
  as,
  icon,
  bottom,
  ...props
}: FloatingProps<T>) {
  const Component = as ?? 'button';

  return (
    <Component
      {...(Component === 'button' ? { type: 'button' } : {})}
      css={[floatingStyle, bottom !== undefined && { bottom }]}
      {...props}
    >
      <span
        aria-hidden="true"
        css={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
      >
        {icon}
      </span>
    </Component>
  );
}
