import Page from '../../../shared/ui/Page';
import { ValidationError } from '../../../shared/api/error';
import { Suspense, useState, type CSSProperties } from 'react';
import TopHeader from '../../../shared/ui/TopHeader';
import backIcon from '../../../shared/assets/left-arrow.svg';
import Main from '../../../shared/ui/Main';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import useCreateAssignment from '../hooks/useCreateAssignment';
import { useInputState } from '../../../shared/hooks/useInputState';
import { ASSIGNMENT_TITLE, ASSIGNMENT_CONTENT, ASSIGNMENT_FORM_STEPS } from '../constants';
import { useAttachments } from '../../../shared/modules/files/useAttachments';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import type { SubmissionVisibility } from '../types';
import useFunnel from '../../../shared/hooks/useFunnel';
import AssignmentContentStep from '../components/AssignmentFormSteps/AssignmentContentStep';
import AssignmentScheduleStep from '../components/AssignmentFormSteps/AssignmentScheduleStep';
import AssignmentSubmittersStep from '../components/AssignmentFormSteps/AssignmentSubmittersStep';
import AssignmentVisibilityStep from '../components/AssignmentFormSteps/AssignmentVisibilityStep';
import Loading from '../../../shared/ui/Loading';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import ErrorContent from '../../../shared/ui/ErrorContent';

const backButtonStyle = {
  display: 'grid',
  width: '32px',
  height: '32px',
  padding: 0,
  placeItems: 'center',
  border: 0,
  background: 'transparent',
  cursor: 'pointer',
} satisfies CSSProperties;

export function FunnelPrevButton({ goBack, disabled }: { goBack: () => void; disabled?: boolean }) {
  return (
    <button
      type="button"
      css={backButtonStyle}
      aria-label="뒤로 가기"
      onClick={goBack}
      disabled={disabled}
    >
      <img src={backIcon} alt="" css={{ width: '24px', height: '24px' }} />
    </button>
  );
}

const initialValues = {
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

export default function CreateAssignmentPage() {
  const { studyId } = useIntegerParams(['studyId']);

  const {
    mutate: createAssignment,
    isPending: isSubmitting,
    fieldErrors,
  } = useCreateAssignment({ studyId });

  const { step, history } = useFunnel<AssignmentStep>({
    steps: ASSIGNMENT_FORM_STEPS,
    initial: { step: '내용' },
  });
  const stepNumber = ASSIGNMENT_FORM_STEPS.indexOf(step) + 1;
  const totalSteps = ASSIGNMENT_FORM_STEPS.length;

  const [title, handleTitleChange] = useInputState(initialValues.title, (value, prevValue) => {
    if (value.length > ASSIGNMENT_TITLE.length) return prevValue;
    return value;
  });

  const [content, handleContentChange] = useInputState(
    initialValues.content,
    (value, prevValue) => {
      if (value.length > ASSIGNMENT_CONTENT.length) return prevValue;
      return value;
    },
  );

  const attachments = useAttachments({
    kind: 'image',
    contentType: 'assignment',
    mode: 'create',
  });

  const [submissionMethod, handleSubmissionMethodChange] = useInputState(
    initialValues.submissionMethod,
    (value, prevValue) => {
      if (value.length > ASSIGNMENT_CONTENT.length) return prevValue;
      return value;
    },
  );

  const [closeAt, setCloseAt] = useState<string>(initialValues.closeAt);
  const [remindAts, setRemindAts] = useState<string[]>([]);

  const [submitterIds, setSubmitterIds] = useState<number[]>();
  const [submissionVisibility, setSubmissionVisibility] = useState<SubmissionVisibility>(
    initialValues.submissionVisibility,
  );

  function navigateToErrorStep(error: unknown) {
    if (!(error instanceof ValidationError)) return;

    const errorStep = ASSIGNMENT_FORM_STEPS.find((step) =>
      STEP_FIELDS[step].some((field) => field in error.fieldErrors),
    );
    if (!errorStep) return;

    history.go(ASSIGNMENT_FORM_STEPS.indexOf(errorStep) - ASSIGNMENT_FORM_STEPS.indexOf(step));
  }

  const onSubmit = () => {
    if (isSubmitting || !attachments.isReady || !submitterIds?.length) return;

    createAssignment(
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
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
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
            onSubmit={onSubmit}
          />
        )}
      </Main>
    </Page>
  );
}
