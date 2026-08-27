import { CSSProperties } from 'react';
import loadingImage from '../assets/icons/loading-running-rabbit.webp';

const divStyle = {
  display: 'flex',
  flex: 1,
  width: '100%',
  minHeight: 0,
  flexDirection: 'column',
  justifyContent: 'center',
  alignItems: 'center',
} satisfies CSSProperties;

export default function Loading() {
  return (
    <div css={divStyle}>
      <img src={loadingImage} alt="로딩 중" width={150} height={150} />
    </div>
  );
}
