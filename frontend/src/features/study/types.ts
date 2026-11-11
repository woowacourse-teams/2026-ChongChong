export type Role = 'LEADER' | 'MEMBER';

export interface Study {
  id: number;
  role: Role;
  name: string;
  description: string | null;
  imageUrl?: string | null;
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
  isPublic: boolean;
  studyName: string;
  description: string | null;
  imageUrl?: string | null;
  role: Role;
  userName: string;
}

export interface InviteLink {
  inviteLink: string;
}

export interface StudyJoinInfo {
  id: number;
  name: string;
  description: string | null;
  imageUrl: string | null;
  memberCount: number;
}

export type StudyJoinTarget =
  { token: string; studyId?: never } | { studyId: number; token?: never };

export interface PublicStudyListResponse {
  studies: StudyJoinInfo[];
  hasNext: boolean;
  nextCursor: number | null;
}

export interface StudyFormValues {
  isPublic?: boolean;
  name: string;
  description: string;
  imageFileId?: number | null;
}
