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
  minHeight: '52px',
  fontSize: tokens.fontSize[14],
  padding: `${tokens.spacing[3]} ${tokens.spacing[6]}`,
  borderRadius: tokens.radius.xl,
  background: tokens.bg.default,
  color: tokens.text.primary,
  boxShadow: tokens.shadow[2],
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
