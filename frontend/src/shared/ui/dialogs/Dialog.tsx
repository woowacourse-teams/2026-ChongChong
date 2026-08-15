import { tokens, typography } from '../../../styles/global';

export interface DialogProps {
  ref: React.RefObject<HTMLDialogElement | null>;
  title: React.ReactNode;
  description: React.ReactNode;
  actions: React.ReactNode;
}

const dialogStyle = {
  width: '310px',
  minHeight: '173px',
  padding: tokens.spacing[0],
  background: tokens.bg.default,
  border: tokens.border.neutral,
  borderRadius: tokens.radius.lg,
  boxShadow: tokens.shadow[3],
  color: tokens.text.default,
  fontFamily: tokens.fontFamily.base,
  '&[open]': {
    display: 'flex',
    flexDirection: 'column',
  },
  '&::backdrop': {
    background: tokens.bg.dim,
  },
} as const;

const bodyStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  justifyContent: 'center',
  alignItems: 'center',
  gap: tokens.spacing[2],
  padding: `${tokens.spacing[6]} ${tokens.spacing[5]}`,
  textAlign: 'center',
} as const;

const actionsStyle = {
  display: 'flex',
  borderTop: tokens.border.neutral,
} as const;

const titleStyle = {
  ...typography.subtitle,
  fontWeight: tokens.fontWeight.semibold,
  color: tokens.text.default,
  whiteSpace: 'pre-line',
  margin: 0,
} as const;

const descriptionStyle = {
  ...typography.body,
  color: tokens.text.muted,
  whiteSpace: 'pre-line',
  margin: 0,
} as const;

export function DialogTitle({ children }: { children: React.ReactNode }) {
  return <h3 css={{ ...titleStyle }}>{children}</h3>;
}

export function DialogDescription({ children }: { children: React.ReactNode }) {
  return <p css={{ ...descriptionStyle }}>{children}</p>;
}

export default function Dialog({ ref, title, description, actions }: DialogProps) {
  return (
    <dialog ref={ref} css={{ ...dialogStyle }}>
      <div css={{ ...bodyStyle }}>
        {title}
        {description}
      </div>
      <div css={{ ...actionsStyle }}>{actions}</div>
    </dialog>
  );
}
