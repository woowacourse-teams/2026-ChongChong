export interface Assignment {
  id: number;
  title: string;
  content: string;
  submissionMethod: string;
  closeAt: string;
  memberCount?: number;
  completeCount?: number;
  remindAt?: string;
  isComplete: boolean;
}

export interface AssignmentListResponse {
  nextCursor: number;
  hasNext: boolean;
  assignments: Assignment[];
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
  submissionMethod: string;
  closeAt: string;
  submissionId?: number;
}

export type AssignmentValue = Omit<AssignmentDetail, 'id' | 'submissionId'>;

export type UpdateAssignmentValue = Partial<AssignmentValue>;

export interface Submission {
  id: number;
  name: string;
  profileImage: string;
  createdAt: string;
}

export interface SubmissionDetail extends Submission {
  content: string;
  link?: string;
}

export interface AssignmentSubmissionValue {
  content: string;
  link?: string;
}

export interface CreateAssignmentResponse {
  readonly assignmentId: number;
}

export interface CreateSubmissionResponse {
  submissionId: number;
}
