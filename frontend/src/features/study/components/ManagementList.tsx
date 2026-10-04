import { CSSProperties } from 'react';
import { Link } from 'react-router';
import List from '../../../shared/ui/List';
import { tokens, typography } from '../../../styles/global';
import useBooleanState from '../../../shared/hooks/useBooleanState';
import useDeleteStudy from '../hooks/useDeleteStudy';
import useLeaveStudy from '../hooks/useLeaveStudy';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';

const StudyManagementItemStyle = {
  ...typography.title,
  display: 'block',
  padding: `${tokens.spacing[5]} 0`,
  borderBottom: tokens.border.neutral,
  cursor: 'pointer',
  textAlign: 'left',
  width: '100%',
} satisfies CSSProperties;

interface StudyLeaderManagementListProps {
  studyId: number;
}

interface StudyMemberManagementListProps {
  studyId: number;
}

export function StudyLeaderManagementList({ studyId }: StudyLeaderManagementListProps) {
  const [isOpen, openDialog, closeDialog] = useBooleanState();

  const { mutate: deleteStudy, isPending } = useDeleteStudy({ onError: closeDialog });
  return (
    <List>
      {/* <List.Item css={StudyManagementItemStyle}>스터디 프로필 수정 (준비 중)</List.Item> */}
      {/* TODO: 스터디 프로필 수정 페이지가 추가되면 링크를 연결합니다. */}
      <List.Item>
        <Link css={StudyManagementItemStyle} to={`/studies/${studyId}/edit`}>
          스터디 정보 수정
        </Link>
      </List.Item>
      <List.Item>
        <button
          css={{ ...StudyManagementItemStyle, color: tokens.text.critical }}
          onClick={openDialog}
        >
          스터디 삭제하기
        </button>
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
      </List.Item>
    </List>
  );
}

export function StudyMemberManagementList({ studyId }: StudyMemberManagementListProps) {
  const [isOpen, openDialog, closeDialog] = useBooleanState();

  const { mutate: leaveStudyMember, isPending } = useLeaveStudy({ onError: closeDialog });
  return (
    <List>
      {/* TODO: 스터디 탈퇴를 멤버 페이지에서 옮깁니다. */}
      {/* <List.Item css={StudyManagementItemStyle}>
        <Link to="">스터디 탈퇴</Link>
      </List.Item> */}
      <button css={{ color: tokens.text.critical }} onClick={openDialog}>
        스터디 탈퇴하기
      </button>

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
    </List>
  );
}
