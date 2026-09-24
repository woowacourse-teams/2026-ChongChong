export type SubmissionStatus = 'NOT_ASSIGNED' | 'NOT_SUBMITTED' | 'SUBMITTED';

interface AssignmentSummaryBase {
  id: number;
  title: string;
  content: string;
  submissionMethod: string;
  closeAt: string;
  submissionStatus: SubmissionStatus;
}

export interface LeaderAssignmentSummary extends AssignmentSummaryBase {
  memberCount: number;
  completeCount: number;
  remindAt?: string;
  isComplete: boolean;
}

export type MemberAssignmentSummary = AssignmentSummaryBase;

export type Assignment = LeaderAssignmentSummary | MemberAssignmentSummary;

export interface AssignmentListResponse {
  nextCursor: number | null;
  hasNext: boolean;
  assignments: Assignment[];
}

export interface Member {
  id: number;
  name: string;
  profileImage: string | null;
}

export interface IncompleteMember extends Member {
  lastRemindAt: string | null;
}

export interface AssignmentSubmitStatus {
  id: number;
  memberCount: number;
  completeCount: number;
  incompleteCount: number;
  remindAt: string | null;
  completeMembers: Member[];
  incompleteMembers: IncompleteMember[];
}

export interface AssignmentDetail {
  id: number;
  title: string;
  content: string;
  submissionMethod: string;
  closeAt: string;
  submissionTarget: SubmissionTarget;
}

export type SubmissionTarget = 'MEMBERS_ONLY' | 'MEMBERS_AND_LEADER';

export type AssignmentValue = Omit<AssignmentDetail, 'id'> & {
  remindAts?: string[] | null;
};

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
  link: string | null;
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
  submissionStatus: 'SUBMITTED';
  submissionId: number;
  createdAt: string;
  content?: string;
  link?: string;
}

export interface UnsubmittedAssignment {
  submissionStatus: 'NOT_SUBMITTED';
  submissionId: number;
}

export interface NotAssignedAssignment {
  submissionStatus: 'NOT_ASSIGNED';
}

export type UserAssignmentSubmitDetail =
  | SubmittedAssignment
  | UnsubmittedAssignment
  | NotAssignedAssignment;
