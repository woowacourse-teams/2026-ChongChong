import { useRef, useState } from 'react';
import {
  Image,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  ScrollView,
  StyleSheet,
  View,
} from 'react-native';
import { Toast } from '../../ui/feedback';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import { noticeLayout as n } from './layout';
import type { Notice } from './model';
import { NoticeIcon } from './NoticeIcon';
import { useNotices } from './NoticeProvider';
export function NoticeReader({
  notice,
  leader,
}: {
  readonly notice: Notice;
  readonly leader: boolean;
}) {
  const { markRead } = useNotices();
  const self = notice.recipients.find((item) => item.id === 'self');
  const [progress, setProgress] = useState(0);
  const [contentWidth, setContentWidth] = useState<number>(n.imageWidth);
  const [toast, setToast] = useState(false);
  const measured = useRef({ content: 0, viewport: 0 });
  const checkEnd = (offset: number) => {
    const { content, viewport } = measured.current;
    if (!content || !viewport || leader || !self || self.readAt) return;
    const percent = Math.min(
      100,
      Math.round(((offset + viewport) / content) * 100),
    );
    setProgress(percent);
    if (offset + viewport >= content - 2) {
      markRead(notice.id);
      setToast(true);
    }
  };
  const onScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) =>
    checkEnd(event.nativeEvent.contentOffset.y);
  return (
    <View style={styles.page}>
      <ScrollView
        onScroll={onScroll}
        scrollEventThrottle={16}
        onLayout={(event) => {
          measured.current.viewport = event.nativeEvent.layout.height;
          setContentWidth(event.nativeEvent.layout.width);
          checkEnd(0);
        }}
        onContentSizeChange={(_, height) => {
          measured.current.content = height;
          checkEnd(0);
        }}
        contentContainerStyle={[
          styles.content,
          !leader && { paddingTop: n.titleTop },
        ]}
      >
        {!leader && (
          <AppText variant="title" strong style={styles.title}>
            {notice.title}
          </AppText>
        )}
        <AppText variant="large" style={styles.body} tone="secondary">
          {notice.body}
        </AppText>
        {notice.images.length > 0 && (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.images}
          >
            {notice.images.map((image) => (
              <Image
                key={image.id}
                source={image.source}
                accessibilityLabel={image.name}
                style={
                  notice.images.length === 1
                    ? [
                        styles.singleImage,
                        {
                          width: contentWidth,
                          height: contentWidth * (n.imageHeight / n.imageWidth),
                        },
                      ]
                    : styles.image
                }
                resizeMode="cover"
              />
            ))}
          </ScrollView>
        )}
      </ScrollView>
      {toast && (
        <View style={styles.toast}>
          <Toast
            message="읽음으로 표시됐어요"
            onDismiss={() => setToast(false)}
          />
        </View>
      )}
      {!leader && self && (
        <View style={styles.footer}>
          {self.readAt ? (
            <View style={styles.completed}>
              <NoticeIcon name="check" size={22} />
              <AppText variant="large" style={styles.green}>
                {self.readAt}에 읽음
              </AppText>
            </View>
          ) : (
            <>
              <AppText variant="large" style={styles.green}>
                끝까지 읽으면 읽음으로 표시돼요
              </AppText>
              <AppText variant="caption" tone="tertiary">
                지금 {progress}% 읽었어요
              </AppText>
            </>
          )}
        </View>
      )}
    </View>
  );
}
const styles = StyleSheet.create({
  page: { flex: 1 },
  content: {
    paddingTop: n.summaryTop,
    paddingBottom: t.space.section,
    gap: t.space.xl,
  },
  title: { marginBottom: t.space.sm },
  body: { lineHeight: t.typography.large.lineHeight },
  images: { gap: t.space.sm },
  image: {
    width: n.galleryImageSize,
    height: n.galleryImageSize,
    borderRadius: t.radius.md,
  },
  singleImage: {
    width: n.imageWidth,
    height: n.imageHeight,
    borderRadius: t.radius.md,
  },
  toast: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: n.readingFooterHeight + t.space.gutter,
    zIndex: 1,
  },
  footer: {
    minHeight: n.readingFooterHeight,
    marginHorizontal: -t.space.gutter,
    paddingHorizontal: t.space.gutter,
    borderTopWidth: t.size.line,
    borderColor: t.color.border,
    alignItems: 'center',
    justifyContent: 'center',
    gap: t.space.xs,
  },
  completed: { flexDirection: 'row', alignItems: 'center', gap: t.space.xs },
  green: { color: t.color.brand },
});
