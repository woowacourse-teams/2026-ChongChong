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
  NOT_SUBMITTED: { variant: 'brandOutline', label: '미제출' },
  NOT_ASSIGNED: { variant: 'neutralSolid', label: '제출 대상 아님' },
} satisfies Record<SubmissionStatus, { variant: Variant; label: string }>;
