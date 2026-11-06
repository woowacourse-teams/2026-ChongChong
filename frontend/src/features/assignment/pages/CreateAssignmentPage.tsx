import Page from '../../../shared/ui/Page';
import { ValidationError } from '../../../shared/api/error';
import { Suspense, useState, type CSSProperties } from 'react';
import TopHeader from '../../../shared/ui/TopHeader';
import backIcon from '../../../shared/assets/left-arrow.svg';
import Main from '../../../shared/ui/Main';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import useCreateAssignment from '../hooks/useCreateAssignment';
import { useInputState } from '../../../shared/hooks/useInputState';
import { ASSIGNMENT_TITLE, ASSIGNMENT_CONTENT } from '../constants';
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

type AssignmentStep = '내용' | '일정' | '제출 대상' | '공개여부';

export default function CreateAssignmentPage() {
  const { studyId } = useIntegerParams(['studyId']);

  const {
    mutate: createAssignment,
    isPending: isSubmitting,
    fieldErrors,
  } = useCreateAssignment({ studyId });

  const { step, history } = useFunnel<AssignmentStep>({ initial: { step: '내용' } });

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
        onError: (error) => {
          if (!(error instanceof ValidationError)) return;
          const stepFields = [
            ['title', 'content', 'submissionMethod', 'attachmentFileIds'],
            ['closeAt', 'remindAts'],
            ['submitterIds'],
            ['submissionVisibility'],
          ];
          const errorStepIndex = stepFields.findIndex((fields) =>
            fields.some((field) => field in error.fieldErrors),
          );
          if (errorStepIndex >= 0) history.go(errorStepIndex - 3);
        },
      },
    );
  };

  return (
    <Page>
      <TopHeader
        css={{
          gridTemplateColumns: '24px 1fr 24px',
          '& > div:nth-of-type(2)': { textAlign: 'left' },
          '& button': { width: 24, height: 24 },
        }}
        left={
          step === '내용' ? (
            <PrevButton />
          ) : (
            <FunnelPrevButton goBack={() => history.back()} disabled={isSubmitting} />
          )
        }
        middle={<TopHeader.Title>과제</TopHeader.Title>}
      />
      <Main
        css={{
          paddingTop: step === '내용' ? 16 : 24,
          paddingBottom: 'max(20px, var(--safe-bottom))',
        }}
      >
        {step === '내용' && (
          <AssignmentContentStep
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
