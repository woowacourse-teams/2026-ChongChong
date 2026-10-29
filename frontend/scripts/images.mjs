import ImageMinimizerPlugin from 'image-minimizer-webpack-plugin';
import { Buffer } from 'node:buffer';

export async function minimizePng(original, options) {
  const result = await ImageMinimizerPlugin.sharpMinify(original, options);
  if (result && result.data.length > original.data.length) {
    return { ...result, data: original.data };
  }
  return result;
}

// 플러그인이 이 함수를 별도 worker에서 실행하므로 의존성을 함수 안에서 불러옵니다.
export async function minimizeSvg(original) {
  const { optimize } = await import('svgo');
  const { default: sharp } = await import('sharp');
  let svg = original.data.toString();
  const images = [...svg.matchAll(/data:image\/png;base64,([A-Za-z0-9+/=\r\n]+)/g)];

  for (const [uri, base64] of images) {
    const input = Buffer.from(base64, 'base64');
    const output = await sharp(input)
      .png({ compressionLevel: 9, adaptiveFiltering: true })
      .toBuffer();
    if (output.length < input.length) {
      svg = svg.replaceAll(uri, `data:image/png;base64,${output.toString('base64')}`);
    }
  }

  const result = optimize(svg, {
    path: original.filename,
    // SVG 안의 작은 scale 값과 패턴의 배치를 보존합니다.
    floatPrecision: 8,
    plugins: [
      {
        name: 'preset-default',
        params: { overrides: { cleanupIds: false, convertTransform: false } },
      },
    ],
  });
  const data = Buffer.from(result.data);

  return {
    ...original,
    data: data.length < original.data.length ? data : original.data,
    info: {
      ...original.info,
      minimized: true,
      minimizedBy: ['svgo', ...(original.info?.minimizedBy ?? [])],
    },
  };
}

export async function generateWebp(original, options) {
  const { default: sharp } = await import('sharp');
  const { default: ImageMinimizerPlugin } = await import('image-minimizer-webpack-plugin');
  const { width, height } = options.resize ?? {};

  // SVG를 작은 기본 크기로 먼저 래스터화한 뒤 확대하면 흐려지므로,
  // 요청한 해상도로 먼저 렌더링합니다. 패턴과 잘림도 함께 유지됩니다.
  if (/\.svg$/i.test(original.filename) && (width || height)) {
    const metadata = await sharp(original.data).metadata();
    const scale = Math.max(
      1,
      width ? width / metadata.width : 1,
      height ? height / metadata.height : 1,
    );
    const data = await sharp(original.data, { density: 72 * scale })
      .png()
      .toBuffer();
    return ImageMinimizerPlugin.sharpGenerate(
      { ...original, filename: original.filename.replace(/\.svg$/i, '.png'), data },
      options,
    );
  }

  return ImageMinimizerPlugin.sharpGenerate(original, options);
}
