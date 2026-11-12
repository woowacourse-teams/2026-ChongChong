import { useMutation, useQueryClient } from '@tanstack/react-query';
import { uploadAttachment } from '../../../shared/modules/files/api';
import { validateAttachments } from '../../../shared/modules/files/validation';
import { resetProfileImage, updateProfileImage } from '../api';
import studyQueries from '../../study/queries';
import commentQueries from '../../../shared/modules/comments/queries';
import userQueries from '../queries';

export default function useUpdateProfileImage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (file: File | null) => {
      if (file === null) {
        await resetProfileImage();
        return null;
      }
      const validation = validateAttachments([file], 0, 'image', { includeFileName: false });
      if (validation) throw new Error(validation.message);
      const fileId = await uploadAttachment(file);
      return updateProfileImage(fileId);
    },
    retry: false,
    onSuccess: (profile) => {
      queryClient.setQueryData(
        userQueries.profile().queryKey,
        (previous) => profile ?? (previous ? { ...previous, profileImageUrl: null } : previous),
      );
      // 참여 스터디의 작성자·멤버 이미지도 다음 조회부터 새 프로필을 사용한다.
      void queryClient.invalidateQueries({ queryKey: studyQueries.all() });
      void queryClient.invalidateQueries({ queryKey: commentQueries.all() });
    },
  });
}
