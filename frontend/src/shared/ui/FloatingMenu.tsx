import { ComponentProps, ComponentPropsWithRef, ElementType, ReactNode } from 'react';
import { CSSObject } from '@emotion/react';
import { tokens } from '../../styles/global';
import Button from './Button';
import FloatingPanel from './FloatingPanel';
import { FLOATING_SIZE, floatingMotion } from './floatingStyles';
import { Text } from './Typography';

type FloatingMenuProps = Omit<ComponentProps<typeof FloatingPanel>, 'children' | 'closeButton'> & {
  children: ReactNode;
};

type ItemProps<T extends ElementType> = {
  as?: T;
  icon: ReactNode;
  label: string;
  description?: string;
} & Omit<ComponentPropsWithRef<T>, 'as' | 'icon' | 'label' | 'description' | 'children'>;

const itemStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[3],
  width: '100%',
  minHeight: '72px',
  padding: 0,
  border: 0,
  borderRadius: tokens.radius.md,
  background: 'transparent',
  color: tokens.text.default,
  font: 'inherit',
  textAlign: 'left',
  textDecoration: 'none',
  cursor: 'pointer',
  '&:hover': { background: tokens.bg.subtle },
  '&:focus-visible': { outline: `2px solid ${tokens.text.brand}`, outlineOffset: '2px' },
} satisfies CSSObject;

/** 메뉴 표현을 공유하되 항목의 동작과 이동 경로는 외부에서 조합한다. */
export default function FloatingMenu({ children, ...props }: FloatingMenuProps) {
  return (
    <FloatingPanel
      {...props}
      closeButton={({ close, expanded }) => {
        const motion = floatingMotion[expanded ? 'open' : 'close'];
        return (
          <Button
            variant="brandSolid"
            size="large"
            onClick={close}
            css={{
              width: '100%',
              // 접힌 윤곽은 원래 Floating과 맞추고 펼쳐진 닫기 버튼만 낮춘다.
              height: expanded ? 48 : FLOATING_SIZE,
              padding: 0,
              borderRadius: tokens.radius.md,
              transition: `height ${motion.durationMs}ms ${motion.easing}`,
              '@media (prefers-reduced-motion: reduce)': { transition: 'none' },
              '&:focus-visible': {
                outline: `2px solid ${tokens.text.default}`,
                outlineOffset: '-4px',
              },
            }}
          >
            <span
              css={{
                opacity: expanded ? 1 : 0,
                transition: `opacity ${expanded ? 160 : 90}ms ease ${expanded ? 65 : 0}ms`,
                '@media (prefers-reduced-motion: reduce)': { transition: 'none' },
              }}
            >
              닫기
            </span>
          </Button>
        );
      }}
    >
      <div css={{ display: 'flex', flexDirection: 'column', gap: tokens.spacing[1] }}>
        {children}
      </div>
    </FloatingPanel>
  );
}

FloatingMenu.Item = function Item<T extends ElementType = 'button'>({
  as,
  icon,
  label,
  description,
  ...props
}: ItemProps<T>) {
  const Component = as ?? 'button';

  return (
    <Component {...(Component === 'button' ? { type: 'button' } : {})} css={itemStyle} {...props}>
      <span
        aria-hidden="true"
        css={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
          width: '40px',
          height: '40px',
          borderRadius: tokens.radius.md,
          background: tokens.bg.brand,
        }}
      >
        {icon}
      </span>
      <span css={{ display: 'flex', flexDirection: 'column', gap: tokens.spacing[1] }}>
        <Text variant="subtitle" css={{ fontWeight: tokens.fontWeight.semibold }}>
          {label}
        </Text>
        {description && (
          <Text variant="label" css={{ color: tokens.text.muted }}>
            {description}
          </Text>
        )}
      </span>
    </Component>
  );
};
