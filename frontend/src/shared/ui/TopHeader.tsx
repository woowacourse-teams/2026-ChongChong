import { CSSProperties } from 'react';
import { tokens, typography } from '../../styles/global';

interface TopHeaderProps {
  left?: React.ReactNode;
  middle?: React.ReactNode;
  right?: React.ReactNode;
}

const topHeaderStyle = {
  position: 'sticky',
  top: 0,
  zIndex: 10,
  display: 'grid',
  gridTemplateColumns: 'minmax(32px, auto) 1fr minmax(32px, auto)',
  alignItems: 'center',
  gap: tokens.spacing[2],
  boxSizing: 'border-box',
  width: '100%',
  minHeight: `calc(${tokens.layout.headerHeight} + ${tokens.layout.safeTop})`,
  paddingTop: tokens.layout.safeTop,
  paddingRight: tokens.layout.gutter,
  paddingLeft: tokens.layout.gutter,
  background: tokens.bg.default,
  color: tokens.text.default,
  fontFamily: tokens.fontFamily.base,
} satisfies CSSProperties;

const sideStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[1],
} satisfies CSSProperties;

const leftStyle = { ...sideStyle } satisfies CSSProperties;

const middleStyle = {
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  minWidth: 0,
  textAlign: 'center',
} satisfies CSSProperties;

const rightStyle = {
  ...sideStyle,
  justifyContent: 'flex-end',
} satisfies CSSProperties;

const titleStyle = {
  ...typography.title,
  color: tokens.text.default,
  margin: 0,
} satisfies CSSProperties;

const subtitleStyle = {
  ...typography.footnote,
  color: tokens.text.muted,
  margin: 0,
} satisfies CSSProperties;

function Title({ children }: { children: React.ReactNode }) {
  return <h1 css={titleStyle}>{children}</h1>;
}

function Subtitle({ children }: { children: React.ReactNode }) {
  return <p css={subtitleStyle}>{children}</p>;
}

function TopHeaderRoot({ left, middle, right }: TopHeaderProps) {
  return (
    <header css={topHeaderStyle}>
      <div css={leftStyle}>{left}</div>
      <div css={middleStyle}>{middle}</div>
      <div css={rightStyle}>{right}</div>
    </header>
  );
}

const TopHeader = Object.assign(TopHeaderRoot, {
  Title,
  Subtitle,
});

export default TopHeader;
