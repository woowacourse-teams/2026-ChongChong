import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';
import { useAssignments } from '../../features/assignments/AssignmentProvider';
import { FileAttachments } from '../../features/assignments/FileAttachments';
import { validSubmissionLink } from '../../features/assignments/model';
import { assignmentStyles as s } from '../../features/assignments/styles';
import { useDraftGuard } from '../../features/assignments/useDraftGuard';
import type { NoticeImage } from '../../features/notices/model';
import { isFutureReminder } from '../../features/notices/previewClock';
import { AppHeader } from '../../ui/AppHeader';
import { ConfirmDialog } from '../../ui/feedback';
import { AppText, Button, Field } from '../../ui/primitives';
import { Screen } from '../../ui/Screen';
export default function AssignmentSubmit() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { assignments, submit } = useAssignments();
  const assignment = assignments.find((item) => item.id === id);
  const self = assignment?.members.find((person) => person.id === 'self');
  const existing = self?.submission;
  const [body, setBody] = useState(existing?.body ?? '');
  const [link, setLink] = useState(existing?.link ?? '');
  const [files, setFiles] = useState<readonly NoticeImage[]>(
    existing?.files ?? [],
  );
  const guard = useDraftGuard(
    body !== (existing?.body ?? '') ||
      link !== (existing?.link ?? '') ||
      JSON.stringify(files) !== JSON.stringify(existing?.files ?? []),
  );
  const validLink = validSubmissionLink(link);
  return (
    <View style={s.page}>
      <AppHeader
        title="과제"
        onBack={() =>
          router.canGoBack()
            ? router.back()
            : router.replace('/study/assignments')
        }
      />
      <Screen>
        {!assignment || !self || !isFutureReminder(assignment.deadline) ? (
          <AppText>제출할 수 없는 과제예요</AppText>
        ) : (
          <>
            <Field
              label="내용"
              required
              placeholder="설명을 입력해주세요"
              value={body}
              onChangeText={setBody}
              multiline
              style={s.bodyField}
            />
            <Field
              label="링크"
              placeholder="https://"
              value={link}
              onChangeText={setLink}
              autoCapitalize="none"
              keyboardType="url"
              {...(!validLink ? { error: '주소가 올바르지 않아요' } : {})}
            />
            <FileAttachments files={files} onChange={setFiles} />
            <Button
              label={existing ? '제출물 수정하기' : '과제 제출하기'}
              disabled={!body.trim() || !validLink}
              onPress={() => {
                if (
                  submit(id, { body: body.trim(), link: link.trim(), files })
                ) {
                  guard.allowLeave();
                  router.dismissTo({
                    pathname: '/assignments/[id]',
                    params: { id, tab: 'detail' },
                  });
                }
              }}
            />
          </>
        )}
      </Screen>
      <ConfirmDialog
        visible={guard.discard}
        title="작성을 그만둘까요?"
        description="저장하지 않은 내용은 사라져요."
        confirmLabel="나가기"
        onCancel={guard.cancel}
        onConfirm={guard.confirm}
      />
    </View>
  );
}
