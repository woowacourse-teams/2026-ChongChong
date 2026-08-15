import { tokens, typography } from '../../../styles/global';
import type { CSSObject } from '@emotion/react';
import type { CSSProperties } from 'react';
import type { ComponentPropsWithoutRef } from 'react';

type TextAreaProps = ComponentPropsWithoutRef<'textarea'>;

const textAreaBaseStyle = {
  width: '100%',
  minHeight: '96px',
  padding: tokens.spacing[4],
  alignItems: 'flex-start',
  borderRadius: tokens.radius.md,
  resize: 'none',
  ...typography.label,
  border: tokens.border.default,
} satisfies CSSProperties;

const textAreaStyle = {
  ...textAreaBaseStyle,

  '&:focus': {
    border: tokens.border.brand,
    outline: 'none',
  } satisfies CSSProperties,

  '&[aria-invalid="true"]': {
    border: tokens.border.critical,
  } satisfies CSSProperties,
} satisfies CSSObject;

export default function TextArea(props: TextAreaProps) {
  return <textarea css={textAreaStyle} {...props} />;
}
