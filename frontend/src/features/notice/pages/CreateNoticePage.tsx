import type { CSSProperties } from 'react';
import { useNavigate } from 'react-router';
import backIcon from '../../../shared/assets/left-arrow.svg';
import TopHeader from '../../../shared/ui/TopHeader';
import NoticeForm from '../components/NoticeForm';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';

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

export default function CreateNoticePage() {
  const navigate = useNavigate();

  return (
    <Page>
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
      <Main>
        <NoticeForm submitLabel="공지 올리기" />
      </Main>
    </Page>
  );
}
