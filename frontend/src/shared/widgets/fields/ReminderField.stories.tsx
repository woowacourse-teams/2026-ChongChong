import { useId } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useArgs } from 'storybook/preview-api';
import { fn } from 'storybook/test';
import { toLocalDateTime } from '../../utils/formatDate';
import ReminderField from './ReminderField';

const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
tomorrow.setHours(18, 0, 0, 0);
const dayAfterTomorrow = new Date(tomorrow);
dayAfterTomorrow.setDate(dayAfterTomorrow.getDate() + 1);
const firstReminder = toLocalDateTime(tomorrow);
const secondReminder = toLocalDateTime(dayAfterTomorrow);
const emptyReminders: string[] = [];

const meta = {
  title: 'Widgets/Fields/ReminderField',
  component: ReminderField,
  tags: ['autodocs'],
  parameters: {
    docs: {
      controls: { disable: true },
      description: {
        component:
          '리마인드 시각을 등록하고 삭제하는 필드입니다. 등록하기에서 날짜와 시간을 선택한 뒤 완료하면 목록이 갱신됩니다. Docs 예시는 상태만 표시하며, 날짜와 시간 선택은 Canvas에서 확인할 수 있습니다. 속성 변경과 Controls 동기화는 Canvas에서 확인해주세요. 같은 시각은 중복 등록하지 않습니다. 예시 시각은 페이지를 불러온 날의 다음 날과 다다음 날 오후 6시이며, 날짜 선택은 실제 현재 시각을 기준으로 합니다. 실제 알림이나 서버 요청은 발생하지 않습니다.',
      },
    },
  },
  args: {
    id: 'storybook-reminder',
    label: '리마인드 시각',
    value: emptyReminders,
    helpText: '설정한 시각마다 읽지 않은 스터디원에게 알림을 보내드릴게요.',
    errorText: '',
    disabled: false,
    onChange: fn(),
  },
  argTypes: {
    id: {
      description: 'Docs에서 중복되지 않도록 고유 접미사를 붙이는 식별자입니다.',
      control: false,
    },
    label: { description: '필드 라벨입니다.', control: 'text' },
    value: {
      description:
        '등록된 로컬 날짜·시간 문자열 배열입니다. 예: 2026-11-18T18:00. 정상 형식의 값을 사용합니다. null·undefined는 빈 목록으로 표시합니다.',
      control: 'object',
    },
    helpText: { description: '오류가 없을 때 표시하는 도움말입니다.', control: 'text' },
    errorText: { description: '도움말보다 우선 표시하는 오류 문구입니다.', control: 'text' },
    disabled: { description: '리마인드 추가와 각 시각의 삭제를 막습니다.', control: 'boolean' },
    onChange: {
      description: '등록·삭제 후 날짜 배열을 받고 Controls와 Actions를 갱신합니다.',
      control: false,
    },
  },
  decorators: [
    (Story, context) =>
      context.viewMode === 'docs' ? (
        <div
          css={{ contain: 'layout paint' }}
          onClickCapture={(event) => {
            event.preventDefault();
            event.stopPropagation();
          }}
        >
          <Story />
        </div>
      ) : (
        <Story />
      ),
  ],
  render: function Render(args) {
    const [, updateArgs] = useArgs();
    const uniqueId = useId();

    return (
      <ReminderField
        {...args}
        id={`${args.id}-${uniqueId}`}
        onChange={(value) => {
          updateArgs({ value });
          args.onChange(value);
        }}
      />
    );
  },
} satisfies Meta<typeof ReminderField>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Empty: Story = {};
export const WithReminder: Story = { args: { value: [firstReminder] } };
export const MultipleReminders: Story = {
  args: { value: [firstReminder, secondReminder] },
  parameters: {
    docs: {
      description: {
        story:
          '리마인드 시각을 여러 개 등록한 상태입니다. 추가·삭제와 중복 시각 처리는 Canvas에서 확인할 수 있습니다.',
      },
    },
  },
};
export const WithoutHelp: Story = { args: { helpText: '' } };
export const Error: Story = { args: { helpText: '', errorText: '리마인드 시각을 확인해주세요.' } };
export const ErrorOverHelp: Story = {
  args: {
    helpText: '오류가 있으면 숨겨지는 도움말입니다.',
    errorText: '오류 문구를 먼저 표시합니다.',
  },
};
export const Disabled: Story = { args: { disabled: true } };
export const DisabledWithReminders: Story = {
  args: { value: [firstReminder, secondReminder], disabled: true },
};
export const CustomLabel: Story = {
  args: {
    label: '과제 리마인드 시각',
    helpText: '미제출 스터디원에게 알림을 보낼 시각을 설정해주세요.',
  },
};
