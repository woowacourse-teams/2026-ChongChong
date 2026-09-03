import { useId } from 'react';
import type { CSSProperties } from 'react';
import assignmentIcon from '../../../shared/assets/assign.svg';
import assignmentGreenIcon from '../../../shared/assets/assign-green.svg';
import homeIcon from '../../../shared/assets/home.svg';
import homeGreenIcon from '../../../shared/assets/home-green.svg';
import leftArrowIcon from '../../../shared/assets/left-arrow.svg';
import noticeIcon from '../../../shared/assets/notice.svg';
import noticeGreenIcon from '../../../shared/assets/notice-green.svg';
import userIcon from '../../../shared/assets/user.svg';
import userGreenIcon from '../../../shared/assets/user-green.svg';
import studyArt from '../../../shared/assets/icons/header-icon.svg';
import Button from '../../../shared/ui/Button';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import List from '../../../shared/ui/List';
import Main from '../../../shared/ui/Main';
import TopHeader from '../../../shared/ui/TopHeader';
import { formatDateToString } from '../../../shared/utils/formatDate';
import { tokens, typography } from '../../../styles/global';
import AssignmentArticle from '../../assignment/components/AssignmentArticle';
import PreviewSubmitStatusCard from './PreviewSubmitStatusCard';
import InviteLinkBox from '../../member/components/InviteLinkBox';
import MemberRow from '../../member/components/MemberRow';
import {
  MemberActiveAssignmentCard,
  MemberActiveNoticeCard,
} from '../../study/components/ActiveStudyCard';
import { StudyMemberWelcomeBanner } from '../../study/components/WelcomeBanner';
import chongchongLogo from '../assets/chongchong-logo.png';
import type { PreviewFeatureId } from '../previewFeatures';

const studyName = '차곡차곡 독서 모임';
const assignment = {
  id: 1,
  title: '3장 읽고 생각 나누기',
  content: '인상 깊었던 문장과 나의 생각을 정리해주세요.',
  submissionMethod: '독서 노트 링크를 제출해주세요',
  closeAt: '2026-09-09T20:00:00',
};
const noticeTitle = '이번 주 모임 장소 안내';
const formStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSProperties;
const sectionLabelStyle = {
  ...typography.sectionLabel,
  margin: '0 0 ' + tokens.spacing[3],
  color: tokens.text.default,
} satisfies CSSProperties;
const submissionStatus = {
  id: assignment.id,
  memberCount: 4,
  completeCount: 3,
  incompleteCount: 1,
  completeMembers: ['서연', '민준', '하은'].map((name, index) => ({
    id: index + 1,
    name,
    profileImage: null,
  })),
  incompleteMembers: [{ id: 4, name: '지우', profileImage: null }],
};

function ScreenHeader({
  title,
  subtitle,
  plain = false,
}: {
  title: string;
  subtitle?: string;
  plain?: boolean;
}) {
  return (
    <TopHeader
      left={
        <span className="cc-demo-back">
          <img src={leftArrowIcon} alt="" width={24} height={24} />
        </span>
      }
      middle={
        <>
          {plain ? <div>{title}</div> : <TopHeader.Title>{title}</TopHeader.Title>}
          {subtitle && <TopHeader.Subtitle>{subtitle}</TopHeader.Subtitle>}
        </>
      }
    />
  );
}

/** Same layout and assets as BottomTab, without router links or route hooks. */
function ScreenTabs({ active }: { active: 'home' | 'notice' | 'assignment' | 'members' }) {
  const tabs = [
    { name: 'home', label: '홈', icon: active === 'home' ? homeGreenIcon : homeIcon },
    {
      name: 'notice',
      label: '공지',
      icon: active === 'notice' ? noticeGreenIcon : noticeIcon,
    },
    {
      name: 'assignment',
      label: '과제',
      icon: active === 'assignment' ? assignmentGreenIcon : assignmentIcon,
    },
    { name: 'members', label: '멤버', icon: active === 'members' ? userGreenIcon : userIcon },
  ];

  return (
    <div className="cc-demo-tabs">
      {tabs.map((tab) => (
        <div key={tab.name}>
          <img src={tab.icon} alt="" width={22} height={22} />
          <p className={active === tab.name ? 'cc-demo-tab-active' : undefined}>{tab.label}</p>
        </div>
      ))}
    </div>
  );
}

