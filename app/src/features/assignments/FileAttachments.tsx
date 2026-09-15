import * as DocumentPicker from 'expo-document-picker';
import { useState } from 'react';
import { Pressable, View } from 'react-native';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';
import type { NoticeImage } from '../notices/model';
import { NoticeIcon } from '../notices/NoticeIcon';
import { assignmentStyles as s } from './styles';
export function FileAttachments({
  files,
  onChange,
}: {
  readonly files: readonly NoticeImage[];
  readonly onChange: (files: readonly NoticeImage[]) => void;
}) {
  const [error, setError] = useState('');
  const choose = async () => {
    setError('');
    try {
      const result = await DocumentPicker.getDocumentAsync({
        multiple: true,
        copyToCacheDirectory: true,
      });
      if (result.canceled) return;
      onChange([
        ...files,
        ...result.assets.map((file, index) => ({
          id: `${Date.now()}-${index}`,
          name: file.name,
          source: { uri: file.uri },
        })),
      ]);
    } catch (caught) {
      if (!(caught instanceof Error)) throw caught;
      setError('파일을 불러오지 못했어요. 다시 선택해주세요.');
    }
  };
  return (
    <View style={s.group}>
      <AppText variant="large">파일 업로드</AppText>
      {files.map((file) => (
        <View key={file.id} style={s.file}>
          <NoticeIcon name="image" />
          <AppText style={{ flex: 1 }} numberOfLines={1}>
            {file.name}
          </AppText>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${file.name} 제거`}
            onPress={() =>
              onChange(files.filter((item) => item.id !== file.id))
            }
          >
            <NoticeIcon name="close" />
          </Pressable>
        </View>
      ))}
      <Pressable
        accessibilityRole="button"
        onPress={() => void choose()}
        style={s.add}
      >
        <NoticeIcon name="plus" />
        <AppText>파일 불러오기</AppText>
      </Pressable>
      {!!error && (
        <AppText accessibilityRole="alert" style={{ color: t.color.danger }}>
          {error}
        </AppText>
      )}
    </View>
  );
}
