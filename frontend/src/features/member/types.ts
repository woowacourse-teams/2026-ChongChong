export type StudyRole = 'LEADER' | 'MEMBER';

export interface Member {
  id: number;
  name: string;
  profileImage: string;
  role: StudyRole;
}
