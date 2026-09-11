import TopHeader from '../../../shared/ui/TopHeader';
import NoticeForm from '../components/NoticeForm';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import { useNavigate } from 'react-router';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import noticeQueries from '../queries';
import { useSuspenseQuery } from '@tanstack/react-query';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import { UpdateNoticeValue } from '../types';
import { updateNotice } from '../api';
import { Suspense, useMemo } from 'react';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import Loading from '../../../shared/ui/Loading';
import ErrorContent from '../../../shared/ui/ErrorContent';

export default function EditNoticePage() {
  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>공지</TopHeader.Title>} />
      <Main>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense fallback={<Loading />}>
            <EditNoticePage.Content />
          </Suspense>
        </ErrorBoundary>
      </Main>
    </Page>
  );
}

EditNoticePage.Content = function Content() {
  const { studyId, noticeId } = useIntegerParams(['studyId', 'noticeId']);
  const { data: notice } = useSuspenseQuery(noticeQueries.detail(studyId, noticeId));
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (values: UpdateNoticeValue) => updateNotice(studyId, noticeId, values),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: noticeQueries.lists(studyId),
      });

      queryClient.invalidateQueries({
        queryKey: noticeQueries.detail(studyId, noticeId).queryKey,
      });

      navigate(`/studies/${studyId}/notices/${noticeId}`);
    },

    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast status="Error" message={error.message} />);
    },
  });

  const fieldErrors = useMemo(() => {
    return error instanceof ValidationError ? error.fieldErrors : {};
  }, [error]);

  return (
    <NoticeForm
      submitLabel="수정하기"
      isSubmitting={isPending}
      fieldErrors={fieldErrors}
      onSubmit={mutate}
      initialValues={{
        title: notice.title,
        content: notice.content,
      }}
    />
  );
};
