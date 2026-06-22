import axios from 'axios';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface ConfirmScreenStatus {
  banquetId: number;
  bindCode: string;
  bindStatus: string;
  deviceType: string;
  online: boolean;
  onlineSessions: number;
}

export interface ConfirmScreenGiftEvent {
  type: string;
  banquetId: number;
  giftRecordId: number;
  guestName: string;
  amount: number;
  message?: string;
  paidAt: string;
}

const client = axios.create({
  baseURL: '/api',
  timeout: 10000
});

export async function bindConfirmScreen(payload: {
  banquetId: number;
  bindCode: string;
}): Promise<ConfirmScreenStatus> {
  const { data } = await client.post<ApiResponse<ConfirmScreenStatus>>('/confirm-screen/bind', payload);
  if (data.code !== 0) {
    throw new Error(data.message || '绑定失败');
  }
  return data.data;
}

export async function getConfirmScreenStatus(bindCode: string): Promise<ConfirmScreenStatus> {
  const { data } = await client.get<ApiResponse<ConfirmScreenStatus>>(
    `/confirm-screen/status/${encodeURIComponent(bindCode)}`
  );
  if (data.code !== 0) {
    throw new Error(data.message || '读取绑定状态失败');
  }
  return data.data;
}

export async function getLatestConfirmScreenEvent(banquetId: number): Promise<ConfirmScreenGiftEvent | null> {
  const { data } = await client.get<ApiResponse<ConfirmScreenGiftEvent | null>>(
    `/confirm-screen/banquets/${encodeURIComponent(banquetId)}/latest-event`
  );
  if (data.code !== 0) {
    throw new Error(data.message || '读取最近礼金事件失败');
  }
  return data.data;
}
