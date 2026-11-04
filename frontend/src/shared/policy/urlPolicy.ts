export type LinkPolicy = {
  to: string;
  replace: boolean;
};
export type NavigationPolicy = LinkPolicy | { to: -1 };
type Id = number | string;
export type StudyTab = 'home' | 'notices' | 'assignments' | 'members';
export type ContentTab = 'detail' | 'status';

const path = {
  landing: '/',
  studies: '/studies',
  notifications: '/notifications',
  login: '/login',
  callback: '/auth/kakao/callback',
  myPage: '/studies/mypage',
  createStudy: '/studies/new',
  joinStudy: '/studies/join',
  study: (studyId: Id) => `/studies/${studyId}`,
  management: (studyId: Id) => `/studies/${studyId}/management`,
  editStudy: (studyId: Id) => `/studies/${studyId}/edit`,
  members: (studyId: Id) => `/studies/${studyId}/members`,
  notices: (studyId: Id) => `/studies/${studyId}/notices`,
  createNotice: (studyId: Id) => `/studies/${studyId}/notices/create`,
  notice: (studyId: Id, noticeId: Id) => `/studies/${studyId}/notices/${noticeId}`,
  editNotice: (studyId: Id, noticeId: Id) => `/studies/${studyId}/notices/${noticeId}/edit`,
  assignments: (studyId: Id) => `/studies/${studyId}/assignments`,
  createAssignment: (studyId: Id) => `/studies/${studyId}/assignments/create`,
  assignment: (studyId: Id, assignmentId: Id) => `/studies/${studyId}/assignments/${assignmentId}`,
  editAssignment: (studyId: Id, assignmentId: Id) =>
    `/studies/${studyId}/assignments/${assignmentId}/edit`,
  submission: (studyId: Id, assignmentId: Id, submissionId: Id) =>
    `/studies/${studyId}/assignments/${assignmentId}/submissions/${submissionId}`,
};

const push = (to: string): LinkPolicy => ({ to, replace: false });
const replace = (to: string): LinkPolicy => ({ to, replace: true });
function back(fallback: string): NavigationPolicy {
  const historyIndex: unknown = window.history.state?.idx;

  return typeof historyIndex === 'number' && Number.isInteger(historyIndex) && historyIndex > 0
    ? { to: -1 }
    : replace(fallback);
}

function parseParentPath(pathname: string) {
  let parentPath = pathname;

  // 후행 슬래시 제거
  if (pathname.endsWith('/')) {
    parentPath = pathname.replace(/\/+$/, '');
  }

  parentPath = parentPath.substring(0, parentPath.lastIndexOf('/'));

  if (parentPath.endsWith('submissions')) {
    return parentPath.substring(0, parentPath.lastIndexOf('/'));
  } else {
    return parentPath;
  }
}

// 직접 URL 진입 등 이전 앱 이력이 없을 때만 사용하는 기본 복귀 목적지.
function fallbackFor(pathname: string): string {
  return parseParentPath(pathname) || path.studies;
}

// OAuth 왕복 후에도 앱 내부의 로그인 전 주소로만 복귀한다.
function loginReturnPath(returnTo: string | null | undefined): string | undefined {
  if (!returnTo?.startsWith('/')) return;

  try {
    const url = new URL(returnTo, window.location.origin);
    const pathname =
      '/' + decodeURIComponent(url.pathname).split('/').filter(Boolean).join('/').toLowerCase();
    if (url.origin !== window.location.origin || url.pathname.startsWith('//')) return;
    if (pathname === path.login || pathname === path.callback) return;
    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return;
  }
}

