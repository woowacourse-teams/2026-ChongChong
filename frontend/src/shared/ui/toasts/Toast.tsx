import { CSSProperties, ReactNode } from 'react';
import { tokens } from '../../../styles/global';

interface ToastProps {
  content: ReactNode;
}

const toastStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: tokens.spacing[3],
  boxSizing: 'border-box',
  width: '100%',
  maxWidth: '300px',
  minHeight: '52px',
  fontSize: tokens.fontSize[18],
  padding: `${tokens.spacing[3]} ${tokens.spacing[6]}`,
  borderRadius: tokens.radius.full,
  background: tokens.bg.default,
  color: tokens.text.primary,
  boxShadow: tokens.shadow[3],
} satisfies CSSProperties;

export function ToastRoot({ content }: ToastProps) {
  return (
    <div css={toastStyle} role="status" aria-live="polite" aria-atomic="true">
      {content}
    </div>
  );
}

export function Toast({ message }: { message: string }) {
  return <ToastRoot content={<div>{message}</div>}></ToastRoot>;
}
