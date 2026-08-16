import { CSSProperties, ReactNode } from 'react';
import { tokens, typography } from '../../../styles/global';
import Badge from '../Badge';

type TitleSize = 'default' | 'large';
type MetaTone = 'muted' | 'brand';

interface ChildrenProps {
  children: ReactNode;
}

interface ContentCardProps extends ChildrenProps {
  onClick?: () => void;
}

interface TitleProps extends ChildrenProps {
  size?: TitleSize;
}

interface FooterProps extends ChildrenProps {
  direction?: 'row' | 'column';
}

interface MetaProps extends ChildrenProps {
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
  color: tokens.text.muted,
  WebkitBoxOrient: 'vertical',
  WebkitLineClamp: 2,
} satisfies CSSProperties;

const footerStyle = {
  display: 'flex',
  alignItems: 'flex-start',
  gap: tokens.spacing[2],
  marginTop: 'auto',
} satisfies CSSProperties;

const metaStyle = {
  ...typography.footnote,
  display: 'inline-flex',
  alignItems: 'center',
  gap: '6px',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

function ContentCardRoot({ children, onClick }: ContentCardProps) {
  return (
    <article css={baseStyle} onClick={onClick}>
      {children}
    </article>
  );
}

function Badges({ children }: ChildrenProps) {
  return <div css={badgesStyle}>{children}</div>;
}

function TitleRow({ children }: ChildrenProps) {
  return <div css={titleRowStyle}>{children}</div>;
}

function Title({ size = 'default', children }: TitleProps) {
  return <h3 css={{ ...titleStyle, ...(size === 'large' ? largeTitleStyle : {}) }}>{children}</h3>;
}

function Accessory({ children }: ChildrenProps) {
  return <span css={accessoryStyle}>{children}</span>;
}

function Trailing({ children }: ChildrenProps) {
  return (
    <span css={trailingStyle} aria-hidden="true">
      {children}
    </span>
  );
}

function Description({ children }: ChildrenProps) {
  return <p css={descriptionStyle}>{children}</p>;
}

function Footer({ direction = 'row', children }: FooterProps) {
  return (
    <footer
      css={{
        ...footerStyle,
        flexDirection: direction,
        gap: direction === 'column' ? 0 : footerStyle.gap,
      }}
    >
      {children}
    </footer>
  );
}

function Meta({ tone = 'muted', icon, children }: MetaProps) {
  return (
    <span css={{ ...metaStyle, color: tone === 'brand' ? tokens.text.brand : tokens.text.muted }}>
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
