import { renderHook, act } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import { useMerchantHistory } from './useMerchantHistory';

const STORAGE_KEY = 'fintrack_merchant_history';

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  localStorage.clear();
  vi.restoreAllMocks();
});

describe('useMerchantHistory', () => {
  it('starts empty when localStorage is empty', () => {
    const { result } = renderHook(() => useMerchantHistory());
    const [history] = result.current;
    expect(history).toEqual([]);
  });

  it('addToHistory adds a merchant and it appears in history', () => {
    const { result } = renderHook(() => useMerchantHistory());
    act(() => {
      result.current[1]('Grab');
    });
    const [history] = result.current;
    expect(history).toContain('Grab');
  });

  it('addToHistory deduplicates case-insensitively (newest occurrence wins)', () => {
    const { result } = renderHook(() => useMerchantHistory());
    act(() => { result.current[1]('grab'); });
    act(() => { result.current[1]('LINE MAN'); });
    act(() => { result.current[1]('Grab'); });
    const [history] = result.current;
    // 'Grab' should appear only once (newest spelling), at the front
    const grabMatches = history.filter((m) => m.toLowerCase() === 'grab');
    expect(grabMatches).toHaveLength(1);
    expect(history[0]).toBe('Grab');
  });

  it('addToHistory caps at 10 entries (11th drops the oldest)', () => {
    const { result } = renderHook(() => useMerchantHistory());
    act(() => {
      for (let i = 1; i <= 11; i++) {
        result.current[1](`Merchant ${i}`);
      }
    });
    const [history] = result.current;
    expect(history).toHaveLength(10);
    // Oldest entry ('Merchant 1') should be gone
    expect(history).not.toContain('Merchant 1');
    // Most recent entry should be first
    expect(history[0]).toBe('Merchant 11');
  });

  it('addToHistory skips empty strings', () => {
    const { result } = renderHook(() => useMerchantHistory());
    act(() => { result.current[1](''); });
    const [history] = result.current;
    expect(history).toHaveLength(0);
  });

  it('addToHistory skips whitespace-only strings', () => {
    const { result } = renderHook(() => useMerchantHistory());
    act(() => { result.current[1]('   '); });
    const [history] = result.current;
    expect(history).toHaveLength(0);
  });

  it('initializes from localStorage on mount', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(['Shopee', 'Lazada']));
    const { result } = renderHook(() => useMerchantHistory());
    const [history] = result.current;
    expect(history).toEqual(['Shopee', 'Lazada']);
  });

  it('persists to localStorage after addToHistory', () => {
    const { result } = renderHook(() => useMerchantHistory());
    act(() => { result.current[1]('7-Eleven'); });
    const stored = JSON.parse(localStorage.getItem(STORAGE_KEY));
    expect(stored).toContain('7-Eleven');
  });
});
