import { ComponentProps, ReactNode } from 'react';
import { Link } from 'react-router';
import { CSSObject } from '@emotion/react';
import { tokens } from '../../../styles/global';
import Button from '../../ui/Button';
import FloatingPanel from './FloatingPanel';
import { Text } from '../../ui/Typography';

type FloatingMenuProps = Omit<ComponentProps<typeof FloatingPanel>, 'children' | 'closeButton'> & {
  children: ReactNode;
};

export default function FloatingMenu({ children, ...props }: FloatingMenuProps) {
  return (
    <FloatingPanel
      {...props}
      closeButton={({ close }) => {
        return <FloatingMenu.CloseButton close={close} />;
      }}
    >
      <div css={{ display: 'flex', flexDirection: 'column', gap: tokens.spacing[1] }}>
        {children}
      </div>
    </FloatingPanel>
  );
}

interface FloatingMenuItemProps extends Omit<ComponentProps<typeof Link>, 'children'> {
  icon: ReactNode;
  label: string;
  description?: string;
}

const floatingMenuItemIconStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  flexShrink: 0,
  width: '40px',
  height: '40px',
  borderRadius: tokens.radius.md,
  background: tokens.bg.brand,
};

const floatingMenuItemStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[3],
  width: '100%',
  minHeight: '72px',
  padding: 0,
  borderRadius: tokens.radius.md,
  color: tokens.text.default,
  textAlign: 'left',
  cursor: 'pointer',
  '&:focus': {
    outline: 'none',
  },
  '&:hover': { background: tokens.bg.subtle },
} satisfies CSSObject;

FloatingMenu.LinkItem = function LinkItem({
  icon,
  label,
  description,
  ...props
}: FloatingMenuItemProps) {
  return (
    <Link css={floatingMenuItemStyle} {...props}>
      <span aria-hidden="true" css={floatingMenuItemIconStyle}>
        {icon}
      </span>
      <div css={{ display: 'flex', flexDirection: 'column', gap: tokens.spacing[1] }}>
        <Text variant="subtitle" css={{ fontWeight: tokens.fontWeight.semibold }}>
          {label}
        </Text>
        {description && (
          <Text variant="label" css={{ color: tokens.text.muted }}>
            {description}
          </Text>
        )}
      </div>
    </Link>
  );
};

interface FloatingMenuCloseButtonProps {
  close: () => void;
}

const FloatingMenuCloseButtonStyle = {
  width: '100%',
  padding: 0,
  borderRadius: tokens.radius.md,
  '&:focus-visible': {
    outline: `2px solid ${tokens.text.default}`,
    outlineOffset: '-4px',
  },
} satisfies CSSObject;

FloatingMenu.CloseButton = function CloseButton({ close }: FloatingMenuCloseButtonProps) {
  return (
    <Button variant="brandSolid" size="large" onClick={close} css={FloatingMenuCloseButtonStyle}>
      <span>닫기</span>
    </Button>
  );
};
