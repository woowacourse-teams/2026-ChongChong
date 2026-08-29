import emptyIcon from '../assets/icons/error-icon.svg';
import { CSSProperties } from 'react';
import { tokens, typography } from '../../styles/global';

interface ErrorContentProps {
  message: string;
}

const divStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[1],
  justifyContent: 'center',
  alignItems: 'center',
  margin: 'auto 0',
} satisfies CSSProperties;

const messageStyle = {
  ...typography.sectionLabel,
  color: tokens.text.muted,
} satisfies CSSProperties;

export default function ErrorContent({ message }: ErrorContentProps) {
  return (
    <div css={divStyle}>
      <img src={emptyIcon} width={150} height={150} alt="오류" />
      <p css={messageStyle}>{message}</p>
    </div>
  );
}
