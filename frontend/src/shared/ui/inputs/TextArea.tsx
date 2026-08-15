import { tokens, typography } from '../../../styles/global';
import type { CSSObject } from '@emotion/react';
import type { ComponentPropsWithoutRef } from 'react';

type TextAreaProps = ComponentPropsWithoutRef<'textarea'>;

const textAreaStyle = {
  width: '100%',
  minHeight: '96px',
  padding: tokens.spacing[4],
  alignItems: 'flex-start',
  borderRadius: tokens.radius.md,
  resize: 'none',
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

export default function TextArea(props: TextAreaProps) {
  return <textarea css={textAreaStyle} {...props} />;
}
