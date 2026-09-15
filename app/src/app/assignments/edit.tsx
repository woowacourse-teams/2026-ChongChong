import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Pressable, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';
import { useAssignments } from '../../features/assignments/AssignmentProvider';
import { AssignmentSchedule } from '../../features/assignments/AssignmentSchedule';
import type { AssignmentDraft } from '../../features/assignments/model';
import { assignmentStyles as s } from '../../features/assignments/styles';
import { useDraftGuard } from '../../features/assignments/useDraftGuard';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { ImageAttachments } from '../../features/notices/ImageAttachments';
import { isFutureReminder } from '../../features/notices/previewClock';
import { AppHeader } from '../../ui/AppHeader';
import { ConfirmDialog } from '../../ui/feedback';
import { AppText, Button, Field } from '../../ui/primitives';
import { Screen } from '../../ui/Screen';
import { tokens as t } from '../../ui/tokens';

export default function AssignmentEdit() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const { assignments, saveAssignment } = useAssignments();
  const { selectedStudy } = useEntryScenario();
  const existing = assignments.find((item) => item.id === id);
  const initial: AssignmentDraft = existing ?? {
    title: '',
    body: '',
    method: '',
    deadline: '',
    visibility: 'public',
    leaderParticipates: true,
    reminders: [],
    images: [],
  };
  const [draft, setDraft] = useState<AssignmentDraft>(initial);
  const patch = (value: Partial<AssignmentDraft>) =>
    setDraft((current) => ({ ...current, ...value }));
  const guard = useDraftGuard(
    JSON.stringify(draft) !== JSON.stringify(initial),
  );
  const valid =
    !!draft.title.trim() &&
    !!draft.body.trim() &&
    !!draft.method.trim() &&
    isFutureReminder(draft.deadline);
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
        {selectedStudy?.role !== 'leader' || (id && !existing) ? (
          <AppText>과제를 작성하거나 수정할 수 없어요</AppText>
        ) : (
          <>
            <Field
              label="제목"
              required
              placeholder="제목을 입력해주세요"
              value={draft.title}
              onChangeText={(title) => patch({ title })}
            />
            <Field
              label="내용"
              required
              placeholder="설명을 입력해주세요"
              value={draft.body}
              onChangeText={(body) => patch({ body })}
              multiline
              style={s.bodyField}
            />
            <Field
              label="제출 방법"
              required
              placeholder="링크를 제출해주세요"
              value={draft.method}
              onChangeText={(method) => patch({ method })}
            />
            <AssignmentSchedule
              deadline={draft.deadline}
              reminders={draft.reminders}
              onDeadline={(deadline) => patch({ deadline })}
              onReminders={(reminders) => patch({ reminders })}
            >
              <View style={s.group}>
                <AppText variant="large">
                  제출물 공개 여부 <AppText style={s.green}>*</AppText>
                </AppText>
                <View style={[s.row, { gap: t.space.lg }]}>
                  {(['public', 'private'] as const).map((value) => (
                    <Pressable
                      key={value}
                      accessibilityRole="radio"
                      accessibilityState={{
                        checked: draft.visibility === value,
                      }}
                      onPress={() => patch({ visibility: value })}
                      style={s.row}
                    >
                      <View
                        style={{
                          width: 20,
                          height: 20,
                          borderRadius: 10,
                          borderWidth: 2,
                          borderColor:
                            draft.visibility === value
                              ? t.color.brand
                              : t.color.placeholder,
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        {draft.visibility === value && (
                          <View
                            style={{
                              width: 10,
                              height: 10,
                              borderRadius: 5,
                              backgroundColor: t.color.brand,
                            }}
                          />
                        )}
                      </View>
                      <AppText>
                        {value === 'public' ? '공개' : '비공개'}
                      </AppText>
                    </Pressable>
                  ))}
                </View>
                <AppText variant="caption" tone="tertiary">
                  공개로 설정하면, 스터디원들끼리 서로의 제출물을 확인할 수
                  있어요
                </AppText>
              </View>
              <View style={s.group}>
                <AppText variant="large">
                  리더 제출 여부 <AppText style={s.green}>*</AppText>
                </AppText>
                <Pressable
                  accessibilityRole="checkbox"
                  accessibilityState={{ checked: draft.leaderParticipates }}
                  onPress={() =>
                    patch({ leaderParticipates: !draft.leaderParticipates })
                  }
                  style={s.row}
                >
                  <View
                    style={{
                      width: 20,
                      height: 20,
                      borderRadius: 3,
                      borderWidth: 1,
                      borderColor: t.color.brand,
                      backgroundColor: draft.leaderParticipates
                        ? t.color.brand
                        : t.color.background,
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    {draft.leaderParticipates && (
                      <Svg
                        width={16}
                        height={16}
                        viewBox="0 0 18 18"
                        fill="none"
                        aria-hidden
                      >
                        <Path
                          d="M15 4.5L6.75 12.75L3 9"
                          stroke={t.color.onBrand}
                          strokeWidth={1.625}
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </Svg>
                    )}
                  </View>
                  <AppText>나도 과제를 제출할게요</AppText>
                </Pressable>
                <AppText variant="caption" tone="tertiary">
                  체크하지 않으면 리더는 과제 제출 대상에서 제외돼요
                </AppText>
              </View>
            </AssignmentSchedule>
            <ImageAttachments
              images={draft.images}
              onChange={(images) => patch({ images })}
            />
            <Button
              label={existing ? '과제 수정하기' : '과제 올리기'}
              disabled={!valid}
              onPress={() => {
                const savedId = saveAssignment(
                  {
                    ...draft,
                    title: draft.title.trim(),
                    body: draft.body.trim(),
                    method: draft.method.trim(),
                  },
                  id,
                );
                if (savedId) {
                  guard.allowLeave();
                  router.dismissTo({
                    pathname: '/assignments/[id]',
                    params: { id: savedId },
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
