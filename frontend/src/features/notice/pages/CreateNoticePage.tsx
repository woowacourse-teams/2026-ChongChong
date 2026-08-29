import { useNavigate } from 'react-router';
import TopHeader from '../../../shared/ui/TopHeader';
import NoticeForm from '../components/NoticeForm';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import useStudyId from '../../study/hooks/useStudyId';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import noticeQueries from '../queries';
import { NoticeFormValues } from '../types';
import { createNotice } from '../api';

export default function CreateNoticePage() {
  const { studyId } = useStudyId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (values: NoticeFormValues) => createNotice(studyId, values),

    onSuccess: ({ noticeId }) => {
      queryClient.invalidateQueries({
        queryKey: noticeQueries.lists(studyId),
      });

      navigate(`/studies/${studyId}/notices/${noticeId}`);
    },
  });

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>공지</TopHeader.Title>} />
      <Main>
        <NoticeForm
          submitLabel="공지 올리기"
          isSubmitting={createMutation.isPending}
          onSubmit={(values) => createMutation.mutate(values)}
        />
      </Main>
    </Page>
  );
}
