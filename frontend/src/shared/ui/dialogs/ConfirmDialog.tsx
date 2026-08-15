import { tokens, typography } from '../../../styles/global';
import Dialog, { DialogDescription, DialogProps, DialogTitle } from './Dialog';

interface ConfirmDialogProps extends Omit<DialogProps, 'actions'> {
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
} as const;

const closeButtonStyle = {
  ...actionButtonStyle,
  borderRadius: `0 0 0 ${tokens.radius.lg}`,
  color: tokens.text.secondary,
} as const;

const confirmButtonStyle = {
  ...actionButtonStyle,
  fontWeight: tokens.fontWeight.semibold,
  borderLeft: tokens.border.neutral,
  borderRadius: `0 0 ${tokens.radius.lg} 0`,
  color: tokens.text.critical,
} as const;

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
      title={title}
      description={description}
      actions={
        <>
          {closeButton}
          {confirmButton}
        </>
      }
    ></Dialog>
  );
}

function CloseButton({ children, ...props }: React.ComponentProps<'button'>) {
  return (
    <button type="button" css={{ ...closeButtonStyle }} {...props}>
      {children}
    </button>
  );
}

function ConfirmButton({ children, ...props }: React.ComponentProps<'button'>) {
  return (
    <button type="button" css={{ ...confirmButtonStyle }} {...props}>
      {children}
    </button>
  );
}

const ConfirmDialog = Object.assign(ConfirmDialogRoot, {
  Title: DialogTitle,
  Description: DialogDescription,
  CloseButton,
  ConfirmButton,
});

export default ConfirmDialog;
