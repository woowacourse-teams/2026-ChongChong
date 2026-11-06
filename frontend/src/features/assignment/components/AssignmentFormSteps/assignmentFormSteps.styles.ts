import type { CSSObject } from '@emotion/react';

export const stepStyle = {
  display: 'flex',
  flexDirection: 'column',
  flex: 1,
  gap: 16,
  '& p, & h2': { margin: 0 },
} satisfies CSSObject;

export const questionStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: 8,
} satisfies CSSObject;

export const footerStyle = {
  marginTop: 'auto',
  paddingTop: 8,
  '& > button': { width: '100%' },
} satisfies CSSObject;
