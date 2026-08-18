import type { CSSProperties } from 'react';
import { useRef } from 'react';
import { useNavigate, useParams } from 'react-router';
import backIcon from '../../shared/assets/left-arrow.svg';
import ConfirmDialog from '../../shared/ui/dialogs/ConfirmDialog';
import TopHeader from '../../shared/ui/TopHeader';
import { tokens } from '../../styles/global';
import NoticeArticle from './components/NoticeArticle';
import NoticeDetailActions from './components/NoticeDetailActions';
import NoticeReadStatus from './components/NoticeReadStatus';

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
  padding: `${tokens.spacing[5]} ${tokens.layout.gutter} calc(${tokens.spacing[8]} + ${tokens.layout.safeBottom})`,
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

const notice = {
  title: '8월 스터디 운영 방식이 바뀝니다',
  author: '바니',
  createdAt: '5시간 전',
  content: `8월부터 스터디 운영 방식을 조금 바꾸려고 합니다. 끝까지 읽고 읽음 버튼을 눌러주세요.

1. 모임 시간
매주 화요일 저녁 9시로 고정합니다. 기존에는 요일을 매주 투표로 정했는데, 일정이 계속 밀리는 문제가 있었습니다. 8월 첫째 주부터 적용합니다람쥐가 노래를한다

2. 발표 순서
발표 순서는 다음과 같습니다. 매주 월요일 랜덤으로 순서를 공지합니다 이런 느낌으로 스크롤을 쭈욱하게 해주면됩니다`,
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
  const deleteDialogRef = useRef<HTMLDialogElement>(null);

  const openDeleteDialog = () => deleteDialogRef.current?.showModal();
  const closeDeleteDialog = () => deleteDialogRef.current?.close();

  const editNotice = () => {
    navigate(`/studies/${studyId}/notices/${noticeId}/modify`);
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

      <main css={contentStyle}>
        <NoticeReadStatus
          readMemberNames={notice.readMemberNames}
          unreadMembers={notice.unreadMembers}
          totalCount={notice.totalCount}
          reminderText={notice.reminderText}
        />

        <NoticeArticle
          title={notice.title}
          author={notice.author}
          createdAt={notice.createdAt}
          content={notice.content}
        />

        <NoticeDetailActions onEdit={editNotice} onDelete={openDeleteDialog} />
      </main>

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
    </div>
  );
}