export const URL_POLICY = {
  // 일반 진입은 새 이력으로 남긴다.
  toLanding: () => push(path.landing),
  toStudies: () => push(path.studies),
  toNotifications: () => push(path.notifications),
  toLogin: () => push(path.login),
  toKakaoCallback: () => push(path.callback),
  toMyPage: () => push(path.myPage),
  toCreateStudy: () => push(path.createStudy),
  toJoinStudy: () => push(path.joinStudy),
  toStudy: (studyId: Id) => push(path.study(studyId)),
  toStudyManagement: (studyId: Id) => push(path.management(studyId)),
  toEditStudy: (studyId: Id) => push(path.editStudy(studyId)),
  toMembers: (studyId: Id) => push(path.members(studyId)),
  toNotices: (studyId: Id) => push(path.notices(studyId)),
  toCreateNotice: (studyId: Id) => push(path.createNotice(studyId)),
  toNotice: (studyId: Id, noticeId: Id) => push(path.notice(studyId, noticeId)),
  toEditNotice: (studyId: Id, noticeId: Id) => push(path.editNotice(studyId, noticeId)),
  toAssignments: (studyId: Id) => push(path.assignments(studyId)),
  toCreateAssignment: (studyId: Id) => push(path.createAssignment(studyId)),
  toAssignment: (studyId: Id, assignmentId: Id) => push(path.assignment(studyId, assignmentId)),
  toEditAssignment: (studyId: Id, assignmentId: Id) =>
    push(path.editAssignment(studyId, assignmentId)),
  toSubmission: (studyId: Id, assignmentId: Id, submissionId: Id) =>
    push(path.submission(studyId, assignmentId, submissionId)),
  toDeepLink: (url: string) => push(url),
  toInvitePage: (url: string) => push(url),

  // 홈에서 첫 탭 진입은 홈을 남기고, 다른 탭으로의 전환은 현재 항목을 교체한다.
  toStudyTab: (studyId: Id | undefined, tab: StudyTab, currentPathname: string): LinkPolicy => {
    if (studyId === undefined) return replace(path.studies);
    const home = path.study(studyId);
    const target = tab === 'home' ? home : `${home}/${tab}`;
    return currentPathname.replace(/\/+$/, '') === home && tab !== 'home'
      ? push(target)
      : replace(target);
  },
  // 콘텐츠 내부 내용/현황 전환은 기존 쿼리와 해시를 보존하며 이력을 추가하지 않는다.
  toContentTab: (
    location: { pathname: string; search: string; hash: string },
    tab: ContentTab,
  ): LinkPolicy => {
    const params = new URLSearchParams(location.search);
    if (tab === 'status') params.set('view', 'status');
    else params.delete('view');
    const search = params.toString();
    return replace(`${location.pathname}${search ? `?${search}` : ''}${location.hash}`);
  },

  // 완료한 생성/참여 화면은 뒤로가기 대상에서 제외한다.
  afterStudyCreate: (studyId: Id) => replace(path.study(studyId)),
  afterStudyJoin: (studyId: Id) => replace(path.study(studyId)),
  afterNoticeCreate: (studyId: Id, noticeId: Id) => replace(path.notice(studyId, noticeId)),
  afterAssignmentCreate: (studyId: Id, assignmentId: Id) =>
    replace(path.assignment(studyId, assignmentId)),
  // 수정 완료는 기존 화면으로 복귀하고, 직접 진입했으면 해당 상세를 사용한다.
  afterStudyEdit: (studyId: Id) => back(path.study(studyId)),
  afterNoticeEdit: (studyId: Id, noticeId: Id) => back(path.notice(studyId, noticeId)),
  afterAssignmentEdit: (studyId: Id, assignmentId: Id) =>
    back(path.assignment(studyId, assignmentId)),
  // 삭제한 현재 화면을 목록으로 교체한다.
  afterNoticeDelete: (studyId: Id) => replace(path.notices(studyId)),
  afterAssignmentDelete: (studyId: Id) => replace(path.assignments(studyId)),
  afterStudyDelete: () => replace(path.studies),
  afterStudyLeave: () => replace(path.studies),
  loginReturnPath,
  afterLogin: (returnTo?: string | null) => replace(loginReturnPath(returnTo) ?? path.studies),
  afterLogout: () => replace(path.login),
  afterAccountWithdrawal: () => replace(path.login),
  afterSessionExpired: () => replace(path.login),
  afterAuthenticationRejected: () => push(path.login),

  back: (pathname: string) => back(fallbackFor(pathname)),
} as const;
