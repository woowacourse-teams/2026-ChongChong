export interface Assignment {
  id: number;
  title: string;
  content: string;
  submissionMethod: string;
  closeAt: string;
  memberCount?: number;
  completeCount?: number;
  remindAt?: string | null;
  isComplete: boolean;
}

export interface AssignmentListResponse {
  nextCursor: number | null;
  hasNext: boolean;
  assignments: Assignment[];
}

export interface Member {
  id: number;
  name: string;
  profileImage: string | null;
  lastRemindAt?: string | null;
}

export interface AssignmentSubmitStatus {
  id: number;
  memberCount: number;
  completeCount: number;
  incompleteCount: number;
  remindAt?: string | null;
  completeMembers: Member[];
  incompleteMembers: Member[];
}

export interface AssignmentDetail {
  id: number;
  title: string;
  content: string;
  submissionMethod: string;
  closeAt: string;
}

export type AssignmentValue = Omit<AssignmentDetail, 'id'>;

export type UpdateAssignmentValue = Partial<AssignmentValue>;

export interface Submission {
  id: number;
  name: string;
  profileImage: string | null;
  createdAt: string;
}

export interface SubmissionListResponse {
  submissions: Submission[];
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
  assignmentId: number;
}

export interface CreateSubmissionResponse {
  submissionId: number;
}

export interface SubmittedAssignment {
  submitted: true;
  submissionId: number;
  createdAt: string;
  content: string;
  link?: string;
}

export interface UnsubmittedAssignment {
  submitted: false;
}

export type UserAssignmentSubmitDetail = SubmittedAssignment | UnsubmittedAssignment;
