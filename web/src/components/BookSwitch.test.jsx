import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import BookSwitch from './BookSwitch';
import { applyCatalog } from '../constants/transaction';
import { DEFAULT_CURRENCY_SETTINGS, saveCurrencySettings } from '../constants/currency';
import { BOOK, getBook, setBook } from '../utils/book';

beforeEach(() => {
  vi.stubGlobal('localStorage', { getItem: () => null, setItem: () => {} });
  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: true })));
});
afterEach(() => {
  setBook(BOOK.COMBINED);
  saveCurrencySettings(DEFAULT_CURRENCY_SETTINGS);
  vi.unstubAllGlobals();
});

const chips = () => screen.getAllByRole('button').map(b => b.textContent);

// ac: web-currency-books - Combined plus one chip for each currency in use, home first
it('shows Combined and one chip for each of three currencies, home first', async () => {
  saveCurrencySettings({ homeCode: 'IDR', homeLabel: 'Indonesia', currencies: [{ code: 'THB', label: 'Thailand', rate: null }] });
  applyCatalog({
    accounts: [
      { id: 'BBL', name: 'BBL', currency: 'THB' },
      { id: 'BCA', name: 'BCA', currency: 'IDR' },
      { id: 'US', name: 'US', currency: 'USD' },
    ],
  });
  render(<BookSwitch />);
  expect(chips()).toEqual(['Combined', 'Indonesia', 'Thailand', 'USD']);
  await userEvent.click(screen.getByRole('button', { name: 'USD' }));
  expect(getBook()).toBe('USD');
  expect(screen.getByRole('button', { name: 'USD' })).toHaveAttribute('aria-pressed', 'true');
});

it('is hidden when one currency is in use', () => {
  applyCatalog({ accounts: [{ id: 'BCA', name: 'BCA', currency: 'IDR' }] });
  render(<BookSwitch />);
  expect(screen.queryByRole('group', { name: 'Book' })).not.toBeInTheDocument();
});

it('is hidden with no accounts', () => {
  applyCatalog({ accounts: [] });
  render(<BookSwitch />);
  expect(screen.queryByRole('group', { name: 'Book' })).not.toBeInTheDocument();
});
