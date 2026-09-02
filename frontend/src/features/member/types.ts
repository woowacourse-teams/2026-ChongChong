export type StudyRole = 'LEADER' | 'MEMBER';

export interface Member {
  id: number;
  name: string;
  profileImage: string | null;
  role: StudyRole;
}

export interface MemberResponse {
  members: Member[];
}
