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
      queryClient.invalidateQueries({ queryKey: studyQueries.all() });
      queryClient.invalidateQueries({ queryKey: commentQueries.all() });
    },
  });
}
