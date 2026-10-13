import { CSSProperties } from 'react';
import { tokens } from '../../styles/global';
import { typography } from '../../styles/global';

export type Variant = 'brandSolid' | 'neutralSolid' | 'brandOutline' | 'warningSolid';

type Size = 'small' | 'large' | 'status';

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
  brandSolid: {
    background: tokens.bg.brand,
    color: tokens.text.onBrand,
  },
  neutralSolid: {
    background: tokens.bg.neutral,
    color: tokens.text.muted,
  },
  brandOutline: {
    background: tokens.bg.default,
    color: tokens.text.brand,
    border: tokens.border.brand,
  },
  warningSolid: {
    background: tokens.bg.warningSubtle,
    color: tokens.text.warning,
  },
};

const sizeStyle = {
  small: {
    padding: `3px ${tokens.spacing[2]}`,
    gap: tokens.spacing[1],
  },
  large: {
    padding: `5px ${tokens.spacing[3]} 5px 5px`,
    gap: '6px',
  },
  status: {
    ...typography.label,
    minHeight: '24px',
    padding: `3px ${tokens.spacing[2]}`,
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
