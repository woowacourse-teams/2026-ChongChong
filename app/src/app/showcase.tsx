import { useState } from 'react';
import { ConfirmDialog, Toast } from '../ui/feedback';
import {
  AppText,
  Badge,
  Button,
  Card,
  EmptyState,
  Field,
} from '../ui/primitives';
import { Screen } from '../ui/Screen';

export default function ShowcaseScreen() {
  const [name, setName] = useState('');
  const [dialog, setDialog] = useState<'confirm' | 'delete' | null>(null);
  const [message, setMessage] = useState('');
  const error =
    name.length > 10 ? '예시 입력은 10자까지 입력해 주세요.' : undefined;
  return (
    <Screen>
      <AppText variant="title">공통 UI</AppText>
      <AppText muted>입력, 버튼, 안내 상태를 직접 확인할 수 있어요.</AppText>
      <Card>
        <Badge>스터디 리드</Badge>
        <AppText variant="subtitle">카드 제목</AppText>
        <AppText>여러 화면에서 같은 스타일을 사용해요.</AppText>
      </Card>
      <Field
        label="이름 입력 예시"
        placeholder="이름을 입력해 주세요"
        value={name}
        onChangeText={setName}
        {...(error ? { error } : {})}
      />
      <Button
        label="저장하기"
        disabled={!name || Boolean(error)}
        onPress={() => setMessage('저장했어요.')}
      />
      <Button label="처리 중" loading onPress={() => {}} />
      <Button
        label="확인 모달 열기"
        variant="secondary"
        onPress={() => setDialog('confirm')}
      />
      <Button
        label="위험 동작 예시"
        variant="danger"
        onPress={() => setDialog('delete')}
      />
      {message !== '' && (
        <Toast message={message} onDismiss={() => setMessage('')} />
      )}
      <EmptyState title="아직 스터디가 없어요" />
      <ConfirmDialog
        visible={dialog !== null}
        title={
          dialog === 'delete' ? '스터디를 삭제할까요?' : '변경을 적용할까요?'
        }
        description={
          dialog === 'delete'
            ? '삭제한 스터디는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'
            : '공통 확인 모달의 동작 예시입니다.'
        }
        destructive={dialog === 'delete'}
        confirmLabel={dialog === 'delete' ? '삭제' : '확인'}
        onCancel={() => setDialog(null)}
        onConfirm={() => {
          setDialog(null);
          setMessage(
            dialog === 'delete'
              ? '삭제 동작을 확인했어요.'
              : '변경을 적용했어요.',
          );
        }}
      />
    </Screen>
  );
}
