import { CSSObject } from '@emotion/react';
import { tokens } from '../../../styles/global';

export const FLOATING_SIZE = 56;

export const floatingMotion = {
  open: { durationMs: 300, easing: 'cubic-bezier(0.22, 1, 0.36, 1)' },
  close: { durationMs: 240, easing: 'cubic-bezier(0.4, 0, 0.2, 1)' },
};

export const floatingPositionStyle = {
  position: 'fixed',
  right: `max(${tokens.layout.gutter}, calc((100% - ${tokens.screenSize.default}) / 2 + ${tokens.layout.gutter}))`,
  bottom: `calc(${tokens.layout.safeBottom} + ${tokens.spacing[5]})`,
} satisfies CSSObject;
