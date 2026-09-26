import type { CSSObject } from '@emotion/react';
import type { ComponentProps, CSSProperties } from 'react';
import { tokens } from '../../styles/global';

type SwitchProps = Omit<ComponentProps<'input'>, 'type' | 'role' | 'children'>;

const switchStyle = {
  position: 'relative',
  display: 'inline-flex',
  flexShrink: 0,
  width: 44,
  height: 24,
  verticalAlign: 'middle',
} satisfies CSSProperties;

const inputStyle = {
  position: 'absolute',
  inset: 0,
  width: '100%',
  height: '100%',
  margin: 0,
  opacity: 0,
  cursor: 'pointer',

  '&:checked + span': {
    background: tokens.bg.brand,
  },

  '&:checked + span > span': {
    transform: 'translateX(20px)',
  },

  '&:focus-visible + span': {
    outline: `2px solid ${tokens.text.primary}`,
    outlineOffset: 2,
  },

  '&:disabled': {
    cursor: 'default',
  },

  '&:disabled + span': {
    opacity: 0.5,
  },
} satisfies CSSObject;

const trackStyle = {
  display: 'inline-flex',
  width: '100%',
  height: '100%',
  padding: 2,
  boxSizing: 'border-box',
  borderRadius: tokens.radius.md,
  background: tokens.bg.neutral,
  pointerEvents: 'none',
} satisfies CSSProperties;

const thumbStyle = {
  width: 20,
  height: 20,
  borderRadius: '50%',
  background: tokens.bg.default,
} satisfies CSSProperties;

export default function Switch(props: SwitchProps) {
  return (
    <label css={switchStyle}>
      <input {...props} type="checkbox" role="switch" css={inputStyle} />
      <span aria-hidden="true" css={trackStyle}>
        <span css={thumbStyle} />
      </span>
    </label>
  );
}
