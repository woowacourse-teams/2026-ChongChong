import { StyleSheet } from 'react-native';
import { tokens as t } from '../../ui/tokens';
import { noticeLayout as n } from './layout';
export const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: t.color.overlay,
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  sheet: {
    width: '100%',
    maxWidth: t.size.content,
    backgroundColor: t.color.background,
    borderTopLeftRadius: t.space.xl,
    borderTopRightRadius: t.space.xl,
    paddingHorizontal: t.space.gutter,
    paddingBottom: t.space.section,
    gap: t.space.md,
  },
  handle: {
    alignSelf: 'center',
    width: n.sheetHandleWidth,
    height: t.space.xs,
    borderRadius: t.radius.pill,
    backgroundColor: t.color.tertiary,
    marginTop: n.sheetHandleTop,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    minHeight: n.timeRowHeight,
  },
  green: { color: t.color.brand },
  selection: {
    backgroundColor: t.color.brandSubtle,
    borderRadius: t.radius.md,
    alignItems: 'center',
    height: n.selectionHeight,
    justifyContent: 'center',
  },
  grid: { flexDirection: 'row', flexWrap: 'wrap' },
  day: {
    width: '14.285714%',
    height: n.calendarDayHeight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dayCircle: {
    width: n.calendarDayHeight,
    height: n.calendarDayHeight,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: t.radius.pill,
  },
  arrow: { width: n.timeRowHeight, alignItems: 'center' },
  times: {
    height: n.timeViewportHeight,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.sm,
  },
  timeContent: { padding: t.space.sm },
  time: {
    height: n.timeRowHeight,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
