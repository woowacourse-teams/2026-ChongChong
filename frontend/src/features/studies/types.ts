type Role = 'STUDY_LEADER' | 'SOME';

export interface Study {
  id: string;
  role: Role;
  title: string;
  description: string;
  memberCount: number;
  noticeCount: number;
  assignmentCount: number;
}
