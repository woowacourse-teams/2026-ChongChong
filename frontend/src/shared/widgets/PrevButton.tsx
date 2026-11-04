import { CSSProperties } from 'react';
import usePageNavigation from '../hooks/usePageNavigation';
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
  const { back } = usePageNavigation();

  return (
    <button type="button" css={backButtonStyle} aria-label="뒤로 가기" onClick={back}>
      <img src={backIcon} alt="" css={{ width: '24px', height: '24px' }} />
    </button>
  );
}
