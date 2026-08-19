import { CSSProperties } from 'react';
import { tokens } from '../../styles/global';
import { typography } from '../../styles/global';

type Variant = 'BrandSolid' | 'NeutralSolid' | 'BrandOutline';

type Size = 'Small' | 'Large';

interface BadgeProps extends React.ComponentProps<'span'> {
  variant: Variant;
  size: Size;
  children: React.ReactNode;
}

const badgeStyle = {
  display: 'inline-flex',
  justifyContent: 'center',
  alignItems: 'center',
  borderRadius: tokens.radius.full,
} satisfies CSSProperties;

const variantStyle = {
  BrandSolid: {
    background: tokens.bg.brand,
    color: tokens.text.onBrand,
  },
  NeutralSolid: {
    background: tokens.bg.neutral,
    color: tokens.text.muted,
  },
  BrandOutline: {
    background: tokens.bg.default,
    color: tokens.text.brand,
    border: tokens.border.brand,
  },
};

const sizeStyle = {
  Small: {
    padding: `3px ${tokens.spacing[2]}`,
    gap: tokens.spacing[1],
  },
  Large: {
    padding: `5px ${tokens.spacing[3]} 5px 5px`,
    gap: '6px',
  },
};

export default function Badge({ variant, size, children, ...props }: BadgeProps) {
  return (
    <span
      css={{ ...typography.caption, ...badgeStyle, ...variantStyle[variant], ...sizeStyle[size] }}
      {...props}
    >
      {children}
    </span>
  );
}
