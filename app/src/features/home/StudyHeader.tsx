import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import { EntryIcon } from '../entry/EntryIcon';
import { useEntryScenario } from '../entry/EntryProvider';
import { homeLayout as layout } from './layout';

export function StudyHeader() {
  const router = useRouter();
  const { name, selectedStudy } = useEntryScenario();
  const [menu, setMenu] = useState(false);
  return (
    <SafeAreaView edges={['top', 'left', 'right']} style={styles.safe}>
      <View style={styles.header}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="내 스터디로 돌아가기"
          onPress={() => router.replace('/studies')}
          style={styles.back}
        >
          <EntryIcon name="back" />
        </Pressable>
        <View style={styles.copy}>
          <AppText variant="subtitle" numberOfLines={1}>
            {selectedStudy?.name ?? '스터디'}
          </AppText>
          <AppText variant="caption" tone="tertiary">
            {name} · {selectedStudy?.role === 'leader' ? '리드' : '스터디원'}
          </AppText>
        </View>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="스터디 메뉴"
          accessibilityState={{ expanded: menu }}
          onPress={() => setMenu(!menu)}
          style={styles.touch}
        >
          <EntryIcon name="menu" size={layout.header.menuSize} />
        </Pressable>
      </View>
      {menu && (
        <View style={styles.menu}>
          <Pressable
            accessibilityRole="button"
            onPress={() => {
              setMenu(false);
              router.push('/notifications');
            }}
            style={styles.menuItem}
          >
            <AppText>알림</AppText>
          </Pressable>
          <Pressable
            accessibilityRole="button"
            onPress={() => {
              setMenu(false);
              router.replace('/studies');
            }}
            style={styles.menuItem}
          >
            <AppText>내 스터디</AppText>
          </Pressable>
        </View>
      )}
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  safe: { backgroundColor: t.color.background },
  header: {
    height: t.size.header,
    width: '100%',
    maxWidth: t.size.content,
    alignSelf: 'center',
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: t.space.gutter,
    paddingRight: layout.header.rightPadding,
    gap: layout.header.contentGap,
  },
  back: { width: t.size.icon, height: t.size.touch, justifyContent: 'center' },
  touch: {
    width: t.size.touch,
    height: t.size.touch,
    alignItems: 'center',
    justifyContent: 'center',
  },
  copy: {
    flex: 1,
    gap: layout.header.copyGap,
    transform: [{ translateY: layout.header.copyOffset }],
  },
  menu: {
    width: '100%',
    maxWidth: t.size.content,
    alignSelf: 'center',
    paddingHorizontal: t.space.gutter,
    borderBottomWidth: t.size.line,
    borderColor: t.color.border,
  },
  menuItem: { minHeight: t.size.touch, justifyContent: 'center' },
});
