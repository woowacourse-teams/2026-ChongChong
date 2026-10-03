import { CSSProperties } from 'react';
import { Link } from 'react-router';
import List from '../../../shared/ui/List';
import { tokens, typography } from '../../../styles/global';
import { StudyRole } from '../../member/types';

const StudyManagementItemStyle = {
  ...typography.title,
  padding: `${tokens.spacing[5]} 0`,
  borderBottom: tokens.border.neutral,
  cursor: 'pointer',
} satisfies CSSProperties;

interface Props {
  role: StudyRole;
}

export function ManagementSection({ role }: Props) {
  return (
    <section>
      {role === 'LEADER' ? <StudyLeaderManagementList /> : <StudyMemberManagementList />}
    </section>
  );
}

function StudyLeaderManagementList() {
  return (
    <List>
      {/* <List.Item css={StudyManagementItemStyle}>스터디 프로필 수정 (준비 중)</List.Item> */}
      {/* TODO: 스터디 프로필 수정 페이지가 추가되면 링크를 연결합니다. */}
      <List.Item css={StudyManagementItemStyle}>
        <Link to="">스터디 정보 수정</Link>
      </List.Item>
      {/* TODO: 스터디 삭제를 멤버 페이지에서 옮깁니다. */}
      {/* <List.Item>
        <Link>스터디 삭제</Link>
      </List.Item> */}
    </List>
  );
}

function StudyMemberManagementList() {
  return (
    <List>
      {/* TODO: 스터디 탈퇴를 멤버 페이지에서 옮깁니다. */}
      {/* <List.Item css={StudyManagementItemStyle}>
        <Link to="">스터디 탈퇴</Link>
      </List.Item> */}
    </List>
  );
}
