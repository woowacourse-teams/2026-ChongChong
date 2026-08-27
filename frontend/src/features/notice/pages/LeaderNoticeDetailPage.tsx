import type { CSSProperties } from 'react';
import { useRef, Suspense } from 'react';
import { useNavigate, useParams } from 'react-router';
import backIcon from '../../../shared/assets/left-arrow.svg';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import Main from '../../../shared/ui/Main';
import TopHeader from '../../../shared/ui/TopHeader';
import NoticeArticle from '../components/NoticeArticle';
import DetailActions from '../../../shared/ui/components/DetailActions';
import NoticeReadStatus from '../components/NoticeReadStatus';
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

export default function LeaderNoticeDetailPage() {
  const navigate = useNavigate();
  const { studyId, noticeId } = useParams();
  const deleteDialogRef = useRef<HTMLDialogElement>(null);

  const openDeleteDialog = () => deleteDialogRef.current?.showModal();
  const closeDeleteDialog = () => deleteDialogRef.current?.close();
  const editNotice = () => navigate(`/studies/${studyId}/notices/${noticeId}/edit`);

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
        <Main>
          <NoticeReadStatus
            readuserNames={notice.readuserNames}
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

          <DetailActions onEdit={editNotice} onDelete={openDeleteDialog} />
        </Main>
      </Suspense>

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
      <BottomTab />
    </Page>
  );
}