function StudyScreen() {
  const id = useId();

  return (
    <>
      <ScreenHeader title="스터디 만들기" plain />
      <Main
        css={{
          padding: '0 ' + tokens.spacing[4] + ' ' + tokens.layout.gutter + ' ' + tokens.spacing[5],
        }}
      >
        <div className="cc-demo-study-art">
          <img src={studyArt} alt="" width={70} height={70} />
        </div>
        <div css={{ ...formStyle, flex: 'initial' }}>
          <Field
            id={id + '-name'}
            isRequired
            label="스터디 이름"
            helpText="스터디원에게 그대로 보여요"
          >
            <Input id={id + '-name'} value={studyName} readOnly maxLength={15} />
          </Field>
          <Field
            id={id + '-description'}
            label="어떤 스터디인가요?"
            helpText="모이는 요일과 시간을 적어두면 초대할 때 설명이 줄어들어요"
          >
            <TextArea
              id={id + '-description'}
              value="매주 수요일 저녁 8시, 함께 읽고 생각을 나눠요."
              readOnly
              maxLength={30}
            />
          </Field>
          <Button variant="brandSolid" size="large" css={{ marginTop: tokens.spacing[1] }}>
            스터디 만들기
          </Button>
        </div>
      </Main>
    </>
  );
}

function InviteScreen() {
  return (
    <>
      <ScreenHeader title="멤버" />
      <Main>
        <section>
          <h2 css={typography.subtitle}>스터디 멤버</h2>
          <List css={{ marginBottom: tokens.spacing[6] }}>
            {['서연', '지우', '민준', '하은'].map((name, index) => (
              <List.Item key={name}>
                <MemberRow.Member name={name} role={index === 0 ? 'LEADER' : 'MEMBER'} />
              </List.Item>
            ))}
          </List>
          <InviteLinkBox
            title="링크를 통해 새로운 스터디원을 초대해요"
            inviteLink="https://chongchong.app/studies/join?token=book-study"
          />
        </section>
        <Button variant="criticalSolid" size="large" css={{ margin: tokens.spacing[5] + ' 0' }}>
          스터디 탈퇴하기
        </Button>
      </Main>
      <ScreenTabs active="members" />
    </>
  );
}

function AssignmentScreen() {
  const id = useId();

  return (
    <>
      <ScreenHeader title="과제" />
      <Main>
        <div css={formStyle}>
          <Field id={id + '-title'} label="제목" isRequired>
            <Input id={id + '-title'} value={assignment.title} readOnly maxLength={20} />
          </Field>
          <Field id={id + '-content'} label="내용" isRequired>
            <TextArea id={id + '-content'} value={assignment.content} readOnly maxLength={10000} />
          </Field>
          <Field id={id + '-method'} label="제출 방법" isRequired>
            <Input id={id + '-method'} value={assignment.submissionMethod} readOnly />
          </Field>
          <Field id={id + '-close'} label="마감 시각" isRequired>
            <Button id={id + '-close'} variant="neutralOutline" size="large">
              {formatDateToString(assignment.closeAt)}
            </Button>
          </Field>
          <Button variant="brandSolid" size="large" css={{ marginTop: tokens.spacing[8] }}>
            과제 올리기
          </Button>
        </div>
      </Main>
      <ScreenTabs active="assignment" />
    </>
  );
}

