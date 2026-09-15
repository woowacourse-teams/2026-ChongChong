import { useNavigate } from 'react-router';
import TopHeader from '../../../shared/ui/TopHeader';
import NoticeForm from '../components/NoticeForm';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import noticeQueries from '../queries';
import { NoticeFormValues } from '../types';
import { createNotice } from '../api';
import { useMemo } from 'react';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

export default function CreateNoticePage() {
  const { studyId } = useIntegerParams(['studyId']);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (values: NoticeFormValues) => createNotice(studyId, values),

    onSuccess: ({ noticeId }) => {
      queryClient.invalidateQueries({
        queryKey: noticeQueries.lists(studyId),
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
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>공지</TopHeader.Title>} />
      <Main>
        <NoticeForm
          submitLabel="공지 올리기"
          isSubmitting={isPending}
          fieldErrors={fieldErrors}
          onSubmit={mutate}
        />
      </Main>
    </Page>
  );
}
