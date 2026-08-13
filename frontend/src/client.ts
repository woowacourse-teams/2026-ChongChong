import ky from 'ky';

const api = ky.create({
  baseUrl: 'https://api.github.com/',
  hooks: {},
});

export default api;
