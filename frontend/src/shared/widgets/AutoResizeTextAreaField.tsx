import useAutoResizeTextArea from '../hooks/useAutoResizeTextArea';
import TextAreaField, { TextAreaFieldProps } from './TextAreaField';

type AutoResizeTextAreaFieldProps = Omit<TextAreaFieldProps, 'ref'>;

export default function AutoResizeTextAreaField(props: AutoResizeTextAreaFieldProps) {
  const { targetRef } = useAutoResizeTextArea({
    inputValue: props.value,
  });

  return <TextAreaField {...props} ref={targetRef}></TextAreaField>;
}
