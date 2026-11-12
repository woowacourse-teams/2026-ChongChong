import { useId, ComponentProps, ReactNode } from 'react';
import type { CSSObject } from '@emotion/react';
import BottomSheet from '../ui/BottomSheet';
import { Heading } from '../ui/Typography';
import { tokens } from '../../styles/global';

type Actions = {
  onSelect: () => void;
  onClose: () => void;
  onReset: () => void;
};

interface ProfileImageActionSheet {
  open: boolean;
  hasImage: boolean;
  actions: Actions;
}

interface ActionButtonProps extends ComponentProps<'button'> {
  onClick: () => void;
  children: ReactNode;
}

const HeaderStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  height: 40,
  margin: 0,
} satisfies CSSObject;

const ActionStyle = {
  display: 'block',
  width: '100%',
  height: 56,
  padding: '0 24px',
  border: 0,
  background: tokens.bg.default,
  color: tokens.text.default,
  font: 'inherit',
  fontSize: 16,
  textAlign: 'left',
  cursor: 'pointer',
  '&:hover': { background: tokens.bg.brandSubtle },
  '&:focus-visible': { outline: `2px solid ${tokens.bg.brand}`, outlineOffset: -2 },
} satisfies CSSObject;

function ActionButton({ onClick, children, ...props }: ActionButtonProps) {
  return (
    <button type="button" css={ActionStyle} onClick={onClick} {...props}>
      {children}
    </button>
  );
}

export default function ProfileImageActionSheet({
  open,
  hasImage,
  actions,
}: ProfileImageActionSheet) {
  const titleId = useId();

  return (
    <BottomSheet open={open} aria-labelledby={titleId} onClose={actions.onClose}>
      <header css={HeaderStyle}>
        <Heading id={titleId} level={2} variant="title">
          프로필 이미지
        </Heading>
      </header>
      <ActionButton onClick={actions.onSelect}>프로필 이미지 선택</ActionButton>
      {hasImage && <ActionButton onClick={actions.onReset}>기본 프로필 이미지 사용</ActionButton>}
      <ActionButton onClick={actions.onClose}>취소</ActionButton>
    </BottomSheet>
  );
}
