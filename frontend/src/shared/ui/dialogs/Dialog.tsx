import { CSSProperties, useId } from 'react';
import { tokens, typography } from '../../../styles/global';
import { CSSObject } from '@emotion/react';

export interface DialogProps {
  ref: React.RefObject<HTMLDialogElement | null>;
  title: string;
  description?: string;
  actions: React.ReactNode;
  role?: 'dialog' | 'alertdialog';
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
  } satisfies CSSProperties,
  '&::backdrop': {
    background: tokens.bg.dim,
  } satisfies CSSProperties,
} satisfies CSSObject;

const bodyStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  justifyContent: 'center',
  alignItems: 'center',
  gap: tokens.spacing[2],
  padding: `${tokens.spacing[6]} ${tokens.spacing[5]}`,
  textAlign: 'center',
} satisfies CSSProperties;

const actionsStyle = {
  display: 'flex',
  borderTop: tokens.border.neutral,
} satisfies CSSProperties;

const titleStyle = {
  ...typography.subtitle,
  fontWeight: tokens.fontWeight.semibold,
  color: tokens.text.default,
  whiteSpace: 'pre-line',
  margin: 0,
} satisfies CSSProperties;

const descriptionStyle = {
  ...typography.body,
  color: tokens.text.muted,
  whiteSpace: 'pre-line',
  margin: 0,
} satisfies CSSProperties;

export default function Dialog({ ref, title, description, actions, role = 'dialog' }: DialogProps) {
  const id = useId();
  const titleId = `${id}-title`;
  const descriptionId = `${id}-description`;

  return (
    <dialog
      ref={ref}
      role={role}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      css={dialogStyle}
    >
      <div css={bodyStyle}>
        <h2 id={titleId} css={titleStyle}>
          {title}
        </h2>
        {description && (
          <p id={descriptionId} css={descriptionStyle}>
            {description}
          </p>
        )}
      </div>
      <div css={actionsStyle}>{actions}</div>
    </dialog>
  );
}
