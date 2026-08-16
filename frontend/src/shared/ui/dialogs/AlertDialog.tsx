import { tokens, typography } from '../../../styles/global';
import { CSSProperties } from 'react';
import Dialog, { DialogProps } from './Dialog';

interface AlertDialogProps extends Omit<DialogProps, 'actions' | 'role'> {
  closeButton: React.ReactNode;
}

const closeButtonStyle = {
  ...typography.button,
  fontWeight: tokens.fontWeight.semibold,
  display: 'flex',
  flex: 1,
  justifyContent: 'center',
  alignItems: 'center',
  padding: `${tokens.spacing[3]} ${tokens.spacing[5]}`,
  background: 'transparent',
  border: 'none',
  borderRadius: `0 0 ${tokens.radius.lg} ${tokens.radius.lg}`,
  color: tokens.text.brand,
  cursor: 'pointer',
} satisfies CSSProperties;

function AlertDialogRoot({ ref, title, description, closeButton }: AlertDialogProps) {
  return (
    <Dialog
      ref={ref}
      role="alertdialog"
      title={title}
      description={description}
      actions={closeButton}
    />
  );
}

function CloseButton({ children, ...props }: React.ComponentProps<'button'>) {
  return (
    <button type="button" css={{ ...closeButtonStyle }} {...props}>
      {children}
    </button>
  );
}

const AlertDialog = Object.assign(AlertDialogRoot, {
  CloseButton,
});

export default AlertDialog;
