// import { existsSync } from 'node:fs';
// import path from 'path';
// import process from 'node:process';
// import { fileURLToPath } from 'url';
// import HtmlWebpackPlugin from 'html-webpack-plugin';
// import webpack from 'webpack';

// const __dirname = path.dirname(fileURLToPath(import.meta.url));
// const envFile = process.env.NODE_ENV === 'production' ? '.env.production' : '.env.local';

// const envPath = path.resolve(__dirname, envFile);

// if (existsSync(envPath)) {
//   process.loadEnvFile(envPath);
// }

// export default {
//   mode: 'development',
//   entry: './main.tsx',
//   module: {
//     rules: [
//       {
//         test: /\.(ts|tsx)$/,
//         use: 'babel-loader',
//         exclude: /node_modules/,
//       },
//       {
//         test: /\.(png|svg|jpg|jpeg|gif|webp)$/i,
//         type: 'asset',
//       },
//       {
//         test: /\.(woff|woff2|eot|ttf|otf)$/i,
//         type: 'asset/resource',
//         generator: {
//           filename: 'assets/[name][ext]',
//         },
//       },
//     ],
//   },
//   output: {
//     filename: 'bundle.js',
//     path: path.resolve(__dirname, 'dist'),
//     publicPath: '/',
//   },
//   resolve: {
//     extensions: ['.ts', '.js', '.tsx'],
//   },
//   plugins: [
//     new HtmlWebpackPlugin({
//       template: './index.html',
//       filename: 'index.html',
//       inject: true,
//     }),
//     new webpack.DefinePlugin({
//       'process.env.API_BASE_URL': JSON.stringify(
//         process.env.API_BASE_URL ?? 'https://mock.chongchong.com',
//       ),
//       'process.env.KAKAO_REST_API_KEY': JSON.stringify(process.env.KAKAO_REST_API_KEY ?? ''),
//       'process.env.POSTHOG_HOST': JSON.stringify(process.env.POSTHOG_HOST),
//       'process.env.POSTHOG_PROJECT_TOKEN': JSON.stringify(process.env.POSTHOG_PROJECT_TOKEN),
//     }),
//   ],
//   devServer: {
//     static: [
//       {
//         directory: path.join(__dirname, 'dist'),
//       },
//       {
//         directory: path.join(__dirname, 'public'),
//       },
//     ],
//     port: 3005,
//     open: true,
//     hot: true,
//     historyApiFallback: true,
//     client: {
//       overlay: true,
//     },
//   },
// };

import { existsSync } from 'node:fs';
import path from 'path';
import process from 'node:process';
import { fileURLToPath } from 'url';
import HtmlWebpackPlugin from 'html-webpack-plugin';
import webpack from 'webpack';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default (_, argv) => {
  const mode = argv.mode ?? 'development';
  const envFile = mode === 'production' ? '.env.production' : '.env.local';
  const envPath = path.resolve(__dirname, envFile);

  // 배포 플랫폼에 등록된 환경 변수는 유지하고,
  // 없는 값만 env 파일에서 불러옵니다.
  if (existsSync(envPath)) {
    process.loadEnvFile(envPath);
  }

  return {
    mode,
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
            filename: 'assets/[name][ext]',
          },
        },
      ],
    },

    output: {
      filename: 'bundle.js',
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
      }),
    ],

    devServer: {
      static: [
        {
          directory: path.join(__dirname, 'dist'),
        },
        {
          directory: path.join(__dirname, 'public'),
        },
      ],
      port: 3005,
      open: true,
      hot: true,
      historyApiFallback: true,
      client: {
        overlay: true,
      },
    },
  };
};
