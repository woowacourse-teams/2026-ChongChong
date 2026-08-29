import { CSSProperties } from 'react';
import { useNavigate } from 'react-router';
import { useSuspenseQueries } from '@tanstack/react-query';
import useStudyId from '../../study/hooks/useStudyId';
import { memberQueries } from '../queries';
import studyQueries from '../../study/queries';
import { tokens, typography } from '../../../styles/global';
import Button from '../../../shared/ui/Button';
import List from '../../../shared/ui/List';
import MemberRow from './MemberRow';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import InviteLinkBox from './InviteLinkBox';
import useDialogControl from '../../../shared/hooks/useDialogControl';
import useDeleteStudy from '../../study/hooks/useDeleteStudy';
import useKickStudyMember from '../hooks/useKickMember';
import useLeaveStudyMember from '../hooks/useLeaveStudyMember';

const listStyle = {
  marginBottom: tokens.spacing[6],
} satisfies CSSProperties;

const actionButtonStyle = {
  margin: `${tokens.spacing[5]} 0`,
} satisfies CSSProperties;

function LeaderContent() {
  const { studyId } = useStudyId();
  const navigate = useNavigate();

  const [
    {
      data: { members },
    },
    {
      data: { inviteLink },
    },
  ] = useSuspenseQueries({
    queries: [memberQueries.list(studyId), studyQueries.inviteLink(studyId)],
  });

  const { mutate: kickStudyMember } = useKickStudyMember();

  const { dialogRef, open, close } = useDialogControl();

  const { mutate: deleteStudy, isPending } = useDeleteStudy();

  function handleDeleteStudy() {
    deleteStudy(
      { studyId },
      {
        onSuccess: () => navigate('/studies'),
      },
    );
  }

  return (
    <>
      <section>
        <h2 css={typography.subtitle}>스터디 멤버</h2>
        <List css={listStyle}>
          {members.map((member) => (
            <List.Item key={member.id}>
              <MemberRow.Leader
                data-testid="member-row"
                name={member.name}
                role={member.role}
                onKick={() => kickStudyMember({ studyId, memberId: member.id })}
              />
            </List.Item>
          ))}
        </List>
        <InviteLinkBox title={'링크를 통해 새로운 스터디원을 초대해요'} inviteLink={inviteLink} />
      </section>
      <Button variant="criticalSolid" size="large" css={actionButtonStyle} onClick={open}>
        스터디 삭제하기
      </Button>
      <ConfirmDialog
        ref={dialogRef}
        title={'스터디를 삭제할까요?'}
        description={'삭제한 스터디는 다시 복구할 수 없어요. 정말 삭제하시겠어요?'}
        closeButton={<ConfirmDialog.CloseButton onClick={close}>취소</ConfirmDialog.CloseButton>}
        confirmButton={
          <ConfirmDialog.ConfirmButton onClick={handleDeleteStudy} disabled={isPending}>
            삭제
          </ConfirmDialog.ConfirmButton>
        }
      />
    </>
  );
}

function MemberContent() {
  const { studyId } = useStudyId();

  const [
    {
      data: { members },
    },
    {
      data: { inviteLink },
    },
  ] = useSuspenseQueries({
    queries: [memberQueries.list(studyId), studyQueries.inviteLink(studyId)],
  });

  const navigate = useNavigate();

  const { mutate: leaveStudyMember } = useLeaveStudyMember();

  function handleLeaveStudyMember() {
    leaveStudyMember({ studyId });
    navigate('/studies');
  }

  return (
    <>
      <section>
        <h2 css={typography.subtitle}>스터디 멤버</h2>
        <List css={listStyle}>
          {members.map((member) => (
            <List.Item key={member.id}>
              <MemberRow.Member data-testid="member-row" name={member.name} role={member.role} />
            </List.Item>
          ))}
        </List>
        <InviteLinkBox title={'링크를 통해 새로운 스터디원을 초대해요'} inviteLink={inviteLink} />
      </section>
      <Button
        variant="criticalSolid"
        size="large"
        css={actionButtonStyle}
        onClick={handleLeaveStudyMember}
      >
        스터디 탈퇴하기
      </Button>
    </>
  );
}

const MemberListContent = {
  Leader: LeaderContent,
  Member: MemberContent,
};

export default MemberListContent;
