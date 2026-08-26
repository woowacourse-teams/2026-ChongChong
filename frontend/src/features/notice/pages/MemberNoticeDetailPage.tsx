import type { CSSProperties, UIEvent } from 'react';
import { useEffect, useRef, useState, Suspense } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import backIcon from '../../../shared/assets/left-arrow.svg';
import Main from '../../../shared/ui/Main';
import TopHeader from '../../../shared/ui/TopHeader';
import { tokens } from '../../../styles/global';
import MemberNoticeReadState from '../components/MemberNoticeReadState';
import NoticeArticle from '../components/NoticeArticle';
import { notice } from '../noticeData';
import Page from '../../../shared/ui/Page';
import BottomTab from '../../../shared/ui/components/BottomTab';
import Loading from '../../../shared/ui/Loading';

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
  minHeight: 0,
  margin: '0 auto',
  padding: `${tokens.spacing[5]} ${tokens.layout.gutter} ${tokens.spacing[8]}`,
  flex: 1,
  flexDirection: 'column',
  overflowY: 'auto',
} satisfies CSSProperties;

export default function MemberNoticeDetailPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const contentRef = useRef<HTMLElement>(null);
  const startsCompleted = searchParams.get('progress') === '100';
  const [readProgress, setReadProgress] = useState(startsCompleted ? 100 : 0);

  useEffect(() => {
    const content = contentRef.current;
    if (content && content.scrollHeight <= content.clientHeight) {
      setReadProgress(100);
    }
  }, []);

  const updateReadProgress = (event: UIEvent<HTMLElement>) => {
    const { scrollTop, scrollHeight, clientHeight } = event.currentTarget;
    const scrollableHeight = scrollHeight - clientHeight;
    const progress = scrollableHeight <= 0 ? 100 : Math.round((scrollTop / scrollableHeight) * 100);

    setReadProgress((current) => Math.max(current, progress));
  };

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
      <Suspense fallback={<Loading />}>
        <Main ref={contentRef} css={contentStyle} onScroll={updateReadProgress}>
          <NoticeArticle
            title={notice.title}
            author={notice.author}
            createdAt={notice.createdAt}
            content={notice.content}
            hasTopMargin={false}
          />
        </Main>
      </Suspense>

      <MemberNoticeReadState progress={readProgress} readAt="8월 3일 21:14" />
      <BottomTab />
    </Page>
  );
}
