import { useState } from 'react';

const STORAGE_KEY = 'fintrack_merchant_history';
const MAX_HISTORY = 10;

function loadHistory() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function saveHistory(history) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
  } catch {
    // ignore storage errors
  }
}

export function useMerchantHistory() {
  const [history, setHistory] = useState(() => loadHistory());

  function addToHistory(merchant) {
    const trimmed = typeof merchant === 'string' ? merchant.trim() : '';
    if (!trimmed) return;

    setHistory((prev) => {
      const lower = trimmed.toLowerCase();
      // Remove any existing entry that matches case-insensitively, then prepend
      const deduped = prev.filter((m) => m.toLowerCase() !== lower);
      const next = [trimmed, ...deduped].slice(0, MAX_HISTORY);
      saveHistory(next);
      return next;
    });
  }

  return [history, addToHistory];
}
