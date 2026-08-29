import { CSSProperties } from 'react';
import { typography, tokens } from '../../styles/global';
import underConstructionIcon from '../assets/icons/no-construction-icon.svg';

interface Props {
  message: string;
}

const divStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[1],
  justifyContent: 'center',
  alignItems: 'center',
  marginBottom: tokens.spacing[6],
} satisfies CSSProperties;

const messageStyle = {
  ...typography.sectionLabel,
  color: tokens.text.muted,
} satisfies CSSProperties;

export default function UnderConstructionContent({ message }: Props) {
  return (
    <div css={divStyle}>
      <img src={underConstructionIcon} width={150} height={150} alt="빈 목록" />
      <p css={messageStyle}>{message}</p>
    </div>
  );
}
