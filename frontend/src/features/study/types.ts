export type Role = 'LEADER' | 'MEMBER';

export interface Study {
  id: number;
  role: Role;
  name: string;
  description: string | null;
  memberCount: number;
  noticeCount: number;
  assignmentCount: number;
}

export interface LeaderStudyDetail {
  notices: {
    count: number;
    items: { id: number; title: string; completeCount: number; memberCount: number }[];
  };
  assignments: {
    count: number;
    items: { id: number; title: string; completeCount: number; memberCount: number }[];
  };
}

export interface MemberStudyDetail {
  totalCount: number;
  notices: {
    items: { id: number; title: string }[];
  };
  assignments: {
    items: { id: number; title: string }[];
  };
}

export type StudyDetail<R extends Role> = R extends 'LEADER'
  ? LeaderStudyDetail
  : MemberStudyDetail;

export interface UserStudyInfo {
  studyName: string;
  role: Role;
  userName: string;
}

export interface InviteLink {
  inviteLink: string;
}
