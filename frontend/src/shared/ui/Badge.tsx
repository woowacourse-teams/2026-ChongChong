import { tokens } from '../../styles/global';
import { typography } from '../../styles/global';

type Variant = 'BrandSolid' | 'NeutralSolid' | 'BrandOutline';

type Size = 'Small' | 'Large';

interface BadgeProps {
  variant: Variant;
  size: Size;
  children: React.ReactNode;
}

const BadgeStyle = {
  display: 'inline-flex',
  justifyContent: 'center',
  alignItems: 'center',
  borderRadius: tokens.radius.full,
};

const VariantStyle = {
  BrandSolid: {
    background: tokens.bg.brand,
    color: tokens.text.onBrand,
  },
  NeutralSolid: {
    background: tokens.bg.neutral,
    color: tokens.text.onBrandStrong,
  },
  BrandOutline: {
    background: tokens.bg.default,
    color: tokens.text.brand,
    border: tokens.border.brand,
  },
};

const SizeStyle = {
  Small: {
    padding: `3px ${tokens.spacing[2]}`,
    gap: tokens.spacing[1],
  },
  Large: {
    padding: `5px ${tokens.spacing[3]} 5px 5px`,
    gap: '6px',
  },
};

export default function Badge({ variant, size, children, ...rest }: BadgeProps) {
  return (
    <span
      css={{ ...typography.caption, ...BadgeStyle, ...VariantStyle[variant], ...SizeStyle[size] }}
      {...rest}
    >
      {children}
    </span>
  );
}
