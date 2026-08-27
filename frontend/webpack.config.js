import { existsSync } from 'node:fs';
import path from 'path';
import process from 'node:process';
import { fileURLToPath } from 'url';
import HtmlWebpackPlugin from 'html-webpack-plugin';
import webpack from 'webpack';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const envPath = path.resolve(__dirname, '.env');

if (existsSync(envPath)) {
  process.loadEnvFile(envPath);
}

export default {
  mode: 'development',
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
    proxy: [
      {
        context: ['/auth/csrf', '/auth/login', '/auth/refresh', '/auth/logout'],
        target: process.env.API_BASE_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    ],
    client: {
      overlay: true,
    },
  },
};
