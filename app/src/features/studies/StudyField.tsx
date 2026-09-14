import { useState } from 'react';
import { StyleSheet, TextInput, View } from 'react-native';
import { AppText } from '../../ui/primitives';
import { tokens as t } from '../../ui/tokens';

type Props = {
  readonly label: string;
  readonly required?: boolean;
  readonly value: string;
  readonly onChangeText: (value: string) => void;
  readonly placeholder: string;
  readonly multiline?: boolean;
  readonly helper: string;
  readonly error: boolean;
};
export function StudyField({
  label,
  required = false,
  value,
  onChangeText,
  placeholder,
  multiline = false,
  helper,
  error,
}: Props) {
  const [focused, setFocused] = useState(false);
  return (
    <View style={styles.field}>
      <AppText variant="large">
        {label}
        {required && (
          <AppText variant="large" style={styles.brand}>
            {' '}
            *
          </AppText>
        )}
      </AppText>
      <TextInput
        accessibilityLabel={label}
        accessibilityHint={helper}
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={t.color.placeholder}
        multiline={multiline}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        style={[
          styles.input,
          multiline && styles.multiline,
          focused && styles.focused,
        ]}
      />
      <AppText
        variant="caption"
        tone="tertiary"
        accessibilityRole={error ? 'alert' : undefined}
        style={error && styles.error}
      >
        {helper}
      </AppText>
    </View>
  );
}
const styles = StyleSheet.create({
  field: { gap: t.space.sm },
  brand: { color: t.color.brand },
  input: {
    height: 54,
    borderWidth: t.size.line,
    borderColor: t.color.border,
    borderRadius: t.radius.md,
    paddingHorizontal: t.space.lg,
    paddingVertical: t.space.controlVertical,
    fontFamily: t.font.regular,
    color: t.color.text,
    ...t.typography.large,
  },
  multiline: { height: 96, textAlignVertical: 'top' },
  focused: {
    borderColor: t.color.brand,
    outlineWidth: 0,
    outlineStyle: 'solid',
  },
  error: { color: t.color.danger },
});
