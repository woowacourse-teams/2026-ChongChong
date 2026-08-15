import { tokens, typography } from '../../../styles/global';
import type { ComponentPropsWithoutRef } from 'react';
import type { CSSObject } from '@emotion/react';

type InputProps = ComponentPropsWithoutRef<'input'>;

const inputStyle = {
  width: '100%',
  minHeight: '52px',
  padding: tokens.spacing[4],
  alignItems: 'flex-start',
  borderRadius: tokens.radius.md,
  ...typography.label,
  border: tokens.border.default,

  '&:focus': {
    border: tokens.border.brand,
    outline: 'none',
  },

  '&[aria-invalid="true"]': {
    border: tokens.border.critical,
  },
} satisfies CSSObject;

export default function Input(props: InputProps) {
  return <input type="text" css={inputStyle} {...props} />;
}
