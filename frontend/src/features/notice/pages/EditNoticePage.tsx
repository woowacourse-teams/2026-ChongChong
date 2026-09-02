import TopHeader from '../../../shared/ui/TopHeader';
import NoticeForm from '../components/NoticeForm';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import { useParams, useNavigate } from 'react-router';
import noticeQueries from '../queries';
import { useSuspenseQuery } from '@tanstack/react-query';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import { UpdateNoticeValue } from '../types';
import { updateNotice } from '../api';

export default function EditNoticePage() {
  const { studyId, noticeId } = useParams();
  const { data: notice } = useSuspenseQuery(
    noticeQueries.detail(Number(studyId), Number(noticeId)),
  );
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const updateMutation = useMutation({
    mutationFn: (values: UpdateNoticeValue) =>
      updateNotice(Number(studyId), Number(noticeId), values),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: noticeQueries.lists(Number(studyId)),
      });

      queryClient.invalidateQueries({
        queryKey: noticeQueries.detail(Number(studyId), Number(noticeId)).queryKey,
      });

      navigate(`/studies/${studyId}/notices/${noticeId}`);
    },
  });

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>공지</TopHeader.Title>} />

      <Main>
        <NoticeForm
          submitLabel="수정하기"
          isSubmitting={updateMutation.isPending}
          error={updateMutation.error}
          onSubmit={updateMutation.mutate}
          initialValues={{
            title: notice.title,
            content: notice.content,
          }}
        />
      </Main>
    </Page>
  );
}
