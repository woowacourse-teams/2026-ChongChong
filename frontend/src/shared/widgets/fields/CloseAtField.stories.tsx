import { useId } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useArgs } from 'storybook/preview-api';
import { fn } from 'storybook/test';
import { toLocalDateTime } from '../../utils/formatDate';
import CloseAtField from './CloseAtField';

const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
tomorrow.setHours(18, 0, 0, 0);
const sampleDeadline = toLocalDateTime(tomorrow);

const meta = {
  title: 'Widgets/Fields/CloseAtField',
  component: CloseAtField,
  tags: ['autodocs'],
  parameters: {
    docs: {
      controls: { disable: true },
      description: {
        component:
          '마감 시각을 선택하는 필드입니다. 날짜 선택 → 다음 → 시각 선택 → 완료하면 value가 갱신됩니다. Docs 예시는 상태만 표시하며, 날짜와 시간 선택은 Canvas에서 확인할 수 있습니다. 속성 변경과 Controls 동기화는 Canvas에서 확인해주세요. 샘플은 페이지를 불러온 날의 다음 날 오후 6시입니다. 선택 가능한 시각은 실제 현재 시각을 기준으로 합니다. isRequired는 필수 표시만 제공하며 오류 문구는 버튼 테두리를 바꾸지 않습니다.',
      },
    },
  },
  args: {
    id: 'storybook-close-at',
    value: '',
    isRequired: false,
    errorText: '',
    disabled: false,
    onChange: fn(),
  },
  argTypes: {
    id: {
      description: 'Docs에서 중복되지 않도록 고유 접미사를 붙이는 식별자입니다.',
      control: false,
    },
    value: {
      description:
        '로컬 날짜·시간 문자열입니다. 예: 2026-11-18T18:00. 빈 문자열은 미설정 상태입니다.',
      control: 'text',
    },
    isRequired: { description: '마감 시각 라벨에 필수 표시를 추가합니다.', control: 'boolean' },
    errorText: { description: '선택 영역 아래의 오류 문구입니다.', control: 'text' },
    disabled: { description: '날짜 선택창을 열 수 없게 합니다.', control: 'boolean' },
    onChange: { description: '선택한 문자열을 Controls와 Actions에 반영합니다.', control: false },
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
      <CloseAtField
        {...args}
        id={`${args.id}-${uniqueId}`}
        onChange={(value) => {
          updateArgs({ value });
          args.onChange(value);
        }}
      />
    );
  },
} satisfies Meta<typeof CloseAtField>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Empty: Story = {};
export const WithValue: Story = { args: { value: sampleDeadline } };
export const Required: Story = { args: { isRequired: true } };
export const Error: Story = { args: { errorText: '마감 시각을 설정해주세요.' } };
export const Disabled: Story = { args: { disabled: true } };
export const DisabledWithValue: Story = { args: { value: sampleDeadline, disabled: true } };
