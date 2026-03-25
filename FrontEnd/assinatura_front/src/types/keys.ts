export interface PublicKeyItem {
  userId: number;
  email: string;
  publicKey: string;
}

export interface MyKeysResponse {
  userId: number;
  email: string;
  publicKey: string;
  privateKey: string;
}
