import type { CSSProperties } from 'react';
import { useNavigate } from 'react-router';
import backIcon from '../../shared/assets/left-arrow.svg';
import TopHeader from '../../shared/ui/TopHeader';
import { tokens } from '../../styles/global';
import NoticeForm from './components/NoticeForm';

const pageStyle = {
  display: 'flex',
  minHeight: '100dvh',
  flexDirection: 'column',
  background: tokens.bg.default,
} satisfies CSSProperties;

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

const contentStyle = {
  display: 'flex',
  width: '100%',
  margin: '0 auto',
  padding: `${tokens.spacing[4]} ${tokens.layout.gutter} calc(${tokens.spacing[8]} + ${tokens.layout.safeBottom})`,
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

export default function CreateNoticePage() {
  const navigate = useNavigate();

  return (
    <div css={pageStyle}>
      <TopHeader
        left={
          <button
            type="button"
            css={backButtonStyle}
            aria-label="뒤로 가기"
            onClick={() => navigate(-1)}
          >
            <img src={backIcon} alt="뒤로 가기" width={24} height={24} />
          </button>
        }
        middle={<TopHeader.Title>공지</TopHeader.Title>}
      />
      <main css={contentStyle}>
        <NoticeForm submitLabel="공지 올리기" />
      </main>
    </div>
  );
}
