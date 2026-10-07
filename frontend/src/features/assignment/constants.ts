import type { Variant } from '../../shared/ui/Badge';
import type { SubmissionStatus } from './types';

export const ASSIGNMENT_TITLE = {
  length: 100,
};

export const ASSIGNMENT_CONTENT = {
  length: 10000,
};

export const ASSIGNMENT_SUBMISSION_CONTENT = {
  length: 10000,
};

export const ASSIGNMENT_SUBMISSION_LINK = {
  length: 10000,
};

export const submissionStatusBadge = {
  SUBMITTED: { variant: 'brandSolid', label: '제출 완료' },
  LATE_SUBMITTED: { variant: 'brandSolid', label: '지각 제출' },
  NOT_SUBMITTED: { variant: 'brandOutline', label: '미제출' },
  MISSING: { variant: 'brandOutline', label: '마감 후 미제출' },
  NOT_ASSIGNED: { variant: 'neutralSolid', label: '제출 대상 아님' },
} satisfies Record<SubmissionStatus, { variant: Variant; label: string }>;

export const visibilityOptions = [
  { label: '공개', value: 'ALL_STUDY_MEMBERS' },
  { label: '비공개', value: 'LEADER_ONLY' },
] as const;
