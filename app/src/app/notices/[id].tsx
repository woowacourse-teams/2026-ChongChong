import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useEntryScenario } from '../../features/entry/EntryProvider';
import { noticeLayout as n } from '../../features/notices/layout';
import { NoticeIcon } from '../../features/notices/NoticeIcon';
import { useNotices } from '../../features/notices/NoticeProvider';
import { NoticeReader } from '../../features/notices/NoticeReader';
import { NoticeSummary } from '../../features/notices/NoticeSummary';
import { AppHeader } from '../../ui/AppHeader';
import { ConfirmDialog } from '../../ui/feedback';
import { AppText, Button } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
export default function NoticeDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { notices, deleteNotice } = useNotices();
  const { selectedStudy } = useEntryScenario();
  const notice = notices.find((item) => item.id === id);
  const leader = selectedStudy?.role === 'leader';
  const [tab, setTab] = useState<'summary' | 'detail'>('summary');
  const [menu, setMenu] = useState(false);
  const [confirm, setConfirm] = useState(false);
  const back = () =>
    router.canGoBack() ? router.back() : router.replace('/study/notices');
  return (
    <SafeAreaView edges={['bottom', 'left', 'right']} style={styles.page}>
      <View style={styles.header}>
        <AppHeader title="공지" onBack={back} />
        {leader && notice && (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="공지 더보기"
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
            accessibilityLabel="공지 메뉴 닫기"
            onPress={() => setMenu(false)}
            style={[StyleSheet.absoluteFill, { zIndex: 1 }]}
          />
          <View style={styles.menu}>
            <Pressable
              accessibilityRole="button"
              onPress={() => {
                setMenu(false);
                router.push({ pathname: '/notices/edit', params: { id } });
              }}
              style={styles.menuItem}
            >
              <AppText>공지 수정</AppText>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              onPress={() => {
                setMenu(false);
                setConfirm(true);
              }}
              style={styles.menuItem}
            >
              <AppText style={{ color: t.color.danger }}>공지 삭제</AppText>
            </Pressable>
          </View>
        </>
      )}
      {notice ? (
        <View style={styles.content}>
          {leader && (
            <>
              <AppText variant="title" strong style={styles.title}>
                {notice.title}
              </AppText>
              <AppText variant="caption" tone="tertiary">
                {notice.createdLabel}
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
          )}
          {leader && tab === 'summary' ? (
            <ScrollView>
              <NoticeSummary notice={notice} />
            </ScrollView>
          ) : (
            <NoticeReader key={notice.id} notice={notice} leader={leader} />
          )}
        </View>
      ) : (
        <View style={styles.content}>
          <AppText>공지를 찾을 수 없어요</AppText>
          <Button
            label="공지 목록으로"
            onPress={() => router.replace('/study/notices')}
          />
        </View>
      )}
      <ConfirmDialog
        visible={confirm}
        minHeight={n.dialogHeight}
        cancelTone="tertiary"
        title="공지를 삭제할까요?"
        description={
          '삭제한 공지는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'
        }
        confirmLabel="삭제"
        destructive
        onCancel={() => setConfirm(false)}
        onConfirm={() => {
          deleteNotice(id);
          setConfirm(false);
          router.dismissTo('/study/notices');
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
