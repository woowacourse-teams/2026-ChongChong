import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import BottomTab from '../../../shared/ui/components/BottomTab';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import { useNavigate, useParams } from 'react-router';
import { useRef } from 'react';
import NoticeDetailActions from '../../notice/components/NoticeDetailActions';
import Main from '../../../shared/ui/Main';
import assignmentQueries from '../queries';
import { useSuspenseQueries } from '@tanstack/react-query';
import SubmitStatusSection from '../components/SubmitStatusSection';
import AssignmentArticle from '../components/AssignmentArticle';

export default function LeaderAssignmentDetailpage() {
  const navigate = useNavigate();
  const { studyId, assignmentId } = useParams();
  const deleteDialogRef = useRef<HTMLDialogElement>(null);

  const [{ data: assignment }, { data: submitStatusResponse }] = useSuspenseQueries({
    queries: [
      assignmentQueries.detail(Number(studyId), Number(assignmentId)),
      assignmentQueries.submitStatus(Number(studyId), Number(assignmentId)),
    ],
  });

  const openDeleteDialog = () => deleteDialogRef.current?.showModal();
  const closeDeleteDialog = () => deleteDialogRef.current?.close();
  const editAssignment = () => navigate(`/studies/${studyId}/assignments/${assignmentId}/edit`);

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />

      <Main>
        <SubmitStatusSection status={submitStatusResponse} />
        <AssignmentArticle assignment={assignment} />

        <NoticeDetailActions onEdit={editAssignment} onDelete={openDeleteDialog} />
      </Main>

      <ConfirmDialog
        ref={deleteDialogRef}
        title="과제를 삭제할까요?"
        description={'삭제한 과제는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'}
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
