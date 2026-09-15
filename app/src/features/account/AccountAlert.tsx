import { Modal, Pressable, StyleSheet, View } from 'react-native';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';

export function AccountAlert({
  title,
  description,
  onClose,
}: {
  readonly title: string;
  readonly description: string;
  readonly onClose: () => void;
}) {
  return (
    <Modal transparent visible onRequestClose={onClose}>
      <View style={styles.overlay} accessibilityViewIsModal>
        <View style={styles.dialog}>
          <View style={styles.body}>
            <AppText
              variant="subtitle"
              accessibilityRole="header"
              style={styles.center}
            >
              {title}
            </AppText>
            <AppText tone="secondary" style={styles.center}>
              {description}
            </AppText>
          </View>
          <Pressable
            accessibilityRole="button"
            onPress={onClose}
            style={styles.action}
          >
            <AppText variant="large" style={styles.confirm}>
              확인
            </AppText>
          </Pressable>
        </View>
      </View>
    </Modal>
  );
}
const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: t.space.gutter,
    backgroundColor: t.color.overlay,
  },
  dialog: {
    width: '100%',
    maxWidth: t.size.dialog,
    borderRadius: t.radius.dialog,
    overflow: 'hidden',
    backgroundColor: t.color.background,
  },
  body: {
    paddingTop: t.space.xl,
    paddingHorizontal: t.space.gutter,
    paddingBottom: t.space.gutter,
    gap: t.space.sm,
  },
  center: { textAlign: 'center' },
  action: {
    minHeight: t.size.control + t.size.line,
    borderTopWidth: t.size.line,
    borderColor: t.color.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  confirm: { color: t.color.brand },
});
