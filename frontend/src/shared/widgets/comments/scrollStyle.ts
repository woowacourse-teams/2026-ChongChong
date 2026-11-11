import type { CSSObject } from '@emotion/react';
import { tokens } from '../../../styles/global';

export const commentScrollbarStyle = {
  overflowX: 'hidden',
  scrollbarWidth: 'thin',
  scrollbarColor: `${tokens.text.muted} transparent`,
  '&::-webkit-scrollbar': { width: '4px', height: 0 },
  '&::-webkit-scrollbar-track': { background: 'transparent' },
  '&::-webkit-scrollbar-thumb': { background: tokens.text.muted, borderRadius: '999px' },
} satisfies CSSObject;
