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
    color: tokens.text.default,
  },
  CriticalSolid: {
    background: tokens.bg.critical,
    border: 'none',
    color: tokens.text.default,
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
    width: '171px',
    height: '44px',
  },
  Large: {
    width: '350px',
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
