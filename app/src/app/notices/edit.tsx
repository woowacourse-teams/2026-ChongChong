import { router, useLocalSearchParams, useNavigation } from 'expo-router';
import {
  type NavigationAction,
  usePreventRemove,
} from 'expo-router/react-navigation';
import { useRef, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { ImageAttachments } from '../../features/notices/ImageAttachments';
import { noticeLayout as n } from '../../features/notices/layout';
import type { NoticeImage } from '../../features/notices/model';
import { NoticeIcon } from '../../features/notices/NoticeIcon';
import { useNotices } from '../../features/notices/NoticeProvider';
import { ReminderSheet } from '../../features/notices/ReminderSheet';
import { AppHeader } from '../../ui/AppHeader';
import { ConfirmDialog } from '../../ui/feedback';
import { AppText, Button, Field } from '../../ui/primitives';
import { Screen } from '../../ui/Screen';
import { tokens as t } from '../../ui/tokens';
export default function NoticeEditScreen() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const { notices, saveNotice } = useNotices();
  const { selectedStudy } = useEntryScenario();
  const navigation = useNavigation();
  const pendingAction = useRef<NavigationAction | null>(null);
  const allowLeave = useRef(false);
  const existing = notices.find((item) => item.id === id);
  const [title, setTitle] = useState(existing?.title ?? '');
  const [body, setBody] = useState(existing?.body ?? '');
  const [images, setImages] = useState<readonly NoticeImage[]>(
    existing?.images ?? [],
  );
  const [reminders, setReminders] = useState<readonly string[]>(
    existing?.reminders ?? [],
  );
  const [sheet, setSheet] = useState(false);
  const [discard, setDiscard] = useState(false);
  const valid = !!title.trim() && !!body.trim();
  const dirty =
    title !== (existing?.title ?? '') ||
    body !== (existing?.body ?? '') ||
    JSON.stringify(images) !== JSON.stringify(existing?.images ?? []) ||
    JSON.stringify(reminders) !== JSON.stringify(existing?.reminders ?? []);
  usePreventRemove(dirty, ({ data }) => {
    if (allowLeave.current) {
      navigation.dispatch(data.action);
      return;
    }
    pendingAction.current = data.action;
    setDiscard(true);
  });
  const back = () =>
    router.canGoBack() ? router.back() : router.replace('/study/notices');
  const formatReminder = (value: string) =>
    `${Number(value.slice(0, 4))}년 ${Number(value.slice(5, 7))}월 ${Number(value.slice(8, 10))}일 ${value.slice(11)}`;
  return (
    <View style={{ flex: 1 }}>
      <AppHeader title="공지" onBack={back} />
      <Screen>
        {selectedStudy?.role !== 'leader' || (id && !existing) ? (
          <AppText>공지를 작성하거나 수정할 수 없어요</AppText>
        ) : (
          <>
            <Field
              label="제목"
              required
              placeholder="제목을 입력해주세요"
              value={title}
              onChangeText={setTitle}
            />
            <View style={styles.group}>
              <Field
                label="내용"
                required
                placeholder="설명을 입력해주세요"
                value={body}
                onChangeText={setBody}
                multiline
                style={styles.body}
              />
              <AppText variant="caption" tone="tertiary">
                스터디원은 끝까지 읽어야 읽음 처리를 할 수 있어요
              </AppText>
            </View>
            <View style={styles.group}>
              <AppText variant="large">리마인드 시각</AppText>
              {reminders.map((value) => (
                <View key={value} style={styles.reminder}>
                  <AppText style={{ flex: 1 }}>{formatReminder(value)}</AppText>
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={`${formatReminder(value)} 제거`}
                    onPress={() =>
                      setReminders(reminders.filter((item) => item !== value))
                    }
                  >
                    <NoticeIcon name="close" />
                  </Pressable>
                </View>
              ))}
              <Pressable
                accessibilityRole="button"
                onPress={() => setSheet(true)}
                style={styles.add}
              >
                <NoticeIcon name="plus" />
                <AppText>리마인드 등록하기</AppText>
              </Pressable>
              <AppText variant="caption" tone="tertiary">
                설정한 시각마다 읽지 않은 스터디원에게 알림을 보내드릴게요
              </AppText>
            </View>
            <ImageAttachments images={images} onChange={setImages} />
            <Button
              label={existing ? '공지 수정하기' : '공지 올리기'}
              disabled={!valid}
              onPress={() => {
                const savedId = saveNotice(
                  { title: title.trim(), body: body.trim(), images, reminders },
                  id,
                );
                if (savedId) {
                  allowLeave.current = true;
                  router.dismissTo({
                    pathname: '/notices/[id]',
                    params: { id: savedId },
                  });
                }
              }}
            />
          </>
        )}
      </Screen>
      {sheet && (
        <ReminderSheet
          onClose={() => setSheet(false)}
          onSave={(value) => {
            setReminders((current) =>
              current.includes(value) ? current : [...current, value],
            );
            setSheet(false);
          }}
        />
      )}
      <ConfirmDialog
        visible={discard}
        title="작성을 그만둘까요?"
        description="저장하지 않은 내용은 사라져요."
        confirmLabel="나가기"
        onCancel={() => setDiscard(false)}
        onConfirm={() => {
          allowLeave.current = true;
          setDiscard(false);
          if (pendingAction.current) navigation.dispatch(pendingAction.current);
          else back();
        }}
      />
    </View>
  );
}
const styles = StyleSheet.create({
  group: { gap: t.space.sm },
  body: { height: n.bodyFieldHeight, textAlignVertical: 'top' },
  reminder: {
    minHeight: t.size.control,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.md,
    paddingHorizontal: t.space.lg,
    flexDirection: 'row',
    alignItems: 'center',
  },
  add: {
    height: t.size.control,
    backgroundColor: t.color.subtle,
    borderRadius: t.radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: t.space.xs,
  },
});
