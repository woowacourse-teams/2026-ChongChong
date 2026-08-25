import { queryOptions } from '@tanstack/react-query';
import {
  fetchAssignmentList,
  fetchAssignmentSubmitStatus,
  fetchAssignment,
  fetchAssignmentSubmission,
  fetchAssignmentSubmissionDetail,
} from './api';

const assignmentQueries = {
  all: () => ['studies'] as const,

  study: (studyId: number) => [...assignmentQueries.all(), studyId] as const,

  lists: (studyId: number) => [...assignmentQueries.study(studyId), 'assignments'] as const,

  list: (studyId: number) =>
    queryOptions({
      queryKey: assignmentQueries.lists(studyId),
      queryFn: () => fetchAssignmentList(studyId),
    }),

  detail: (studyId: number, assignmentId: number) =>
    queryOptions({
      queryKey: [...assignmentQueries.lists(studyId), assignmentId, 'detail'],
      queryFn: () => fetchAssignment(studyId, assignmentId),
    }),

  submitStatus: (studyId: number, assignmentId: number) =>
    queryOptions({
      queryKey: [...assignmentQueries.lists(studyId), assignmentId, 'submit-status'],
      queryFn: () => fetchAssignmentSubmitStatus(studyId, assignmentId),
    }),

  submissions: (studyId: number, assignmentId: number) =>
    queryOptions({
      queryKey: [...assignmentQueries.lists(studyId), assignmentId, 'submissions'],
      queryFn: () => fetchAssignmentSubmission(studyId, assignmentId),
    }),

  submissionDetail: (studyId: number, assignmentId: number, submissionId: number) =>
    queryOptions({
      queryKey: [
        ...assignmentQueries.lists(studyId),
        assignmentId,
        'submissions',
        submissionId,
        'detail',
      ],
      queryFn: () => fetchAssignmentSubmissionDetail(studyId, assignmentId, submissionId),
    }),
};

export default assignmentQueries;