function NoticeScreen() {
  const id = useId();

  return (
    <>
      <ScreenHeader title="공지" />
      <Main>
        <div css={formStyle}>
          <Field id={id + '-title'} label="제목" isRequired>
            <Input id={id + '-title'} value={noticeTitle} readOnly maxLength={20} />
          </Field>
          <Field
            id={id + '-content'}
            label="내용"
            isRequired
            helpText="스터디원은 끝까지 읽어야 읽음 처리를 할 수 있어요"
          >
            <TextArea
              id={id + '-content'}
              value={
                '이번 주는 차곡 북카페에서 만나요.\n\n수요일 저녁 8시, 2층에서 만나요.\n책과 독서 노트를 준비해주세요.'
              }
              readOnly
              rows={5}
              maxLength={10000}
            />
          </Field>
          <Button variant="brandSolid" size="large" css={{ marginTop: tokens.spacing[8] }}>
            공지 올리기
          </Button>
        </div>
      </Main>
    </>
  );
}

type ScreenAnimationProps = {
  active?: boolean;
  animationPaused?: boolean;
};

function ReminderScreen({ active = false, animationPaused = false }: ScreenAnimationProps) {
  return (
    <>
      <ScreenHeader title={studyName} subtitle="지우 · 스터디원" />
      <Main>
        <div className="cc-demo-study-content">
          <StudyMemberWelcomeBanner username="지우" todoCount={2} />
          <div className="cc-demo-study-sections">
            <section>
              <h2 css={sectionLabelStyle}>읽지 않은 공지</h2>
              <List>
                <List.Item>
                  <MemberActiveNoticeCard title={noticeTitle} />
                </List.Item>
              </List>
            </section>
            <section>
              <h2 css={sectionLabelStyle}>제출하지 않은 과제</h2>
              <List>
                <List.Item>
                  <MemberActiveAssignmentCard title={assignment.title} />
                </List.Item>
              </List>
            </section>
          </div>
        </div>
      </Main>
      <ScreenTabs active="home" />
      {active && (
        <div
          className={
            'cc-demo-incoming-banner' +
            (animationPaused ? '' : ' cc-demo-incoming-banner--animated')
          }
        >
          <div className="cc-demo-incoming-banner-content">
            <span className="cc-demo-notification-app">
              <img src={chongchongLogo} alt="" width={40} height={44} />
            </span>
            <div className="cc-demo-notification-content">
              <div className="cc-demo-notification-meta">
                <strong>총총</strong>
                <span>지금</span>
              </div>
              <p className="cc-demo-notification-title">과제 마감이 얼마 남지 않았어요</p>
              <p className="cc-demo-notification-assignment">{assignment.title}</p>
              <p className="cc-demo-notification-description">
                오늘 오후 8시까지 독서 노트를 제출해요.
              </p>
            </div>
          </div>
          <div className="cc-demo-notification-handle" />
        </div>
      )}
    </>
  );
}

function SubmissionScreen() {
  return (
    <>
      <ScreenHeader title="과제" />
      <Main>
        <PreviewSubmitStatusCard status={submissionStatus} />
        <AssignmentArticle assignment={assignment} />
      </Main>
      <ScreenTabs active="assignment" />
    </>
  );
}

const screens = {
  study: StudyScreen,
  invite: InviteScreen,
  assignment: AssignmentScreen,
  notice: NoticeScreen,
  reminder: ReminderScreen,
  submission: SubmissionScreen,
};

/** Real app primitives rendered at a mobile viewport size; all content stays inert. */
export default function FeatureScreen({
  feature,
  active = false,
  animationPaused = false,
}: { feature: PreviewFeatureId } & ScreenAnimationProps) {
  const Screen = screens[feature];

  return (
    <div className={'cc-demo-screen cc-demo-screen--' + feature} aria-hidden="true" inert>
      <div className="cc-demo-app-canvas">
        <Screen active={active} animationPaused={animationPaused} />
      </div>
    </div>
  );
}
