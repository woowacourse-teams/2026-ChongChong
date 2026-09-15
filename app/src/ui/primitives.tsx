import { type PropsWithChildren, useState } from 'react';
import type { TextInputProps, TextProps } from 'react-native';
import {
  ActivityIndicator,
  Image,
  Pressable,
  Text,
  TextInput,
  View,
} from 'react-native';
import { primitiveStyles as styles } from './primitiveStyles';
import { tokens as t } from './tokens';

type AppTextProps = TextProps & {
  readonly variant?: keyof typeof t.typography;
  readonly strong?: boolean;
  readonly muted?: boolean;
  readonly tone?: 'primary' | 'secondary' | 'tertiary';
};
export function AppText({
  variant = 'body',
  strong = false,
  muted = false,
  tone,
  style,
  ...props
}: AppTextProps) {
  return (
    <Text
      {...props}
      style={[
        t.typography[variant],
        {
          fontFamily: strong ? t.font.strong : t.font.regular,
          color:
            tone === 'tertiary'
              ? t.color.tertiary
              : tone === 'secondary' || muted
                ? t.color.secondary
                : t.color.text,
        },
        style,
      ]}
    />
  );
}

type ButtonProps = {
  readonly label: string;
  readonly onPress: () => void;
  readonly variant?: 'primary' | 'secondary' | 'danger';
  readonly disabled?: boolean;
  readonly loading?: boolean;
};
export function Button({
  label,
  onPress,
  variant = 'primary',
  disabled = false,
  loading = false,
}: ButtonProps) {
  const unavailable = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: unavailable, busy: loading }}
      disabled={unavailable}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        styles[variant],
        unavailable && styles.disabled,
        pressed && { opacity: t.pressedOpacity },
      ]}
    >
      {loading && <ActivityIndicator color={t.color.onBrand} />}
      <AppText
        variant="large"
        style={{
          color:
            unavailable || variant !== 'secondary'
              ? t.color.onBrand
              : t.color.brand,
        }}
      >
        {label}
      </AppText>
      {variant === 'secondary' && !unavailable && (
        <View style={styles.secondaryBorder} />
      )}
    </Pressable>
  );
}

type FieldProps = TextInputProps & {
  readonly label: string;
  readonly error?: string;
  readonly required?: boolean;
};
export function Field({
  label,
  error,
  required = false,
  style,
  onFocus,
  onBlur,
  ...props
}: FieldProps) {
  const [focused, setFocused] = useState(false);
  return (
    <View style={styles.group}>
      <AppText variant="large">
        {label}
        {required && (
          <AppText variant="large" style={{ color: t.color.brand }}>
            {' '}
            *
          </AppText>
        )}
      </AppText>
      <TextInput
        {...props}
        accessibilityLabel={required ? `${label} *` : label}
        accessibilityHint={error}
        placeholderTextColor={t.color.placeholder}
        onFocus={(event) => {
          setFocused(true);
          onFocus?.(event);
        }}
        onBlur={(event) => {
          setFocused(false);
          onBlur?.(event);
        }}
        style={[styles.input, focused && styles.focused, style]}
      />
      {error && (
        <AppText
          variant="caption"
          accessibilityRole="alert"
          style={{ color: t.color.danger }}
        >
          {error}
        </AppText>
      )}
    </View>
  );
}
export function Card({ children }: PropsWithChildren) {
  return (
    <View style={styles.card}>
      {children}
      <View style={styles.cardBorder} />
    </View>
  );
}
export function Badge({ children }: PropsWithChildren) {
  return (
    <View style={styles.badge}>
      <AppText variant="caption" style={{ color: t.color.onBrand }}>
        {children}
      </AppText>
    </View>
  );
}
export function EmptyState({
  title,
  description,
  compact = false,
}: {
  readonly title: string;
  readonly description?: string;
  readonly compact?: boolean;
}) {
  return (
    <View
      style={[styles.empty, compact && { paddingTop: 0, paddingBottom: 16 }]}
    >
      <Image
        source={require('../../assets/images/empty-study.png')}
        style={styles.emptyImage}
        resizeMode="contain"
        accessible={false}
      />
      <AppText variant="large" tone="tertiary">
        {title}
      </AppText>
      {description && <AppText tone="tertiary">{description}</AppText>}
    </View>
  );
}
