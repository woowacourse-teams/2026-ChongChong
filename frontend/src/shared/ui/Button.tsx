import { typography } from '../../styles/global';
import { tokens } from '../../styles/global';

// bg + border
type Variant =
  'brandSolid' | 'criticalSolid' | 'brandOutline' | 'criticalOutline' | 'neutralOutline';

// width + height
type Size = 'small' | 'large';

interface Props extends React.ComponentProps<'button'> {
  variant: Variant;
  size: Size;
}

const variantStyle = {
  brandSolid: {
    background: tokens.bg.brand,
    border: 'none',
    color: tokens.text.onBrand,
    disabled: {
      background: tokens.color.optionPlaceholder40,
    },
  },
  criticalSolid: {
    background: tokens.bg.critical,
    border: 'none',
    color: tokens.text.onBrand,
    disabled: {
      background: tokens.bg.critical,
    },
  },
  brandOutline: {
    background: tokens.bg.default,
    border: tokens.border.brand,
    color: tokens.text.brand,
    disabled: {
      background: tokens.bg.default,
    },
  },
  criticalOutline: {
    background: tokens.bg.default,
    border: tokens.border.critical,
    color: tokens.text.critical,
    disabled: {
      background: tokens.bg.default,
    },
  },
  neutralOutline: {
    background: tokens.bg.default,
    border: tokens.border.neutral,
    color: tokens.text.brand,
    disabled: {
      background: tokens.bg.default,
    },
  },
};

const sizeStyle = {
  small: {
    width: '50%',
    height: '44px',
  },
  large: {
    width: '100%',
    height: '52px',
  },
};

const buttonStyle = {
  display: 'flex',
  padding: `${tokens.spacing[0]} ${tokens.spacing[6]}`,
  borderRadius: tokens.radius.md,
  justifyContent: 'center',
  alignItems: 'center',
  alignSelf: 'stretch',
};

export default function Button({ variant, size, children, ...props }: Props) {
  const { disabled: disabledStyle, ...variantBase } = variantStyle[variant];

  return (
    <button
      type="button"
      css={{
        ...typography.button,
        ...buttonStyle,
        ...variantBase,
        ...sizeStyle[size],
        ...(props.disabled && disabledStyle),
      }}
      {...props}
    >
      {children}
    </button>
  );
}
