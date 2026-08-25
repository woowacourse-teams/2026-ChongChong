import { CSSProperties } from 'react';
import { useMutation, useSuspenseQuery, useQueryClient } from '@tanstack/react-query';
import useStudyId from '../../studies/hooks/useStudyId';
import { memberQueries } from '../queries';
import studyQueries from '../../studies/queries';
import { tokens, typography } from '../../../styles/global';
import Button from '../../../shared/ui/Button';
import List from '../../../shared/ui/List';
import MemberRow from './MemberRow';
import CopyIcon from '../../../shared/assets/copy.svg';
import { kickMember } from '../api';

const listStyle = {
  marginBottom: tokens.spacing[6],
} satisfies CSSProperties;

const inviteDescriptionStyle = {
  ...typography.paragraph,
  margin: `${tokens.spacing[2]} 0`,
  color: tokens.text.muted,
} satisfies CSSProperties;

const inviteLinkBlockStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  padding: tokens.spacing[4],
  background: tokens.bg.subtle,
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
} satisfies CSSProperties;

const inviteLinkStyle = {
  ...typography.paragraph,
  overflow: 'hidden',
  color: tokens.text.secondary,
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const copyButtonStyle = {
  flex: '0 0 auto',
  background: 'none',
  border: 'none',
  cursor: 'pointer',
} satisfies CSSProperties;

const actionButtonStyle = {
  margin: `${tokens.spacing[5]} 0`,
} satisfies CSSProperties;

export function InviteLinkBox({ inviteLink }: { inviteLink: string }) {
  const handleCopy = () => {
    navigator.clipboard.writeText(inviteLink);
  };

  return (
    <>
      <p css={inviteDescriptionStyle}>링크를 통해 새로운 스터디원을 초대해요</p>
      <div css={inviteLinkBlockStyle}>
        <span css={inviteLinkStyle}>{inviteLink}</span>
        <button css={copyButtonStyle} type="button" onClick={handleCopy} aria-label="링크 복사">
          <img src={CopyIcon} width={16} height={20} alt="" />
        </button>
      </div>
    </>
  );
}

function LeaderContent() {
  const { studyId } = useStudyId();
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

  return (
    <>
      <section>
        <h2 css={typography.subtitle}>스터디 멤버</h2>
        <List css={listStyle}>
          {members.map((member) => (
            <List.Item key={member.id}>
              <MemberRow.Leader
                data-testid={`member-${member.id}-row`}
                name={member.name}
                role={member.role}
                onKick={() => deleteMember.mutate({ studyId, memberId: member.id })}
              />
            </List.Item>
          ))}
        </List>
        <InviteLinkBox inviteLink={inviteLink} />
      </section>
      <Button variant="criticalSolid" size="large" css={actionButtonStyle} onClick={() => {}}>
        스터디 삭제하기
      </Button>
    </>
  );
}

function MemberContent() {
  const { studyId } = useStudyId();
  const {
    data: { inviteLink },
  } = useSuspenseQuery(studyQueries.inviteLink(studyId));
  const { data: members } = useSuspenseQuery({
    ...memberQueries.list(Number(studyId)),
    select: (data) => data.members,
  });

  return (
    <>
      <section>
        <h2 css={typography.subtitle}>스터디 멤버</h2>
        <List css={listStyle}>
          {members.map((member) => (
            <List.Item key={member.id}>
              <MemberRow.Member
                data-testid={`member-${member.id}-row`}
                name={member.name}
                role={member.role}
              />
            </List.Item>
          ))}
        </List>
        <InviteLinkBox inviteLink={inviteLink} />
      </section>
      <Button variant="criticalSolid" size="large" css={actionButtonStyle} onClick={() => {}}>
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
