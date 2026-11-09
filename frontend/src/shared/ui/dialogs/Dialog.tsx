import { Heading, Paragraph } from '../Typography';
import { CSSProperties, useId, useRef, useEffect } from 'react';
import { tokens } from '../../../styles/global';
import { CSSObject } from '@emotion/react';
import { useBodyScrollLock } from '../../hooks/useBodyScrollLock';

export interface DialogProps {
  title: string;
  description?: string;
  onClose: () => void;
  actions: React.ReactNode;
  role?: 'dialog' | 'alertdialog';
  closeDisabled?: boolean;
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
  fontWeight: tokens.fontWeight.semibold,
  color: tokens.text.default,
  whiteSpace: 'pre-line',
  margin: 0,
} satisfies CSSProperties;

const descriptionStyle = {
  color: tokens.text.muted,
  whiteSpace: 'pre-line',
  margin: 0,
} satisfies CSSProperties;

export default function Dialog({
  title,
  description,
  onClose,
  actions,
  role = 'dialog',
  closeDisabled = false,
}: DialogProps) {
  const id = useId();
  const titleId = `${id}-title`;
  const descriptionId = `${id}-description`;
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    dialogRef.current?.showModal();
  }, []);

  useBodyScrollLock();

  return (
    <dialog
      ref={dialogRef}
      role={role}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      css={dialogStyle}
      onClose={onClose}
      onCancel={(event) => {
        if (closeDisabled) event.preventDefault();
      }}
    >
      <div css={bodyStyle}>
        <Heading variant="subtitle" level={2} id={titleId} css={titleStyle}>
          {title}
        </Heading>
        {description && (
          <Paragraph variant="body" id={descriptionId} css={descriptionStyle}>
            {description}
          </Paragraph>
        )}
      </div>
      <div css={actionsStyle}>{actions}</div>
    </dialog>
  );
}
