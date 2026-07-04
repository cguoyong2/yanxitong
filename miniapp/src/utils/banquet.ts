import { request } from '../api/client';
import { writeActiveEventType } from './event-theme';

interface BanquetLite {
  id: number;
  name?: string;
  eventTypeCode?: string;
  themeCode?: string;
  banquetTime?: string;
  location?: string;
}

export interface BanquetContext {
  id: number;
  name?: string;
  eventTypeCode?: string;
  themeCode?: string;
  banquetTime?: string;
  location?: string;
  invitationId?: number;
  shareSlug?: string;
  updatedAt: number;
}

export const LAST_BANQUET_CONTEXT_KEY = 'yanxitong-last-banquet-context';
export const OPEN_LATEST_INVITATION_KEY = 'yanxitong-open-latest-invitation';

export function readLastBanquetContext(): BanquetContext | undefined {
  const raw = uni.getStorageSync(LAST_BANQUET_CONTEXT_KEY);
  if (!raw) {
    return undefined;
  }
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw) as BanquetContext;
    } catch {
      return undefined;
    }
  }
  return raw as BanquetContext;
}

export function writeLastBanquetContext(input: Partial<BanquetContext> & { id: number }) {
  const previous = readLastBanquetContext();
  const next: BanquetContext = {
    ...previous,
    ...input,
    id: Number(input.id),
    updatedAt: Date.now()
  };
  uni.setStorageSync(LAST_BANQUET_CONTEXT_KEY, next);
  if (next.eventTypeCode) {
    writeActiveEventType(next.eventTypeCode);
  }
  return next;
}

export async function resolveBanquetId(candidate?: string) {
  if (candidate) {
    const numericId = Number(candidate);
    if (numericId) {
      writeLastBanquetContext({ id: numericId });
    }
    return candidate;
  }
  const banquets = await request<BanquetLite[]>('/banquets').catch(() => []);
  const latest = banquets[0];
  if (latest?.id) {
    writeLastBanquetContext(latest);
  }
  if (!latest?.id) {
    const cached = readLastBanquetContext();
    return cached?.id ? String(cached.id) : '';
  }
  return latest?.id ? String(latest.id) : '';
}

export function requireBanquetToast() {
  uni.showToast({ title: '请先创建宴席', icon: 'none' });
}
