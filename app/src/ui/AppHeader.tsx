import { Pressable, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Svg, { Path } from 'react-native-svg';
import { AppText } from './primitives';
import { tokens as t } from './tokens';

type AppHeaderProps = {
  readonly title: string;
  readonly onBack?: () => void;
};

export function AppHeader({ title, onBack }: AppHeaderProps) {
  return (
    <SafeAreaView edges={['top', 'left', 'right']} style={styles.safe}>
      <View style={styles.content}>
        {onBack && (
          <View style={styles.backSlot}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="뒤로 가기"
              onPress={onBack}
              style={({ pressed }) => [
                styles.back,
                pressed && { opacity: t.pressedOpacity },
              ]}
            >
              <Svg
                width={t.size.icon}
                height={t.size.icon}
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden={true}
              >
                <Path
                  d="M15 18L9 12L15 6"
                  stroke={t.color.text}
                  strokeWidth={2.2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </Svg>
            </Pressable>
          </View>
        )}
        <AppText
          variant="large"
          accessibilityRole="header"
          style={styles.title}
        >
          {title}
        </AppText>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { backgroundColor: t.color.background },
  content: {
    width: '100%',
    maxWidth: t.size.content,
    alignSelf: 'center',
    minHeight: t.size.header,
    paddingLeft: t.space.gutter,
    paddingRight: t.space.md,
    paddingVertical: t.size.headerPadding,
    flexDirection: 'row',
    alignItems: 'center',
    gap: t.space.sm,
  },
  backSlot: { width: t.size.icon, height: t.size.icon },
  back: {
    position: 'absolute',
    top: -(t.size.touch - t.size.icon) / 2,
    left: -(t.size.touch - t.size.icon) / 2,
    width: t.size.touch,
    height: t.size.touch,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: { flex: 1 },
});
