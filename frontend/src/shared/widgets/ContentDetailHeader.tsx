import type { CSSProperties } from 'react';
import { tokens, typography } from '../../styles/global';

interface Props {
  title: string;
  dateTime: string;
  meta: string;
}

const headerStyle = {
  display: 'flex',
  flexDirection: 'column',
} satisfies CSSProperties;

const titleStyle = {
  ...typography.headline,
  margin: 0,
  color: tokens.text.primary,
} satisfies CSSProperties;

const metaStyle = {
  ...typography.footnote,
  marginTop: tokens.spacing[1],
  color: tokens.text.muted,
} satisfies CSSProperties;

export default function ContentDetailHeader({ title, dateTime, meta }: Props) {
  return (
    <header css={headerStyle}>
      <h2 css={titleStyle}>{title}</h2>
      <time css={metaStyle} dateTime={dateTime}>
        {meta}
      </time>
    </header>
  );
}
