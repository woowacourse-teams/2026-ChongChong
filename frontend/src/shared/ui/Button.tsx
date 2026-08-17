import { typography } from '../../styles/global';
import { tokens } from '../../styles/global';

// bg + border
type Variant =
  'BrandSolid' | 'CriticalSolid' | 'BrandOutline' | 'CriticalOutline' | 'NeutralOutline';

// width + height
type Size = 'Small' | 'Large';

interface Props extends React.ComponentProps<'button'> {
  variant: Variant;
  size: Size;
}

const variantStyle = {
  BrandSolid: {
    background: tokens.bg.brand,
    border: 'none',
    color: tokens.text.onBrand,
  },
  CriticalSolid: {
    background: tokens.bg.critical,
    border: 'none',
    color: tokens.text.onBrand,
  },
  BrandOutline: {
    background: tokens.bg.default,
    border: tokens.border.brand,
    color: tokens.text.brand,
  },
  CriticalOutline: {
    background: tokens.bg.default,
    border: tokens.border.critical,
    color: tokens.text.critical,
  },
  NeutralOutline: {
    background: tokens.bg.default,
    border: tokens.border.neutral,
    color: tokens.text.primary,
  },
};

const sizeStyle = {
  Small: {
    width: '50%',
    height: '44px',
  },
  Large: {
    width: '100%',
    height: '52px',
  },
};

const ButtonStyle = {
  display: 'flex',
  padding: `${tokens.spacing[0]} ${tokens.spacing[6]}`,
  borderRadius: tokens.radius.md,
  justifyContent: 'center',
  alignItems: 'center',
  alignSelf: 'stretch',
};

export default function Button({ variant, size, children, ...props }: Props) {
  return (
    <button
      type="button"
      css={{ ...typography.button, ...ButtonStyle, ...variantStyle[variant], ...sizeStyle[size] }}
      {...props}
    >
      {children}
    </button>
  );
}
