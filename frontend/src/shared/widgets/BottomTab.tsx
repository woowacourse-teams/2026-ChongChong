import { Paragraph } from '../ui/Typography';
import { CSSProperties } from 'react';
import { URL_POLICY } from '../constants/urlPolicy';
import { Link, useParams, useLocation } from 'react-router';
import HomeIcon from '../assets/home.webp';
import ActivehomeIcon from '../assets/home-green.webp';
import NoticeIcon from '../assets/notice.webp';
import ActiveNoticeIcon from '../assets/notice-green.webp';
import AssignmentIcon from '../assets/assign.webp';
import ActiveAssignmentIcon from '../assets/assign-green.webp';
import MemberIcon from '../assets/user.webp';
import ActiveMemberIcon from '../assets/user-green.webp';
import { tokens } from '../../styles/global';

const tabStyle = {
  position: 'sticky',
  bottom: 0,
  zIndex: 100,
  background: tokens.bg.default,
  maxWidth: tokens.screenSize.default,
  display: 'flex',
  alignItems: 'stretch',
  borderTop: tokens.border.neutral,
  borderTopLeftRadius: tokens.radius.lg,
  borderTopRightRadius: tokens.radius.lg,
} satisfies CSSProperties;

const linkStyle = {
  flex: 1,
  minWidth: 0,
  textAlign: 'center',
  padding: `${tokens.spacing[2]} 0`,
  paddingBottom: `calc(${tokens.spacing[2]} + ${tokens.layout.safeBottom})`,
} satisfies CSSProperties;

const textStyle = {
  textAlign: 'center',
  color: tokens.text.placeholder,
} satisfies CSSProperties;

export default function BottomTab() {
  const { studyId } = useParams();
  const { pathname } = useLocation();
  const basePath =
    studyId === undefined ? URL_POLICY.toStudies().to : URL_POLICY.toStudy(studyId).to;

  const isHome = pathname === basePath;
  const isNotice = pathname.includes('notices');
  const isAssignment = pathname.includes('assignments');
  const isMember = pathname.includes('member');

  return (
    <nav css={tabStyle}>
      <Link
        css={linkStyle}
        {...URL_POLICY.toStudyTab(studyId, 'home', pathname)}
        aria-current={isHome ? 'page' : undefined}
      >
        {<img src={isHome ? ActivehomeIcon : HomeIcon} alt="" width={22} height={22} />}
        <Paragraph variant="footnote" css={[textStyle, isHome && { color: tokens.text.brand }]}>
          홈
        </Paragraph>
      </Link>
      <Link
        css={linkStyle}
        {...URL_POLICY.toStudyTab(studyId, 'notices', pathname)}
        aria-current={isNotice ? 'page' : undefined}
      >
        <img src={isNotice ? ActiveNoticeIcon : NoticeIcon} width={22} height={22} alt="" />
        <Paragraph variant="footnote" css={[textStyle, isNotice && { color: tokens.text.brand }]}>
          공지
        </Paragraph>
      </Link>
      <Link
        css={linkStyle}
        {...URL_POLICY.toStudyTab(studyId, 'assignments', pathname)}
        aria-current={isAssignment ? 'page' : undefined}
      >
        <img
          src={isAssignment ? ActiveAssignmentIcon : AssignmentIcon}
          width={22}
          height={22}
          alt=""
        />
        <Paragraph
          variant="footnote"
          css={[textStyle, isAssignment && { color: tokens.text.brand }]}
        >
          과제
        </Paragraph>
      </Link>
      <Link
        css={linkStyle}
        {...URL_POLICY.toStudyTab(studyId, 'members', pathname)}
        aria-current={isMember ? 'page' : undefined}
      >
        <img src={isMember ? ActiveMemberIcon : MemberIcon} width={22} height={22} alt="" />
        <Paragraph variant="footnote" css={[textStyle, isMember && { color: tokens.text.brand }]}>
          멤버
        </Paragraph>
      </Link>
    </nav>
  );
}
