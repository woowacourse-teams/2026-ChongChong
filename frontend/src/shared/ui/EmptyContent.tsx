import { Paragraph } from './Typography';
import emptyIcon from '../assets/icons/empty-icon.webp';
import { CSSProperties } from 'react';
import { tokens } from '../../styles/global';

interface EmptyContentProps {
  message: string;
}

const divStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  gap: tokens.spacing[1],
  justifyContent: 'center',
  alignItems: 'center',
} satisfies CSSProperties;

const messageStyle = {
  color: tokens.text.muted,
} satisfies CSSProperties;

export default function EmptyState({ message }: EmptyContentProps) {
  return (
    <div css={divStyle}>
      <img src={emptyIcon} width={300} height={300} alt="빈 목록" />
      <Paragraph variant="sectionLabel" css={messageStyle}>
        {message}
      </Paragraph>
    </div>
  );
}
