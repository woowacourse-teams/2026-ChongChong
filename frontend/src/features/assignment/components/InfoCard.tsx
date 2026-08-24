// AssignmentInfoCard.tsx
import type { CSSProperties, ReactNode } from 'react';
import { tokens, typography } from '../../../styles/global';

interface Props {
  icon: string;
  title: string;
  children: ReactNode;
}

const sectionStyle = {
  display: 'flex',
  minHeight: '84px',
  boxSizing: 'border-box',
  flexDirection: 'column',
  padding: tokens.spacing[5],
  borderRadius: tokens.radius.md,
  background: tokens.bg.subtle,
} satisfies CSSProperties;

const titleStyle = {
  ...typography.body,
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[1],
  margin: 0,
  color: tokens.text.brand,
} satisfies CSSProperties;

const contentStyle = {
  ...typography.body,
  margin: `${tokens.spacing[1]} 0 0`,
  color: tokens.text.muted,
  whiteSpace: 'pre-line',
  overflowWrap: 'anywhere',
} satisfies CSSProperties;

export default function InfoCard({ icon, title, children }: Props) {
  return (
    <section css={sectionStyle}>
      <h3 css={titleStyle}>
        <img src={icon} alt="" aria-hidden="true" width={16} height={16} />
        {title}
      </h3>

      <div css={contentStyle}>{children}</div>
    </section>
  );
}
