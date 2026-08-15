import { tokens, typography } from '../../../styles/global';
import type { ComponentPropsWithoutRef } from 'react';
import type { CSSObject } from '@emotion/react';
import type { CSSProperties } from 'react';

type InputProps = ComponentPropsWithoutRef<'input'>;

const inputBaseStyle = {
  width: '100%',
  minHeight: '52px',
  padding: tokens.spacing[4],
  alignItems: 'flex-start',
  borderRadius: tokens.radius.md,
  ...typography.label,
  border: tokens.border.default,
} satisfies CSSProperties;

const inputStyle = {
  ...inputBaseStyle,

  '&:focus': {
    border: tokens.border.brand,
    outline: 'none',
  } satisfies CSSProperties,

  '&[aria-invalid="true"]': {
    border: tokens.border.critical,
  } satisfies CSSProperties,
} satisfies CSSObject;

export default function Input(props: InputProps) {
  return <input type="text" css={inputStyle} {...props} />;
}
