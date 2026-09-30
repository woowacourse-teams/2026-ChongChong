export interface CsrfResponse {
  headerName: string;
  token: string;
}

export interface LoginResponse {
  tokenType: 'Bearer';
  accessToken: string;
  accessTokenExpiresAt: string;
  userId: number;
}

export interface RotateAccessTokenResponse {
  tokenType: 'Bearer';
  accessToken: string;
  accessTokenExpiresAt: string;
}
