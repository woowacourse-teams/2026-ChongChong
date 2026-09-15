import * as ImagePicker from 'expo-image-picker';
import { useState } from 'react';
import { Image, Pressable, StyleSheet, View } from 'react-native';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import type { NoticeImage } from './model';
import { NoticeIcon } from './NoticeIcon';
export function ImageAttachments({
  images,
  onChange,
}: {
  readonly images: readonly NoticeImage[];
  readonly onChange: (images: readonly NoticeImage[]) => void;
}) {
  const [error, setError] = useState('');
  const choose = async () => {
    setError('');
    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsMultipleSelection: true,
        quality: 1,
      });
      if (result.canceled) return;
      onChange([
        ...images,
        ...result.assets.map((asset, index) => ({
          id: `${Date.now()}-${index}`,
          name: asset.fileName ?? `image-${index + 1}.png`,
          source: { uri: asset.uri },
        })),
      ]);
    } catch (caught) {
      if (!(caught instanceof Error)) throw caught;
      setError('이미지를 불러오지 못했어요. 다시 선택해주세요.');
    }
  };
  return (
    <View style={styles.group}>
      <AppText variant="large">이미지 업로드</AppText>
      {images.map((image) => (
        <View key={image.id} style={styles.file}>
          <NoticeIcon name="image" size={20} />
          <AppText style={{ flex: 1 }} numberOfLines={1}>
            {image.name}
          </AppText>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${image.name} 제거`}
            onPress={() =>
              onChange(images.filter((item) => item.id !== image.id))
            }
          >
            <NoticeIcon name="close" />
          </Pressable>
        </View>
      ))}
      <Pressable
        accessibilityRole="button"
        onPress={() => void choose()}
        style={styles.add}
      >
        <NoticeIcon name="plus" />
        <AppText>파일 불러오기</AppText>
      </Pressable>
      <AppText variant="caption" tone="tertiary">
        본문을 설명할 이미지를 추가할 수 있어요
      </AppText>
      {!!error && (
        <AppText accessibilityRole="alert" style={{ color: t.color.danger }}>
          {error}
        </AppText>
      )}
    </View>
  );
}
const styles = StyleSheet.create({
  group: { gap: t.space.sm },
  file: {
    minHeight: t.size.control,
    padding: t.space.controlVertical,
    gap: t.space.sm,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.md,
    flexDirection: 'row',
    alignItems: 'center',
  },
  add: {
    height: t.size.control,
    backgroundColor: t.color.subtle,
    borderRadius: t.radius.md,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: t.space.xs,
  },
});
