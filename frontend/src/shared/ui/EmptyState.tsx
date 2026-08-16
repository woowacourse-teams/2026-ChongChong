import emptyIcon from '../assets/icons/empty-icon.svg';
import { CSSProperties } from 'react';
import { tokens, typography } from '../../styles/global';

interface EmptyStateProps {
  message: string;
}

const divStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[1],
  justifyContent: 'center',
  alignItems: 'center',
} satisfies CSSProperties;

const messageStyle = {
  ...typography.sectionLabel,
  color: tokens.text.muted,
} satisfies CSSProperties;

export default function EmptyState({ message }: EmptyStateProps) {
  return (
    <div css={divStyle}>
      <img src={emptyIcon} width={150} height={150} alt="빈 목록" />
      <p css={messageStyle}>{message}</p>
    </div>
  );
}
