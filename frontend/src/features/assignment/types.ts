export interface Assignment {
  id: number;
  title: string;
  content: string;
  submissionType: string;
  closeAt: string;
  memberCount: number;
  completeCount: number;
  remindAt?: string;
  isComplete: boolean;
}

export interface Member {
  id: number;
  name: string;
  profileImage: string;
  lastRemindAt?: string;
}

export interface AssignmentSubmitStatus {
  id: number;
  memberCount: number;
  completeCount: number;
  incompleteCount: number;
  remindAt?: string;
  completeMembers: Member[];
  incompleteMembers: Member[];
}

export interface AssignmentDetail {
  id: number;
  title: string;
  content: string;
  submissionType: string;
  closeAt: string;
}
