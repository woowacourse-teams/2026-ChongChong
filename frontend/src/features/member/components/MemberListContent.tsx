import { CSSProperties } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import { tokens, typography } from '../../../styles/global';
import Button from '../../../shared/ui/Button';
import ErrorContent from '../../../shared/ui/ErrorContent';
import MemberList from './MemberList';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import InviteStudyLinkBox, { InviteStudyLinkBoxFallback } from './InviteStudyLinkBox';
import useDeleteStudy from '../../study/hooks/useDeleteStudy';
import useLeaveStudyMember from '../hooks/useLeaveStudyMember';
import useBooleanState from '../../../shared/hooks/useBooleanState';

const actionButtonStyle = {
  margin: `${tokens.spacing[5]} 0`,
} satisfies CSSProperties;

function LeaderContent() {
  const { studyId } = useIntegerParams(['studyId']);

  const [isOpen, openDialog, closeDialog] = useBooleanState();

  const { mutate: deleteStudy, isPending } = useDeleteStudy({ onError: closeDialog });

  return (
    <>
      <section>
        <h2 css={typography.subtitle}>스터디 멤버</h2>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <MemberList.Leader />
        </ErrorBoundary>
        <ErrorBoundary
          fallbackRender={({ error }) => (
            <InviteStudyLinkBoxFallback message={getErrorMessage(error)} />
          )}
        >
          <InviteStudyLinkBox studyId={studyId} />
        </ErrorBoundary>
      </section>
      <Button variant="criticalSolid" size="large" css={actionButtonStyle} onClick={openDialog}>
        스터디 삭제하기
      </Button>
      {isOpen && (
        <ConfirmDialog
          title={'스터디를 삭제할까요?'}
          description={'삭제한 스터디는 다시 복구할 수 없어요. 정말 삭제하시겠어요?'}
          onClose={closeDialog}
          closeButton={
            <ConfirmDialog.CloseButton onClick={closeDialog}>취소</ConfirmDialog.CloseButton>
          }
          confirmButton={
            <ConfirmDialog.ConfirmButton
              onClick={() => deleteStudy({ studyId })}
              disabled={isPending}
            >
              삭제
            </ConfirmDialog.ConfirmButton>
          }
        />
      )}
    </>
  );
}

function MemberContent() {
  const { studyId } = useIntegerParams(['studyId']);

  const [isOpen, openDialog, closeDialog] = useBooleanState();

  const { mutate: leaveStudyMember, isPending } = useLeaveStudyMember({ onError: closeDialog });

  return (
    <>
      <section>
        <h2 css={typography.subtitle}>스터디 멤버</h2>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <MemberList.Member />
        </ErrorBoundary>
        <ErrorBoundary
          fallbackRender={({ error }) => (
            <InviteStudyLinkBoxFallback message={getErrorMessage(error)} />
          )}
        >
          <InviteStudyLinkBox studyId={studyId} />
        </ErrorBoundary>
      </section>
      <Button variant="criticalSolid" size="large" css={actionButtonStyle} onClick={openDialog}>
        스터디 탈퇴하기
      </Button>
      {isOpen && (
        <ConfirmDialog
          title={'스터디를 탈퇴하시겠습니까?'}
          description={'스터디를 탈퇴하면 이전 스터디 활동 기록이 전부 사라져요'}
          onClose={closeDialog}
          closeButton={
            <ConfirmDialog.CloseButton onClick={closeDialog}>취소</ConfirmDialog.CloseButton>
          }
          confirmButton={
            <ConfirmDialog.ConfirmButton
              onClick={() => leaveStudyMember({ studyId })}
              disabled={isPending}
            >
              탈퇴
            </ConfirmDialog.ConfirmButton>
          }
        />
      )}
    </>
  );
}

const MemberListContent = {
  Leader: LeaderContent,
  Member: MemberContent,
};

export default MemberListContent;
