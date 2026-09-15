import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AssignmentDetail } from '../../features/assignments/AssignmentDetail';
import { useAssignments } from '../../features/assignments/AssignmentProvider';
import { AssignmentSummary } from '../../features/assignments/AssignmentSummary';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { noticeLayout as n } from '../../features/notices/layout';
import { NoticeIcon } from '../../features/notices/NoticeIcon';
import { AppHeader } from '../../ui/AppHeader';
import { ConfirmDialog } from '../../ui/feedback';
import { AppText, Button } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
export default function AssignmentDetailScreen() {
  const { id, tab: initialTab } = useLocalSearchParams<{
    id: string;
    tab?: string;
  }>();
  const { assignments, deleteAssignment } = useAssignments();
  const { selectedStudy } = useEntryScenario();
  const assignment = assignments.find((item) => item.id === id);
  const leader = selectedStudy?.role === 'leader';
  const [tab, setTab] = useState<'summary' | 'detail'>(
    initialTab === 'detail' ? 'detail' : 'summary',
  );
  useEffect(() => {
    setTab(initialTab === 'detail' ? 'detail' : 'summary');
  }, [initialTab]);
  const [menu, setMenu] = useState(false);
  const [confirm, setConfirm] = useState(false);
  const back = () =>
    router.canGoBack() ? router.back() : router.replace('/study/assignments');
  return (
    <SafeAreaView edges={['bottom', 'left', 'right']} style={styles.page}>
      <View style={styles.header}>
        <AppHeader title="과제" onBack={back} />
        {leader && assignment && (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="과제 더보기"
            accessibilityState={{ expanded: menu }}
            onPress={() => setMenu(!menu)}
            style={styles.more}
          >
            <NoticeIcon name="more" size={20} />
          </Pressable>
        )}
      </View>
      {menu && (
        <>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="과제 메뉴 닫기"
            onPress={() => setMenu(false)}
            style={[StyleSheet.absoluteFill, { zIndex: 1 }]}
          />
          <View style={styles.menu}>
            <Pressable
              accessibilityRole="button"
              onPress={() => {
                setMenu(false);
                router.push({ pathname: '/assignments/edit', params: { id } });
              }}
              style={styles.menuItem}
            >
              <AppText>과제 수정</AppText>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              onPress={() => {
                setMenu(false);
                setConfirm(true);
              }}
              style={styles.menuItem}
            >
              <AppText style={{ color: t.color.danger }}>과제 삭제</AppText>
            </Pressable>
          </View>
        </>
      )}
      {assignment ? (
        <View style={styles.content}>
          {
            <>
              <AppText variant="title" strong style={styles.title}>
                {assignment.title}
              </AppText>
              <AppText variant="caption" tone="tertiary">
                {Number(assignment.deadline.slice(5, 7))}월{' '}
                {Number(assignment.deadline.slice(8, 10))}일{' '}
                {assignment.deadline.slice(11)} 마감 · 제출물{' '}
                {assignment.visibility === 'public' ? '공개' : '비공개'}
              </AppText>
              <View style={styles.tabs} accessibilityRole="tablist">
                {(
                  [
                    { id: 'summary', label: '요약' },
                    { id: 'detail', label: '상세' },
                  ] as const
                ).map((item) => (
                  <Pressable
                    key={item.id}
                    accessibilityRole="tab"
                    accessibilityLabel={item.label}
                    accessibilityState={{ selected: item.id === tab }}
                    onPress={() => setTab(item.id)}
                    style={[styles.tab, tab === item.id && styles.selected]}
                  >
                    <AppText
                      style={{
                        color:
                          tab === item.id ? t.color.brand : t.color.placeholder,
                      }}
                    >
                      {item.label}
                    </AppText>
                  </Pressable>
                ))}
              </View>
            </>
          }
          {tab === 'summary' ? (
            <ScrollView>
              <AssignmentSummary
                assignment={assignment}
                leader={leader}
                onDetail={() => setTab('detail')}
              />
            </ScrollView>
          ) : (
            <ScrollView>
              <AssignmentDetail key={assignment.id} assignment={assignment} />
            </ScrollView>
          )}
        </View>
      ) : (
        <View style={styles.content}>
          <AppText>과제를 찾을 수 없어요</AppText>
          <Button
            label="과제 목록으로"
            onPress={() => router.replace('/study/assignments')}
          />
        </View>
      )}
      <ConfirmDialog
        visible={confirm}
        minHeight={n.dialogHeight}
        cancelTone="tertiary"
        title="과제를 삭제할까요?"
        description={
          '삭제한 과제는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'
        }
        confirmLabel="삭제"
        destructive
        onCancel={() => setConfirm(false)}
        onConfirm={() => {
          deleteAssignment(id);
          setConfirm(false);
          router.dismissTo('/study/assignments');
        }}
      />
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: t.color.background },
  header: { width: '100%', maxWidth: t.size.content, alignSelf: 'center' },
  more: {
    position: 'absolute',
    right: t.space.gutter,
    bottom: t.space.sm,
    width: t.space.section,
    height: t.size.touch,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    width: '100%',
    maxWidth: t.size.content,
    alignSelf: 'center',
    paddingHorizontal: t.space.gutter,
    flex: 1,
  },
  title: { marginTop: n.titleTop, marginBottom: t.space.xs },
  tabs: {
    flexDirection: 'row',
    marginTop: t.space.md,
    borderColor: t.color.border,
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: n.tabPadding,
    borderBottomWidth: t.size.line,
    borderBottomColor: t.color.border,
  },
  selected: { borderBottomColor: t.color.brand },
  menu: {
    position: 'absolute',
    right: t.space.gutter,
    top: n.menuTop,
    width: n.menuWidth,
    backgroundColor: t.color.background,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.md,
    zIndex: 2,
    overflow: 'hidden',
  },
  menuItem: { paddingVertical: n.menuItemPadding, alignItems: 'center' },
});
