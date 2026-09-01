import { CSSProperties } from 'react';
import { useNavigate, useLocation } from 'react-router';
import backIcon from '../assets/left-arrow.svg';

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

export function PrevButton() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const parentPath = pathname.substring(0, pathname.lastIndexOf('/'));

  function goToPreviousPage() {
    // fallback 필요
    navigate(parentPath);
  }

  return (
    <button type="button" css={backButtonStyle} aria-label="뒤로 가기" onClick={goToPreviousPage}>
      <img src={backIcon} alt="" css={{ width: '24px', height: '24px' }} />
    </button>
  );
}
