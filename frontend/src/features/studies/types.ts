export type Role = 'LEADER' | 'MEMBER';

export interface Study {
  id: string;
  role: Role;
  title: string;
  description: string;
  memberCount: number;
  noticeCount: number;
  assignmentCount: number;
}

export interface LeaderStudyDetail {
  memberCount: number;
  notices: {
    count: number;
    items: { id: number; title: string; completeCount: number }[];
  };
  assignments: {
    count: number;
    items: { id: number; title: string; completeCount: number }[];
  };
}

export interface MemberStudyDetail {
  totalCount: number;
  notices: { id: number; title: string }[];
  assignments: { id: number; title: string }[];
}

export type StudyDetail<R extends Role> = R extends 'LEADER'
  ? LeaderStudyDetail
  : MemberStudyDetail;
