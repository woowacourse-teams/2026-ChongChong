import { Paragraph, Text } from '../ui/Typography';
import { CSSProperties } from 'react';
import { URL_POLICY } from '../policy/urlPolicy';
import { Link, useParams, useLocation } from 'react-router';
import usePageNavigation from '../hooks/usePageNavigation';
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
  flexShrink: 0,
  alignItems: 'stretch',
  paddingBottom: tokens.layout.safeBottom,
  borderTop: tokens.border.neutral,
  borderTopLeftRadius: tokens.radius.lg,
  borderTopRightRadius: tokens.radius.lg,
} satisfies CSSProperties;

const linkStyle = {
  flex: 1,
  minWidth: 0,
  minHeight: tokens.layout.tabBarHeight,
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  textAlign: 'center',
  padding: `${tokens.spacing[2]} 0`,
} satisfies CSSProperties;

const textStyle = {
  textAlign: 'center',
  color: tokens.text.placeholder,
} satisfies CSSProperties;

const homeButtonStyle = {
  ...linkStyle,
  border: 0,
  background: 'transparent',
  cursor: 'pointer',
} satisfies CSSProperties;

export default function BottomTab() {
  const { studyId } = useParams();
  const location = useLocation();
  const { pathname } = location;
  const { move } = usePageNavigation();
  const basePath =
    studyId === undefined ? URL_POLICY.toStudies().to : URL_POLICY.toStudy(studyId).to;

  const isHome = pathname === basePath;
  const isNotice = pathname.includes('notices');
  const isAssignment = pathname.includes('assignments');
  const isMember = pathname.includes('member');
  const homeTab = URL_POLICY.toStudyHomeTab(studyId, location);
  const homeContent = (
    <>
      <img src={isHome ? ActivehomeIcon : HomeIcon} alt="" width={22} height={22} />
      <Text
        variant="footnote"
        css={[textStyle, { display: 'block' }, isHome && { color: tokens.text.brand }]}
      >
        홈
      </Text>
    </>
  );

  return (
    <nav css={tabStyle}>
      {typeof homeTab.to === 'number' ? (
        <button type="button" css={homeButtonStyle} onClick={() => move(homeTab)}>
          {homeContent}
        </button>
      ) : (
        <Link css={linkStyle} {...homeTab} aria-current={isHome ? 'page' : undefined}>
          {homeContent}
        </Link>
      )}
      <Link
        css={linkStyle}
        {...URL_POLICY.toStudyTab(studyId, 'notices', location)}
        aria-current={isNotice ? 'page' : undefined}
      >
        <img src={isNotice ? ActiveNoticeIcon : NoticeIcon} width={22} height={22} alt="" />
        <Paragraph variant="footnote" css={[textStyle, isNotice && { color: tokens.text.brand }]}>
          공지
        </Paragraph>
      </Link>
      <Link
        css={linkStyle}
        {...URL_POLICY.toStudyTab(studyId, 'assignments', location)}
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
        {...URL_POLICY.toStudyTab(studyId, 'members', location)}
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
