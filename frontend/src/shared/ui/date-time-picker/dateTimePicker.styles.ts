import type { CSSObject } from '@emotion/react';
import type { CSSProperties } from 'react';
import { tokens, typography } from '../../../styles/global';

export const overlayStyle = {
  position: 'fixed',
  inset: 0,
  zIndex: 1000,
  display: 'flex',
  alignItems: 'flex-end',
  justifyContent: 'center',
} satisfies CSSProperties;

export const backdropStyle = {
  position: 'absolute',
  inset: 0,
  background: tokens.bg.dim,
  willChange: 'opacity',
} satisfies CSSProperties;

export const sheetStyle = {
  position: 'relative',
  zIndex: 1,
  maxWidth: tokens.screenSize.default,
  maxHeight: '100dvh',
  padding: `${tokens.spacing[2]} ${tokens.spacing[5]} calc(${tokens.spacing[8]} + 2px + ${tokens.layout.safeBottom})`,
  borderRadius: `${tokens.spacing[6]} ${tokens.spacing[6]} ${tokens.spacing[0]} ${tokens.spacing[0]}`,
  background: tokens.bg.default,
  boxShadow: tokens.shadow[3],
  color: tokens.text.default,
  outline: 'none',
  overscrollBehavior: 'contain',
  willChange: 'transform',
} satisfies CSSProperties;

export const handleAreaStyle = {
  display: 'flex',
  width: '100%',
  height: tokens.spacing[8],
  alignItems: 'center',
  justifyContent: 'center',
  marginTop: `calc(${tokens.spacing[2]} * -1)`,
  marginBottom: `calc(${tokens.spacing[2]} * -1)`,
  cursor: 'grab',
  touchAction: 'none',
  userSelect: 'none',
} satisfies CSSProperties;

export const handleStyle = {
  width: '36px',
  height: tokens.spacing[1],
  borderRadius: '2px',
  background: tokens.color.optionSubFontColor55,
} satisfies CSSProperties;

export const headerStyle = {
  display: 'flex',
  width: '100%',
  height: '44px',
  alignItems: 'center',
  justifyContent: 'space-between',
  marginTop: tokens.spacing[2],
} satisfies CSSProperties;

export const titleStyle = {
  ...typography.title,
  margin: tokens.spacing[0],
  fontFamily: tokens.fontFamily.base,
  color: tokens.text.default,
} satisfies CSSProperties;

export const actionStyle = {
  ...typography.button,
  padding: tokens.spacing[0],
  border: 0,
  background: 'transparent',
  color: tokens.text.brand,
  cursor: 'pointer',
} satisfies CSSProperties;

export const summaryStyle = {
  display: 'flex',
  width: '100%',
  height: '44px',
  alignItems: 'center',
  justifyContent: 'center',
  marginTop: tokens.spacing[2],
  borderRadius: tokens.radius.md,
  background: tokens.bg.brandSubtle,
  color: tokens.text.default,
  ...typography.body,
} satisfies CSSProperties;

export const monthHeaderStyle = {
  display: 'grid',
  gridTemplateColumns: '40px 1fr 40px',
  height: tokens.spacing[10],
  alignItems: 'center',
  marginTop: tokens.spacing[2],
  textAlign: 'center',
  ...typography.body,
} satisfies CSSProperties;

export const monthButtonStyle = {
  display: 'grid',
  width: '40px',
  height: '40px',
  padding: tokens.spacing[0],
  placeItems: 'center',
  border: 0,
  background: 'transparent',
  color: tokens.text.placeholder,
  fontSize: tokens.fontSize[24],
  lineHeight: tokens.lineHeight[24],
  cursor: 'pointer',

  '&:disabled': {
    opacity: 0.28,
    cursor: 'default',
  },
} satisfies CSSObject;

export const weekdayRowStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(7, 1fr)',
  height: '20px',
  alignItems: 'center',
  color: tokens.text.placeholder,
  textAlign: 'center',
  ...typography.footnote,
} satisfies CSSProperties;

