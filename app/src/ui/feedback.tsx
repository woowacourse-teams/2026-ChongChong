import { Modal, Pressable, StyleSheet, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';
import { AppText } from './primitives';
import { tokens as t } from './tokens';

type ConfirmDialogProps = {
  readonly visible: boolean;
  readonly title: string;
  readonly description: string;
  readonly confirmLabel?: string;
  readonly destructive?: boolean;
  readonly onCancel: () => void;
  readonly onConfirm: () => void;
};
export function ConfirmDialog({
  visible,
  title,
  description,
  onCancel,
  onConfirm,
  confirmLabel = '확인',
  destructive = false,
}: ConfirmDialogProps) {
  return (
    <Modal visible={visible} transparent onRequestClose={onCancel}>
      <View style={styles.overlay} accessibilityViewIsModal>
        <View style={styles.dialog}>
          <View style={styles.dialogBody}>
            <AppText
              variant="subtitle"
              accessibilityRole="header"
              style={styles.centered}
            >
              {title}
            </AppText>
            <AppText muted style={styles.centered}>
              {description}
            </AppText>
          </View>
          <View style={styles.actions}>
            <Pressable
              accessibilityRole="button"
              onPress={onCancel}
              style={styles.action}
            >
              <AppText variant="large" style={styles.cancelLabel}>
                취소
              </AppText>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              onPress={onConfirm}
              style={[styles.action, styles.confirmAction]}
            >
              <AppText
                variant="large"
                style={{
                  color: destructive ? t.color.danger : t.color.brand,
                }}
              >
                {confirmLabel}
              </AppText>
            </Pressable>
          </View>
          <View style={styles.dialogBorder} />
        </View>
      </View>
    </Modal>
  );
}
export function Toast({
  message,
  onDismiss,
}: {
  readonly message: string;
  readonly onDismiss: () => void;
}) {
  return (
    <Pressable
      style={styles.toast}
      accessibilityRole="button"
      accessibilityLabel={message}
      accessibilityHint="누르면 안내를 닫습니다."
      accessibilityLiveRegion="polite"
      onPress={onDismiss}
    >
      <View style={styles.toastIcon}>
        <Svg width={13} height={10} viewBox="0 0 13 10" aria-hidden>
          <Path
            d="M11.5334 0.866699L4.20003 8.20003L0.866699 4.8667"
            fill="none"
            stroke={t.color.onBrand}
            strokeWidth={1.73333}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </Svg>
      </View>
      <AppText style={styles.toastLabel}>{message}</AppText>
    </Pressable>
  );
}
const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
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
  dialogBorder: {
    ...StyleSheet.absoluteFill,
    pointerEvents: 'none',
    borderRadius: t.radius.dialog,
    borderWidth: t.size.line,
    borderColor: t.color.border,
  },
  dialogBody: {
    paddingTop: t.space.xl,
    paddingHorizontal: t.space.gutter,
    paddingBottom: t.space.gutter,
    gap: t.space.sm,
  },
  confirmAction: {
    borderLeftWidth: t.size.line,
    borderLeftColor: t.color.border,
  },
  centered: { textAlign: 'center' },
  cancelLabel: { color: t.color.placeholder },
  actions: {
    flexDirection: 'row',
    borderTopWidth: t.size.line,
    borderColor: t.color.border,
  },
  action: {
    flex: 1,
    minHeight: t.size.control,
    alignItems: 'center',
    justifyContent: 'center',
  },
  toast: {
    backgroundColor: t.color.brand,
    paddingVertical: t.space.controlVertical,
    paddingHorizontal: t.space.lg,
    borderRadius: t.radius.md,
    flexDirection: 'row',
    alignItems: 'center',
    gap: t.space.sm,
  },
  toastLabel: { color: t.color.onBrand, flexShrink: 1 },
  toastIcon: {
    width: t.size.toastIcon,
    height: t.size.toastIcon,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
