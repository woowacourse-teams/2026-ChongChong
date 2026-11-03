import type { CSSProperties, ReactNode } from 'react';
import { tokens, typography } from '../../../styles/global';

const StudyCardStyle = {
  display: 'flex',
  width: '100%',
  height: '148px',
  flexDirection: 'column',
  padding: '15px 19px',
  background: tokens.bg.default,
  border: tokens.border.neutral,
  borderRadius: tokens.radius.lg,
  color: tokens.text.default,
} satisfies CSSProperties;

const StudyBadgesStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: '11px',
  marginBottom: tokens.spacing[3],
} satisfies CSSProperties;

const StudyDescriptionStyle = {
  ...typography.body,
  display: '-webkit-box',
  margin: `${tokens.spacing[1]} 0 0`,
  color: tokens.text.muted,
  WebkitBoxOrient: 'vertical',
  WebkitLineClamp: 2,
  whiteSpace: 'normal',
  overflowWrap: 'anywhere',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
} satisfies CSSProperties;

export default function StudyContentCard({ children }: { children: ReactNode }) {
  return <article css={StudyCardStyle}>{children}</article>;
}

StudyContentCard.Badges = function Badges({ children }: { children: ReactNode }) {
  return <div css={StudyBadgesStyle}>{children}</div>;
};

StudyContentCard.Description = function Description({ children }: { children: ReactNode }) {
  return <p css={StudyDescriptionStyle}>{children}</p>;
};
