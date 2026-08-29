import { ReactNode } from 'react';
import { CSSProperties } from 'react';
import { tokens } from '../../styles/global';

interface PageProps {
  children: ReactNode;
}

const pageStyle = {
  display: 'flex',
  minHeight: '100dvh',
  flexDirection: 'column',
  background: tokens.bg.default,
  maxWidth: tokens.screenSize.default,
  margin: '0 auto',
} satisfies CSSProperties;

export default function Page({ children }: PageProps) {
  return <div css={pageStyle}>{children}</div>;
}
