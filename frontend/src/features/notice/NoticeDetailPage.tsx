import type { CSSProperties, UIEvent } from 'react';
import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import backIcon from '../../shared/assets/left-arrow.svg';
import ConfirmDialog from '../../shared/ui/dialogs/ConfirmDialog';
import TopHeader from '../../shared/ui/TopHeader';
import { tokens } from '../../styles/global';
import MemberNoticeReadState from './components/MemberNoticeReadState';
import NoticeArticle from './components/NoticeArticle';
import NoticeDetailActions from './components/NoticeDetailActions';
import NoticeReadStatus from './components/NoticeReadStatus';
import Main from '../../shared/ui/Main';

const pageStyle = {
  display: 'flex',
  height: '100dvh',
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
  padding: `${tokens.spacing[5]} ${tokens.layout.gutter} calc(${tokens.spacing[8]} + ${tokens.layout.safeBottom})`,
  flex: 1,
  flexDirection: 'column',
  overflowY: 'auto',
} satisfies CSSProperties;

const memberContentStyle = {
  ...contentStyle,
  paddingBottom: tokens.spacing[8],
} satisfies CSSProperties;

const notice = {
  title: '8월 스터디 운영 방식이 바뀝니다',
  author: '바니',
  createdAt: '5시간 전',
  content: `8월부터 스터디 운영 방식을 조금 바꾸려고 합니다. 끝까지 읽고 읽음 버튼을 눌러주세요.

1. 모임 시간
매주 화요일 저녁 9시로 고정합니다. 기존에는 요일을 매주 투표로 정했는데, 일정이 계속 밀리는 문제가 있었습니다. 8월 첫째 주부터 적용합니다.

2. 발표 순서
발표 순서는 다음과 같습니다. 매주 월요일 랜덤으로 순서를 공지합니다. 발표 자료는 모임 하루 전까지 공유해주세요.

3. 코드 리뷰
발표가 없는 주에도 서로의 코드를 한 번씩 확인합니다. 리뷰할 저장소와 범위는 스터디 채널에 남겨주세요. 리뷰는 정답을 알려주기보다 궁금한 점과 다른 선택지를 함께 적어주시면 좋겠습니다.

4. 불참 안내
참석이 어려운 경우 모임 시작 전까지 알려주세요. 미리 공유해주시면 발표 순서를 다음 주로 조정하겠습니다.

운영 방식은 한 달 동안 적용한 뒤 회고에서 다시 이야기해보겠습니다. 불편한 점이나 더 좋은 방법이 있다면 언제든 스터디 채널에 남겨주세요.

긴 글 읽어주셔서 감사합니다. 다음 모임에서 만나요!`,
  readMemberNames: ['디움', '피즈'],
  unreadMembers: [
    { id: 1, name: '안톨리니', remindedAt: '8월 3일 21:02 보냄' },
    { id: 2, name: '이든', remindedAt: '8월 3일 21:02 보냄' },
  ],
  totalCount: 4,
  reminderText: '1분 뒤 리마인드 · 8월 5일 21:00',
};

export default function NoticeDetailPage() {
  const navigate = useNavigate();
  const { studyId, noticeId } = useParams();
  const [searchParams] = useSearchParams();
  const deleteDialogRef = useRef<HTMLDialogElement>(null);
  const contentRef = useRef<HTMLElement>(null);
  const isLeader = searchParams.get('role') === 'leader';
  const startsCompleted = searchParams.get('progress') === '100';
  const [readProgress, setReadProgress] = useState(startsCompleted ? 100 : 38);

  useEffect(() => {
    const content = contentRef.current;
    if (!isLeader && content && content.scrollHeight <= content.clientHeight) {
      setReadProgress(100);
    }
  }, [isLeader]);

  const openDeleteDialog = () => deleteDialogRef.current?.showModal();
  const closeDeleteDialog = () => deleteDialogRef.current?.close();

  const editNotice = () => {
    navigate(`/studies/${studyId}/notices/${noticeId}/edit`);
  };

  const updateReadProgress = (event: UIEvent<HTMLElement>) => {
    const { scrollTop, scrollHeight, clientHeight } = event.currentTarget;
    const scrollableHeight = scrollHeight - clientHeight;
    const progress = scrollableHeight <= 0 ? 100 : Math.round((scrollTop / scrollableHeight) * 100);

    setReadProgress((current) => Math.max(current, progress));
  };

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

      <Main
        ref={contentRef}
        css={isLeader ? contentStyle : memberContentStyle}
        onScroll={isLeader ? undefined : updateReadProgress}
      >
        {isLeader && (
          <NoticeReadStatus
            readMemberNames={notice.readMemberNames}
            unreadMembers={notice.unreadMembers}
            totalCount={notice.totalCount}
            reminderText={notice.reminderText}
          />
        )}

        <NoticeArticle
          title={notice.title}
          author={notice.author}
          createdAt={notice.createdAt}
          content={notice.content}
          hasTopMargin={isLeader}
        />

        {isLeader && <NoticeDetailActions onEdit={editNotice} onDelete={openDeleteDialog} />}
      </Main>

      {!isLeader && <MemberNoticeReadState progress={readProgress} readAt="8월 3일 21:14" />}

      {isLeader && (
        <ConfirmDialog
          ref={deleteDialogRef}
          title="공지를 삭제할까요?"
          description={'삭제한 공지는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'}
          closeButton={
            <ConfirmDialog.CloseButton onClick={closeDeleteDialog}>취소</ConfirmDialog.CloseButton>
          }
          confirmButton={
            <ConfirmDialog.ConfirmButton onClick={closeDeleteDialog}>
              삭제
            </ConfirmDialog.ConfirmButton>
          }
        />
      )}
    </div>
  );
}
