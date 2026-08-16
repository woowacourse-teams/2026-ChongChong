import { tokens, typography } from '../../../styles/global';
import { CSSProperties } from 'react';
import Dialog, { DialogProps } from './Dialog';

interface ConfirmDialogProps extends Omit<DialogProps, 'actions' | 'role'> {
  closeButton: React.ReactNode;
  confirmButton: React.ReactNode;
}

const actionButtonStyle = {
  ...typography.button,
  display: 'flex',
  flex: 1,
  justifyContent: 'center',
  alignItems: 'center',
  padding: `${tokens.spacing[3]} ${tokens.spacing[5]}`,
  background: 'transparent',
  border: 'none',
  cursor: 'pointer',
} satisfies CSSProperties;

const closeButtonStyle = {
  ...actionButtonStyle,
  borderRadius: `0 0 0 ${tokens.radius.lg}`,
  color: tokens.text.secondary,
} satisfies CSSProperties;

const confirmButtonStyle = {
  ...actionButtonStyle,
  fontWeight: tokens.fontWeight.semibold,
  borderLeft: tokens.border.neutral,
  borderRadius: `0 0 ${tokens.radius.lg} 0`,
  color: tokens.text.critical,
} satisfies CSSProperties;

function ConfirmDialogRoot({
  ref,
  title,
  description,
  closeButton,
  confirmButton,
}: ConfirmDialogProps) {
  return (
    <Dialog
      ref={ref}
      role="alertdialog"
      title={title}
      description={description}
      actions={
        <>
          {closeButton}
          {confirmButton}
        </>
      }
    />
  );
}

function CloseButton({ children, ...props }: React.ComponentProps<'button'>) {
  return (
    <button type="button" css={closeButtonStyle} {...props}>
      {children}
    </button>
  );
}

function ConfirmButton({ children, ...props }: React.ComponentProps<'button'>) {
  return (
    <button type="button" css={confirmButtonStyle} {...props}>
      {children}
    </button>
  );
}

const ConfirmDialog = Object.assign(ConfirmDialogRoot, {
  CloseButton,
  ConfirmButton,
});

export default ConfirmDialog;
