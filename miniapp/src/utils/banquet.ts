import { request } from '../api/client';

interface BanquetLite {
  id: number;
}

export async function resolveBanquetId(candidate?: string) {
  if (candidate) {
    return candidate;
  }
  const banquets = await request<BanquetLite[]>('/banquets').catch(() => []);
  const latest = banquets[0];
  return latest?.id ? String(latest.id) : '';
}

export function requireBanquetToast() {
  uni.showToast({ title: '请先创建宴席', icon: 'none' });
}