export const calendarGridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(7, 1fr)',
  gridAutoRows: '28px',
  marginTop: tokens.spacing[2],
} satisfies CSSProperties;

export const dayButtonStyle = {
  display: 'grid',
  width: '100%',
  height: '28px',
  padding: tokens.spacing[0],
  placeItems: 'center',
  border: 0,
  background: 'transparent',
  color: tokens.color.mainBlack,
  cursor: 'pointer',
  ...typography.body,

  '&:disabled': {
    cursor: 'default',
  },
} satisfies CSSObject;

export const selectedDayStyle = {
  display: 'grid',
  width: '28px',
  height: '28px',
  placeItems: 'center',
  borderRadius: tokens.radius.full,
  background: tokens.bg.brand,
  color: tokens.text.onBrand,
} satisfies CSSProperties;

export const timePickerStyle = {
  display: 'flex',
  width: '100%',
  marginTop: tokens.spacing[0],
  flexDirection: 'column',
  gap: tokens.spacing[3],
} satisfies CSSProperties;

export const periodControlStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(2, 1fr)',
  gap: tokens.spacing[2],
} satisfies CSSProperties;

export const periodButtonStyle = {
  height: '44px',
  padding: tokens.spacing[0],
  border: 0,
  borderRadius: tokens.radius.full,
  cursor: 'pointer',
  ...typography.subtitle,
} satisfies CSSProperties;

export const timeSectionStyle = {
  display: 'flex',
  width: '100%',
  flexDirection: 'column',
  gap: tokens.spacing[2],
} satisfies CSSProperties;

export const timeSectionLabelStyle = {
  ...typography.footnote,
  color: tokens.text.default,
} satisfies CSSProperties;

export const timeGridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(8, 40px)',
  justifyContent: 'space-between',
  rowGap: tokens.spacing[2],
} satisfies CSSProperties;

export const timeCellStyle = {
  width: '40px',
  height: '40px',
  padding: tokens.spacing[0],
  border: 0,
  borderRadius: tokens.radius.full,
  cursor: 'pointer',
  ...typography.subtitle,
} satisfies CSSProperties;

export const wheelPickerStyle = {
  position: 'relative',
  display: 'grid',
  width: '100%',
  height: '236px',
  marginTop: tokens.spacing[2],
  gridTemplateColumns: '1.2fr 1fr 1fr',
  overflow: 'hidden',
  border: tokens.border.subtle,
  borderRadius: tokens.radius.md,
  background: tokens.bg.default,
} satisfies CSSProperties;

export const wheelSelectionStyle = {
  position: 'absolute',
  zIndex: 0,
  top: '96px',
  right: tokens.spacing[2],
  left: tokens.spacing[2],
  height: '44px',
  borderRadius: tokens.radius.sm,
  background: tokens.bg.brandSubtle,
  pointerEvents: 'none',
} satisfies CSSProperties;

export const wheelColumnStyle = {
  position: 'relative',
  zIndex: 1,
  height: '236px',
  padding: '96px 0',
  overflowY: 'auto',
  overscrollBehavior: 'contain',
  scrollSnapType: 'y mandatory',
  scrollbarWidth: 'none',
  WebkitOverflowScrolling: 'touch',
  maskImage: 'linear-gradient(to bottom, transparent, black 24%, black 76%, transparent)',
  WebkitMaskImage: 'linear-gradient(to bottom, transparent, black 24%, black 76%, transparent)',

  '&::-webkit-scrollbar': {
    display: 'none',
  },
} satisfies CSSObject;

export const wheelOptionStyle = {
  display: 'flex',
  width: '100%',
  height: '44px',
  alignItems: 'center',
  justifyContent: 'center',
  padding: tokens.spacing[0],
  border: 0,
  background: 'transparent',
  color: tokens.text.default,
  cursor: 'pointer',
  scrollSnapAlign: 'center',
  ...typography.subtitle,
} satisfies CSSProperties;
