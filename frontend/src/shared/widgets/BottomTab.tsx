import type { CSSObject } from '@emotion/react';
import { CSSProperties } from 'react';
import { Link } from 'react-router';
import { useParams, useLocation } from 'react-router';
import HomeIcon from '../assets/home.webp';
import ActivehomeIcon from '../assets/home-green.webp';
import NoticeIcon from '../assets/notice.webp';
import ActiveNoticeIcon from '../assets/notice-green.webp';
import AssignmentIcon from '../assets/assign.webp';
import ActiveAssignmentIcon from '../assets/assign-green.webp';
import MemberIcon from '../assets/user.webp';
import ActiveMemberIcon from '../assets/user-green.webp';
import { tokens, typography } from '../../styles/global';

const tabStyle = {
  position: 'sticky',
  bottom: 0,
  zIndex: 100,
  background: tokens.bg.default,
  maxWidth: tokens.screenSize.default,
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: `${tokens.spacing[2]} ${tokens.spacing[10]}`,
  borderTop: tokens.border.neutral,
  borderTopLeftRadius: tokens.radius.lg,
  borderTopRightRadius: tokens.radius.lg,
} satisfies CSSProperties;

const linkStyle = {
  position: 'relative',
  '&::after': {
    content: '""',
    position: 'absolute',
    inset: `-8px -12px`,
  },
} satisfies CSSObject;

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
      <Link css={linkStyle} to={basePath} aria-current={isHome ? 'page' : undefined}>
        {<img src={isHome ? ActivehomeIcon : HomeIcon} alt="" width={22} height={22} />}
        <p css={[textStyle, isHome && { color: tokens.text.brand }]}>홈</p>
      </Link>
      <Link css={linkStyle} to={`${basePath}/notices`} aria-current={isNotice ? 'page' : undefined}>
        <img src={isNotice ? ActiveNoticeIcon : NoticeIcon} width={22} height={22} alt="" />
        <p css={[textStyle, isNotice && { color: tokens.text.brand }]}>공지</p>
      </Link>
      <Link
        css={linkStyle}
        to={`${basePath}/assignments`}
        aria-current={isAssignment ? 'page' : undefined}
      >
        <img
          src={isAssignment ? ActiveAssignmentIcon : AssignmentIcon}
          width={22}
          height={22}
          alt=""
        />
        <p css={[textStyle, isAssignment && { color: tokens.text.brand }]}>과제</p>
      </Link>
      <Link css={linkStyle} to={`${basePath}/members`} aria-current={isMember ? 'page' : undefined}>
        <img src={isMember ? ActiveMemberIcon : MemberIcon} width={22} height={22} alt="" />
        <p css={[textStyle, isMember && { color: tokens.text.brand }]}>멤버</p>
      </Link>
    </nav>
  );
}
