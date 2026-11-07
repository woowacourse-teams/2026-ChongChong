import { Suspense, useState } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import { ValidationError } from '../../../shared/api/error';
import { useInputState } from '../../../shared/hooks/useInputState';
import { useAttachments } from '../../../shared/modules/files/useAttachments';
import type { Attachment } from '../../../shared/modules/files/types';
import useFunnel from '../../../shared/hooks/useFunnel';
import Loading from '../../../shared/ui/Loading';
import ErrorContent from '../../../shared/ui/ErrorContent';
import { ASSIGNMENT_TITLE, ASSIGNMENT_CONTENT, ASSIGNMENT_FORM_STEPS } from '../constants';
import type { CreateAssignmentFormValues, SubmissionVisibility } from '../types';
import AssignmentContentStep from './AssignmentFormSteps/AssignmentContentStep';
import AssignmentScheduleStep from './AssignmentFormSteps/AssignmentScheduleStep';
import AssignmentSubmittersStep from './AssignmentFormSteps/AssignmentSubmittersStep';
import AssignmentVisibilityStep from './AssignmentFormSteps/AssignmentVisibilityStep';

const emptyValues = {
  title: '',
  content: '',
  submissionMethod: '',
  closeAt: '',
  submissionVisibility: 'ALL_STUDY_MEMBERS',
} as const;

type AssignmentStep = (typeof ASSIGNMENT_FORM_STEPS)[number];

const STEP_FIELDS = {
  내용: ['title', 'content', 'submissionMethod', 'attachmentFileIds'],
  일정: ['closeAt', 'remindAts'],
  '제출 대상': ['submitterIds'],
  공개여부: ['submissionVisibility'],
} satisfies Record<AssignmentStep, string[]>;

interface AssignmentFormFunnelProps {
  studyId: number;
  mode: 'create' | 'edit';
  initialValues?: Partial<CreateAssignmentFormValues>;
  initialAttachments?: Attachment[];
  isSubmitting: boolean;
  fieldErrors: Record<string, string>;
  onSubmit: (
    values: CreateAssignmentFormValues,
    options: { onError: (error: Error) => void },
  ) => void;
}

export default function AssignmentFormFunnel({
  studyId,
  mode,
  initialValues = emptyValues,
  initialAttachments,
  isSubmitting,
  fieldErrors,
  onSubmit,
}: AssignmentFormFunnelProps) {
  const { step, history } = useFunnel<AssignmentStep>({
    steps: ASSIGNMENT_FORM_STEPS,
    initial: { step: '내용' },
  });
  const stepNumber = ASSIGNMENT_FORM_STEPS.indexOf(step) + 1;
  const totalSteps = ASSIGNMENT_FORM_STEPS.length;

  const [title, handleTitleChange] = useInputState(
    initialValues.title ?? emptyValues.title,
    (value, prevValue) => {
      if (value.length > ASSIGNMENT_TITLE.length) return prevValue;
      return value;
    },
  );

  const [content, handleContentChange] = useInputState(
    initialValues.content ?? emptyValues.content,
    (value, prevValue) => {
      if (value.length > ASSIGNMENT_CONTENT.length) return prevValue;
      return value;
    },
  );

  const attachments = useAttachments({
    kind: 'image',
    contentType: 'assignment',
    mode,
    initialAttachments,
  });

  const [submissionMethod, handleSubmissionMethodChange] = useInputState(
    initialValues.submissionMethod ?? emptyValues.submissionMethod,
    (value, prevValue) => {
      if (value.length > ASSIGNMENT_CONTENT.length) return prevValue;
      return value;
    },
  );

  const [closeAt, setCloseAt] = useState<string>(initialValues.closeAt ?? emptyValues.closeAt);
  const [remindAts, setRemindAts] = useState<string[]>(initialValues.remindAts ?? []);

  const [submitterIds, setSubmitterIds] = useState<number[] | undefined>(
    initialValues.submitterIds,
  );
  const [submissionVisibility, setSubmissionVisibility] = useState<SubmissionVisibility>(
    initialValues.submissionVisibility ?? emptyValues.submissionVisibility,
  );

  function navigateToErrorStep(error: unknown) {
    if (!(error instanceof ValidationError)) return;

    const errorStep = ASSIGNMENT_FORM_STEPS.find((step) =>
      STEP_FIELDS[step].some((field) => field in error.fieldErrors),
    );
    if (!errorStep) return;

    const delta = ASSIGNMENT_FORM_STEPS.indexOf(errorStep) - ASSIGNMENT_FORM_STEPS.indexOf(step);
    if (delta < 0) history.go(delta);
  }

  const handleSubmit = () => {
    if (isSubmitting || !attachments.isReady || !submitterIds?.length) return;

    onSubmit(
      {
        title,
        content,
        submissionMethod,
        closeAt,
        submitterIds,
        submissionVisibility,
        remindAts,
        attachmentFileIds: attachments.attachmentFileIds,
      },
      {
        onError: navigateToErrorStep,
      },
    );
  };

  return (
    <>
      {step === '내용' && (
        <AssignmentContentStep
          step={stepNumber}
          totalSteps={totalSteps}
          title={title}
          content={content}
          submissionMethod={submissionMethod}
          handleTitleChange={handleTitleChange}
          handleContentChange={handleContentChange}
          handleSubmissionMethodChange={handleSubmissionMethodChange}
          attachments={attachments}
          fieldErrors={fieldErrors}
          onNext={() => history.push('일정')}
        />
      )}
      {step === '일정' && (
        <AssignmentScheduleStep
          step={stepNumber}
          totalSteps={totalSteps}
          closeAt={closeAt}
          remindAts={remindAts}
          fieldErrors={fieldErrors}
          setCloseAt={setCloseAt}
          setRemindAts={setRemindAts}
          onNext={() => history.push('제출 대상')}
        />
      )}
      {step === '제출 대상' && (
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense fallback={<Loading />}>
            <AssignmentSubmittersStep
              step={stepNumber}
              totalSteps={totalSteps}
              studyId={studyId}
              submitterIds={submitterIds}
              isSubmitting={isSubmitting}
              errorText={fieldErrors.submitterIds}
              setSubmitterIds={setSubmitterIds}
              onNext={(ids) => {
                setSubmitterIds(ids);
                history.push('공개여부');
              }}
            />
          </Suspense>
        </ErrorBoundary>
      )}
      {step === '공개여부' && (
        <AssignmentVisibilityStep
          step={stepNumber}
          totalSteps={totalSteps}
          submissionVisibility={submissionVisibility}
          changeSubmissionVisibility={setSubmissionVisibility}
          isSubmitting={isSubmitting}
          errorText={fieldErrors.submissionVisibility}
          onSubmit={handleSubmit}
          submitLabel={mode === 'edit' ? '과제 수정하기' : '과제 만들기'}
        />
      )}
    </>
  );
}
