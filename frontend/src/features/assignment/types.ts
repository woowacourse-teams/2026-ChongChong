import type { Attachment } from '../../shared/modules/files/types';

export type SubmissionStatus =
  'NOT_ASSIGNED' | 'NOT_SUBMITTED' | 'SUBMITTED' | 'LATE_SUBMITTED' | 'MISSING';

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
  attachments?: Attachment[];
  id: number;
  title: string;
  content: string;
  submissionMethod: string;
  closeAt: string;
  submissionTarget: SubmissionTarget;
  submissionVisibility: SubmissionVisibility;
}

export type SubmissionTarget = 'MEMBERS_ONLY' | 'MEMBERS_AND_LEADER';
export type SubmissionVisibility = 'LEADER_ONLY' | 'ALL_STUDY_MEMBERS';

export type AssignmentValue = Omit<AssignmentDetail, 'id' | 'attachments'> & {
  remindAts?: string[] | null;
  attachmentFileIds?: number[];
};

export type UpdateAssignmentValue = Partial<AssignmentValue>;

interface SubmissionBase {
  id: number;
  name: string;
  profileImage: string | null;
  createdAt: string;
}

export interface Submission extends SubmissionBase {
  submissionStatus: 'SUBMITTED' | 'LATE_SUBMITTED';
}

export interface SubmissionListResponse {
  submissions: Submission[];
}

export interface SubmissionDetail extends SubmissionBase {
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

export interface LateSubmittedAssignment {
  submissionStatus: 'LATE_SUBMITTED';
  submissionId: number;
  createdAt: string;
  content?: string;
  link?: string;
}

export interface UnsubmittedAssignment {
  submissionStatus: 'NOT_SUBMITTED';
  submissionId: number;
}

export interface MissingAssignment {
  submissionStatus: 'MISSING';
  submissionId: number;
}

export interface NotAssignedAssignment {
  submissionStatus: 'NOT_ASSIGNED';
}

export type UserAssignmentSubmitDetail =
  | SubmittedAssignment
  | LateSubmittedAssignment
  | UnsubmittedAssignment
  | MissingAssignment
  | NotAssignedAssignment;

export type CompletedAssignment = SubmittedAssignment | LateSubmittedAssignment;
