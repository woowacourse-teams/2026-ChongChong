import { CSSProperties, ReactNode } from 'react';
import { tokens, typography } from '../../../styles/global';
import Badge from '../Badge';

type TitleSize = 'default' | 'large';
type MetaTone = 'muted' | 'brand';

interface TitleProps extends React.ComponentProps<'h3'> {
  size?: TitleSize;
}

interface FooterProps extends React.ComponentProps<'footer'> {
  direction?: 'row' | 'column';
}

interface MetaProps extends React.ComponentProps<'span'> {
  tone?: MetaTone;
  icon?: ReactNode;
}

const baseStyle = {
  display: 'flex',
  width: '100%',
  height: '180px',
  flexDirection: 'column',
  padding: tokens.spacing[5],
  background: tokens.bg.default,
  border: tokens.border.neutral,
  borderRadius: tokens.radius.lg,
  color: tokens.text.default,
} satisfies CSSProperties;

const badgesStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[1],
  marginBottom: tokens.spacing[3],
} satisfies CSSProperties;

const titleRowStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[2],
} satisfies CSSProperties;

const titleStyle = {
  ...typography.subtitle,
  margin: 0,
  overflow: 'hidden',
  color: tokens.color.mainBlack,
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const largeTitleStyle = {
  ...typography.title,
  color: tokens.text.primary,
} satisfies CSSProperties;

const accessoryStyle = {
  ...typography.label,
  display: 'inline-flex',
  flex: '0 0 auto',
  alignItems: 'center',
  gap: tokens.spacing[1],
  color: tokens.text.brand,
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const trailingStyle = {
  display: 'inline-flex',
  width: '20px',
  height: '20px',
  flex: '0 0 20px',
  alignItems: 'center',
  justifyContent: 'center',
  marginLeft: 'auto',
  color: tokens.text.muted,
  fontSize: tokens.fontSize[24],
  lineHeight: tokens.lineHeight[20],
} satisfies CSSProperties;

const descriptionStyle = {
  ...typography.body,
  display: '-webkit-box',
  margin: `${tokens.spacing[1]} 0 0`,
  overflow: 'hidden',
  color: tokens.text.muted,
  WebkitBoxOrient: 'vertical',
  WebkitLineClamp: 2,
} satisfies CSSProperties;

const footerStyle = {
  display: 'flex',
  alignItems: 'flex-start',
  gap: tokens.spacing[2],
  marginTop: tokens.spacing[1],
} satisfies CSSProperties;

const metaStyle = {
  ...typography.footnote,
  display: 'inline-flex',
  alignItems: 'center',
  gap: '6px',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

function ContentCardRoot({ children, ...props }: React.ComponentProps<'article'>) {
  return (
    <article css={baseStyle} {...props}>
      {children}
    </article>
  );
}

function Badges({ children, ...props }: React.ComponentProps<'div'>) {
  return (
    <div css={badgesStyle} {...props}>
      {children}
    </div>
  );
}

function TitleRow({ children, ...props }: React.ComponentProps<'div'>) {
  return (
    <div css={titleRowStyle} {...props}>
      {children}
    </div>
  );
}

function Title({ size = 'default', children, ...props }: TitleProps) {
  return (
    <h3 css={{ ...titleStyle, ...(size === 'large' ? largeTitleStyle : {}) }} {...props}>
      {children}
    </h3>
  );
}

function Accessory({ children, ...props }: React.ComponentProps<'span'>) {
  return (
    <span css={accessoryStyle} {...props}>
      {children}
    </span>
  );
}

function Trailing({ children, ...props }: React.ComponentProps<'span'>) {
  return (
    <span css={trailingStyle} aria-hidden="true" {...props}>
      {children}
    </span>
  );
}

function Description({ children, ...props }: React.ComponentProps<'p'>) {
  return (
    <p css={descriptionStyle} {...props}>
      {children}
    </p>
  );
}

function Footer({ direction = 'row', children, ...props }: FooterProps) {
  return (
    <footer
      css={{
        ...footerStyle,
        flexDirection: direction,
        gap: direction === 'column' ? 0 : footerStyle.gap,
      }}
      {...props}
    >
      {children}
    </footer>
  );
}

function Meta({ tone = 'muted', icon, children, ...props }: MetaProps) {
  return (
    <span
      css={{ ...metaStyle, color: tone === 'brand' ? tokens.text.brand : tokens.text.muted }}
      {...props}
    >
      {icon && <>{icon}</>}
      {children}
    </span>
  );
}

const ContentCard = Object.assign(ContentCardRoot, {
  Accessory,
  Badge,
  Badges,
  Description,
  Footer,
  Meta,
  Title,
  TitleRow,
  Trailing,
});

export default ContentCard;
