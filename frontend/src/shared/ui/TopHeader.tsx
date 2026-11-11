import { CSSProperties } from 'react';
import { tokens } from '../../styles/global';
import { Heading, Paragraph } from './Typography';

interface TopHeaderProps extends React.ComponentPropsWithoutRef<'header'> {
  left?: React.ReactNode;
  middle?: React.ReactNode;
  right?: React.ReactNode;
}

const topHeaderStyle = {
  position: 'sticky',
  background: tokens.bg.default,
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
  color: tokens.text.default,
  wordBreak: 'keep-all',
  margin: 0,
} satisfies CSSProperties;

const subtitleStyle = {
  color: tokens.text.muted,
  margin: 0,
} satisfies CSSProperties;

export default function TopHeader({ left, middle, right, ...props }: TopHeaderProps) {
  return (
    <header css={topHeaderStyle} {...props}>
      <div css={leftStyle}>{left}</div>
      <div css={middleStyle}>{middle}</div>
      <div css={rightStyle}>{right}</div>
    </header>
  );
}

TopHeader.Title = function Title({ children }: { children: React.ReactNode }) {
  return (
    <Heading level={1} variant="title" css={titleStyle}>
      {children}
    </Heading>
  );
};

TopHeader.Subtitle = function Subtitle({ children }: { children: React.ReactNode }) {
  return (
    <Paragraph variant="footnote" css={subtitleStyle}>
      {children}
    </Paragraph>
  );
};
