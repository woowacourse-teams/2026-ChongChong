import { CSSProperties } from 'react';
import { useNavigate, useLocation } from 'react-router';
import backIcon from '../assets/left-arrow.svg';
import { parseParentPath } from '../utils/parseParentPath';

const backButtonStyle = {
  display: 'grid',
  width: '32px',
  height: '32px',
  padding: 0,
  placeItems: 'center',
  border: 0,
  background: 'transparent',
  cursor: 'pointer',
} satisfies CSSProperties;

interface PrevButtonProps {
  iconSrc?: string;
  className?: string;
  to?: string;
}

export function PrevButton({ iconSrc = backIcon, className, to }: PrevButtonProps = {}) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const parentPath = parseParentPath(pathname);

  function goToPreviousPage() {
    // fallback 필요
    navigate(to ?? parentPath);
  }

  return (
    <button
      type="button"
      css={backButtonStyle}
      className={className}
      aria-label="뒤로 가기"
      onClick={goToPreviousPage}
    >
      <img src={iconSrc} alt="" css={{ width: '24px', height: '24px' }} />
    </button>
  );
}
