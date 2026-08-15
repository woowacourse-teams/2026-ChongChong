import { tokens, typography } from '../../../styles/global';
import type { CSSObject } from '@emotion/react';

interface TextAreaProps {
  onChange: () => void;
  placeholder: string;
}

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
} satisfies CSSObject;

export default function TextArea({ onChange, placeholder }: TextAreaProps) {
  return <textarea css={{ ...textAreaStyle }} onChange={onChange} placeholder={placeholder} />;
}
