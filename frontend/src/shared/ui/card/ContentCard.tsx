import { Heading, Paragraph, Text } from '../Typography';
import { CSSProperties, ReactNode, ComponentProps } from 'react';
import { tokens, typography } from '../../../styles/global';

type TitleSize = 'default' | 'large';
type MetaTone = 'muted' | 'brand';

interface ChildrenProps {
  children: ReactNode;
}

interface ContentCardProps extends ChildrenProps {
  onClick?: () => void;
}

interface TitleProps extends Omit<ComponentProps<typeof Heading>, 'variant' | 'level'> {
  size?: TitleSize;
  level?: ComponentProps<typeof Heading>['level'];
  truncate?: boolean;
}

interface DescriptionProps extends Omit<ComponentProps<typeof Paragraph>, 'variant'> {
  truncate?: boolean;
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
  margin: 0,
  color: tokens.color.mainBlack,
} satisfies CSSProperties;

const truncatedTitleStyle = {
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const largeTitleStyle = {
  color: tokens.text.primary,
} satisfies CSSProperties;

const accessoryStyle = {
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
  margin: `${tokens.spacing[1]} 0 0`,
  color: tokens.text.muted,
} satisfies CSSProperties;

const truncatedDescriptionStyle = {
  display: '-webkit-box',
  WebkitBoxOrient: 'vertical',
  WebkitLineClamp: 2,
  whiteSpace: 'nowrap',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
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
  width: '100%',
  minWidth: 0,
  alignItems: 'center',
  gap: '6px',
} satisfies CSSProperties;

const metaTextStyle = {
  minWidth: 0,
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

export default function ContentCard({ children, onClick }: ContentCardProps) {
  return (
    <article css={baseStyle} onClick={onClick}>
      {children}
    </article>
  );
}

ContentCard.Badges = function Badges({ children }: ChildrenProps) {
  return <div css={badgesStyle}>{children}</div>;
};

ContentCard.TitleRow = function TitleRow({ children, ...props }: ComponentProps<'div'>) {
  return (
    <div css={titleRowStyle} {...props}>
      {children}
    </div>
  );
};

ContentCard.Title = function Title({
  size = 'default',
  level = 3,
  truncate = true,
  children,
  ...props
}: TitleProps) {
  return (
    <Heading
      level={level}
      variant={size === 'large' ? 'title' : 'subtitle'}
      css={[titleStyle, size === 'large' && largeTitleStyle, truncate && truncatedTitleStyle]}
      {...props}
    >
      {children}
    </Heading>
  );
};

ContentCard.Accessory = function Accessory({ children }: ChildrenProps) {
  return (
    <Text variant="label" css={accessoryStyle}>
      {children}
    </Text>
  );
};

ContentCard.Trailing = function Trailing({ children }: ChildrenProps) {
  return (
    <Text variant="inherited" css={trailingStyle} aria-hidden="true">
      {children}
    </Text>
  );
};

ContentCard.Description = function Description({
  truncate = true,
  children,
  ...props
}: DescriptionProps) {
  return (
    <Paragraph
      variant="body"
      css={[descriptionStyle, truncate && truncatedDescriptionStyle]}
      {...props}
    >
      {children}
    </Paragraph>
  );
};

ContentCard.Footer = function Footer({ direction = 'row', children }: FooterProps) {
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
};

ContentCard.Meta = function Meta({ tone = 'muted', icon, children }: MetaProps) {
  return (
    <Text
      variant="inherited"
      css={{ ...metaStyle, color: tone === 'brand' ? tokens.text.brand : tokens.text.muted }}
    >
      {icon && <>{icon}</>}
      <Text variant="inherited" css={metaTextStyle}>
        {children}
      </Text>
    </Text>
  );
};
