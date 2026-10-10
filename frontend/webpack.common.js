import { existsSync } from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';
import HtmlWebpackPlugin from 'html-webpack-plugin';
import webpack from 'webpack';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export function createCommonConfig({ envFile, environment, assetFilename }) {
  if (envFile) {
    const envPath = path.resolve(__dirname, envFile);

    // CodeBuild에 등록된 환경 변수는 유지하고, 없는 값만 로컬 env 파일에서 불러옵니다.
    if (existsSync(envPath)) {
      process.loadEnvFile(envPath);
    }
  }

  return {
    entry: './main.tsx',

    module: {
      rules: [
        {
          test: /\.(ts|tsx)$/,
          use: 'babel-loader',
          exclude: /node_modules/,
        },
        {
          test: /\.(png|svg|jpg|jpeg|gif|webp)$/i,
          type: 'asset',
        },
        {
          test: /\.(woff|woff2|eot|ttf|otf)$/i,
          type: 'asset/resource',
          generator: {
            filename: assetFilename,
          },
        },
      ],
    },

    output: {
      clean: true,
      path: path.resolve(__dirname, 'dist'),
      publicPath: '/',
    },

    resolve: {
      extensions: ['.ts', '.js', '.tsx'],
    },

    plugins: [
      new HtmlWebpackPlugin({
        template: './index.html',
        filename: 'index.html',
        inject: true,
      }),

      new webpack.DefinePlugin({
        'process.env.API_BASE_URL': JSON.stringify(
          process.env.API_BASE_URL ?? 'https://mock.chongchong.com',
        ),
        'process.env.KAKAO_REST_API_KEY': JSON.stringify(process.env.KAKAO_REST_API_KEY ?? ''),
        'process.env.POSTHOG_HOST': JSON.stringify(process.env.POSTHOG_HOST ?? ''),
        'process.env.POSTHOG_PROJECT_TOKEN': JSON.stringify(
          process.env.POSTHOG_PROJECT_TOKEN ?? '',
        ),
        'process.env.USE_MSW': JSON.stringify(process.env.USE_MSW ?? 'false'),
        'process.env.SENTRY_DSN': JSON.stringify(process.env.SENTRY_DSN ?? ''),
        'process.env.SENTRY_ENVIRONMENT': JSON.stringify(process.env.DEPLOY_ENV ?? environment),
      }),
    ],
  };
}
