import { useParams } from 'react-router';

export default function useIntegerParams<K extends string>(
  params: readonly K[],
): Record<K, number> {
  const currentParams = useParams();

  const convertedParams = params.reduce(
    (acc, param) => {
      const convertParam = Number(currentParams[param]);
      if (!convertParam || !Number.isInteger(convertParam)) {
        throw new Error(`현재 '${param}'은 유효하지 않은 값을 가진 경로 파라미터 입니다`);
      }
      acc[param] = Number(currentParams[param]);
      return acc;
    },
    {} as Record<K, number>,
  );

  return convertedParams;
}
