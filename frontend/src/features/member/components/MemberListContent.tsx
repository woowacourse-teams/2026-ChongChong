import { useRef, CSSProperties } from 'react';
import { useNavigate } from 'react-router';
import { useMutation, useSuspenseQuery, useQueryClient } from '@tanstack/react-query';
import useStudyId from '../../studies/hooks/useStudyId';
import { memberQueries } from '../queries';
import studyQueries from '../../studies/queries';
import { tokens, typography } from '../../../styles/global';
import Button from '../../../shared/ui/Button';
import List from '../../../shared/ui/List';
import MemberRow from './MemberRow';
import { kickMember, leaveStudyMember } from '../api';
import { removeStudy } from '../../studies/api';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import InviteLinkBox from './InviteLinkBox';

const listStyle = {
  marginBottom: tokens.spacing[6],
} satisfies CSSProperties;

const actionButtonStyle = {
  margin: `${tokens.spacing[5]} 0`,
} satisfies CSSProperties;

function LeaderContent() {
  const { studyId } = useStudyId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const {
    data: { inviteLink },
  } = useSuspenseQuery(studyQueries.inviteLink(studyId));
  const { data: members } = useSuspenseQuery({
    ...memberQueries.list(studyId),
    select: (data) => data.members,
  });

  const deleteMember = useMutation({
    mutationFn: ({ studyId, memberId }: { studyId: number; memberId: number }) =>
      kickMember({ studyId, memberId }),
    onSettled: (_data, _error, variables) =>
      queryClient.invalidateQueries({ queryKey: memberQueries.lists(variables.studyId) }),
  });

  const removeStudyDialogRef = useRef<HTMLDialogElement>(null);

  const handleOpenDialog = () => {
    removeStudyDialogRef.current?.showModal();
  };

  const handleCloseDialog = () => {
    removeStudyDialogRef.current?.close();
  };

  const deleteStudy = useMutation({
    mutationFn: ({ studyId }: { studyId: number }) => removeStudy(studyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studyQueries.lists() });
      navigate('/studies');
    },
  });

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
                onKick={() => deleteMember.mutate({ studyId, memberId: member.id })}
              />
            </List.Item>
          ))}
        </List>
        <InviteLinkBox title={'링크를 통해 새로운 스터디원을 초대해요'} inviteLink={inviteLink} />
      </section>
      <Button
        variant="criticalSolid"
        size="large"
        css={actionButtonStyle}
        onClick={handleOpenDialog}
      >
        스터디 삭제하기
      </Button>
      <ConfirmDialog
        ref={removeStudyDialogRef}
        title={'스터디를 삭제할까요?'}
        description={'삭제한 스터디는 다시 복구할 수 없어요. 정말 삭제하시겠어요?'}
        closeButton={
          <ConfirmDialog.CloseButton onClick={handleCloseDialog}>취소</ConfirmDialog.CloseButton>
        }
        confirmButton={
          <ConfirmDialog.ConfirmButton onClick={() => deleteStudy.mutate({ studyId })}>
            삭제
          </ConfirmDialog.ConfirmButton>
        }
      />
    </>
  );
}

function MemberContent() {
  const { studyId } = useStudyId();
  const queryClient = useQueryClient();
  const {
    data: { inviteLink },
  } = useSuspenseQuery(studyQueries.inviteLink(studyId));
  const { data: members } = useSuspenseQuery({
    ...memberQueries.list(Number(studyId)),
    select: (data) => data.members,
  });

  const navigate = useNavigate();

  const leaveStudy = useMutation({
    mutationFn: ({ studyId }: { studyId: number }) => leaveStudyMember({ studyId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studyQueries.lists() });
      navigate('/studies');
    },
  });

  return (
    <>
      <section>
        <h2 css={typography.subtitle}>스터디 멤버</h2>
        <List css={listStyle}>
          {members.map((member) => (
            <List.Item key={member.id}>
              <MemberRow.Member
                data-testid="member-row"
                name={member.name}
                role={member.role}
              />
            </List.Item>
          ))}
        </List>
        <InviteLinkBox title={'링크를 통해 새로운 스터디원을 초대해요'} inviteLink={inviteLink} />
      </section>
      <Button
        variant="criticalSolid"
        size="large"
        css={actionButtonStyle}
        onClick={() => leaveStudy.mutate({ studyId })}
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
