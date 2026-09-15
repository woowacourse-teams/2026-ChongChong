import { Image, Pressable, StyleSheet, View } from 'react-native';
import { AppText, Badge, Card } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import { entryAssets } from '../entry/assets';
import { EntryIcon } from '../entry/EntryIcon';

type Study = {
  readonly name: string;
  readonly description: string;
  readonly role: 'leader' | 'member';
  readonly notices: number;
  readonly assignments: number;
  readonly members: number;
};
export function StudyCard({
  study,
  onPress,
}: {
  readonly study: Study;
  readonly onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${study.name} 열기`}
      onPress={onPress}
      style={({ pressed }) => pressed && { opacity: t.pressedOpacity }}
    >
      <Card>
        <View style={styles.contents}>
          <View style={styles.row}>
            <Badge>
              {study.role === 'leader' ? '스터디 리드' : '스터디원'}
            </Badge>
            <View style={styles.count}>
              <EntryIcon name="notice" size={12} color={t.color.tertiary} />
              <AppText variant="caption" tone="tertiary">
                공지 {study.notices}
              </AppText>
            </View>
            <View style={styles.count}>
              <EntryIcon name="assignment" size={12} color={t.color.tertiary} />
              <AppText variant="caption" tone="tertiary">
                과제 {study.assignments}
              </AppText>
            </View>
          </View>
          <View style={styles.detail}>
            <Image
              source={entryAssets.studyCard}
              style={styles.thumbnail}
              resizeMode="contain"
              accessible={false}
            />
            <View style={styles.copy}>
              <View style={styles.titleRow}>
                <AppText
                  variant="subtitle"
                  numberOfLines={1}
                  style={styles.title}
                >
                  {study.name}
                </AppText>
                <View style={styles.people}>
                  <EntryIcon name="members" size={12} color={t.color.brand} />
                  <AppText variant="caption" style={styles.brand}>
                    {study.members}명
                  </AppText>
                </View>
              </View>
              <AppText variant="caption" tone="secondary" numberOfLines={2}>
                {study.description}
              </AppText>
            </View>
            <EntryIcon name="chevron" size={16} color={t.color.text} />
          </View>
        </View>
      </Card>
    </Pressable>
  );
}
const styles = StyleSheet.create({
  contents: { gap: t.space.badgeHorizontal },
  row: { flexDirection: 'row', alignItems: 'center', gap: t.space.md },
  count: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: t.space.xs,
    backgroundColor: t.color.mutedBackground,
    borderRadius: t.radius.pill,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  detail: { flexDirection: 'row', gap: t.space.lg, alignItems: 'center' },
  thumbnail: { width: 64, height: 64, borderRadius: 20 },
  copy: { flex: 1, gap: t.space.xs },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: t.space.sm },
  title: { flexShrink: 1 },
  people: { flexDirection: 'row', alignItems: 'center', gap: 2 },
  brand: { color: t.color.brand },
});
