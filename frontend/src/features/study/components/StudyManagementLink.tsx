import { CSSProperties } from 'react';
import { Link } from 'react-router';
import ManagementIcon from '../../../shared/assets/management-icon.webp';

const managementLinkStyle = {
  display: 'grid',
  width: '32px',
  height: '32px',
  placeItems: 'center',
} satisfies CSSProperties;

interface Props {
  studyId: number;
}

export default function StudyManagementLink({ studyId }: Props) {
  return (
    <Link to={`/studies/${studyId}/management`} css={managementLinkStyle}>
      <img src={ManagementIcon} alt="스터디 관리" width={26} height={26} />
    </Link>
  );
}
