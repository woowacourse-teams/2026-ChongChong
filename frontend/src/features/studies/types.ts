type Role = 'LEADER' | 'MEMBER';

export interface Study {
  id: string;
  role: Role;
  title: string;
  description: string;
  memberCount: number;
  noticeCount: number;
  assignmentCount: number;
}
