import { infiniteQueryOptions, queryOptions } from '@tanstack/react-query';
import {
  fetchAssignmentList,
  fetchAssignmentSubmitStatus,
  fetchAssignment,
  fetchAssignmentSubmission,
  fetchAssignmentSubmissionDetail,
  fetchMyAssignmentSubmission,
} from './api';

const assignmentQueries = {
  all: () => ['studies'] as const,

  study: (studyId: number) => [...assignmentQueries.all(), studyId] as const,

  lists: (studyId: number) => [...assignmentQueries.study(studyId), 'assignments'] as const,

  list: (studyId: number) =>
    infiniteQueryOptions({
      queryKey: assignmentQueries.lists(studyId),
      queryFn: ({ pageParam }: { pageParam: number | null }) =>
        fetchAssignmentList(studyId, pageParam ?? undefined),
      initialPageParam: null,
      getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.nextCursor : undefined),
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

  mySubmission: (studyId: number, assignmentId: number) =>
    queryOptions({
      queryKey: [...assignmentQueries.lists(studyId), assignmentId, 'my-submission'],
      queryFn: () => fetchMyAssignmentSubmission(studyId, assignmentId),
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
