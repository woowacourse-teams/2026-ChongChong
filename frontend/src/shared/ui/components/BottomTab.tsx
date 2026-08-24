import { CSSProperties } from 'react';
import { Link } from 'react-router';
import { useParams, useLocation } from 'react-router';
import HomeIcon from '../../assets/home.svg';
import ActivehomeIcon from '../../assets/home-green.svg';
import NoticeIcon from '../../assets/notice.svg';
import ActiveNoticeIcon from '../../assets/notice-green.svg';
import AssignmentIcon from '../../assets/assign.svg';
import ActiveAssignmentIcon from '../../assets/assign-green.svg';
import MemberIcon from '../../assets/user.svg';
import ActiveMemberIcon from '../../assets/user-green.svg';
import { tokens, typography } from '../../../styles/global';

const tabStyle = {
  position: 'sticky',
  bottom: 0,
  zIndex: 100,
  background: tokens.bg.default,
  width: '448px',
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: `${tokens.spacing[2]} ${tokens.spacing[10]}`,
  borderTop: tokens.border.neutral,
  borderTopLeftRadius: tokens.radius.lg,
  borderTopRightRadius: tokens.radius.lg,
} satisfies CSSProperties;

const textStyle = {
  ...typography.footnote,
  textAlign: 'center',
  color: tokens.text.placeholder,
} satisfies CSSProperties;

export default function BottomTab() {
  const { studyId } = useParams();
  const { pathname } = useLocation();
  const basePath = `/studies/${studyId}`;

  const isHome = pathname === basePath;
  const isNotice = pathname.includes('notices');
  const isAssignment = pathname.includes('assignments');
  const isMember = pathname.includes('member');

  return (
    <nav css={tabStyle}>
      <Link to={basePath}>
        {<img src={isHome ? ActivehomeIcon : HomeIcon} alt="" width={22} height={22} />}
        <p css={[textStyle, isHome && { color: tokens.text.brand }]}>홈</p>
      </Link>
      <Link to={`${basePath}/notices`}>
        <img src={isNotice ? ActiveNoticeIcon : NoticeIcon} width={22} height={22} alt="" />
        <p css={[textStyle, isNotice && { color: tokens.text.brand }]}>공지</p>
      </Link>
      <Link to={`${basePath}/assignments`}>
        <img
          src={isAssignment ? ActiveAssignmentIcon : AssignmentIcon}
          width={22}
          height={22}
          alt=""
        />
        <p css={[textStyle, isAssignment && { color: tokens.text.brand }]}>과제</p>
      </Link>
      <Link to={`${basePath}/members`}>
        <img src={isMember ? ActiveMemberIcon : MemberIcon} width={22} height={22} alt="" />
        <p css={[textStyle, isMember && { color: tokens.text.brand }]}>멤버</p>
      </Link>
    </nav>
  );
}
