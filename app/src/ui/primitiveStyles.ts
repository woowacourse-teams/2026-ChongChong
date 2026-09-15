import { StyleSheet } from 'react-native';
import { tokens as t } from './tokens';
export const primitiveStyles = StyleSheet.create({
  button: {
    minHeight: t.size.control,
    paddingVertical: t.space.controlVertical,
    paddingHorizontal: t.space.lg,
    borderRadius: t.radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: t.space.sm,
  },
  primary: { backgroundColor: t.color.brand },
  secondary: {
    backgroundColor: t.color.background,
  },
  secondaryBorder: {
    ...StyleSheet.absoluteFill,
    pointerEvents: 'none',
    borderRadius: t.radius.md,
    borderWidth: t.size.line,
    borderColor: t.color.brand,
  },
  danger: { backgroundColor: t.color.danger },
  disabled: { backgroundColor: t.color.disabled },
  group: { gap: t.space.sm },
  input: {
    minHeight: t.size.control,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.md,
    paddingVertical: t.space.controlVertical,
    paddingHorizontal: t.space.lg,
    fontFamily: t.font.regular,
    color: t.color.text,
    ...t.typography.large,
  },
  focused: {
    borderColor: t.color.brand,
    outlineWidth: 0,
    outlineStyle: 'solid',
  },
  card: {
    minHeight: t.size.cardMinHeight,
    borderRadius: t.radius.lg,
    padding: t.space.gutter,
    gap: t.space.md,
    backgroundColor: t.color.background,
  },
  cardBorder: {
    ...StyleSheet.absoluteFill,
    pointerEvents: 'none',
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.lg,
  },
  badge: {
    alignSelf: 'flex-start',
    backgroundColor: t.color.brand,
    paddingHorizontal: t.space.badgeHorizontal,
    paddingVertical: t.space.badgeVertical,
    borderRadius: t.radius.pill,
  },
  empty: {
    alignItems: 'center',
    gap: t.space.sm,
    paddingVertical: t.space.xxl,
  },
  emptyImage: {
    width: t.size.emptyIllustration,
    height: t.size.emptyIllustration,
  },
});
