import { BOOK, getBook, setBook, bookCurrency, bookHoldsWallet, bookHoldsTransaction } from './book';
import { applyCatalog } from '../constants/transaction';

// The same cases as Android's BookStoreTest. BBL is THB, BCA is IDR (home).
afterEach(() => setBook(BOOK.COMBINED));

it('opens on Combined', () => expect(getBook()).toBe(BOOK.COMBINED));

it('holds every wallet in Combined', () => {
  expect(bookCurrency(BOOK.COMBINED)).toBeNull();
  expect(bookHoldsWallet(BOOK.COMBINED, 'BBL')).toBe(true);
  expect(bookHoldsWallet(BOOK.COMBINED, 'BCA')).toBe(true);
});

it('holds only its own currency in a book', () => {
  expect(bookCurrency('THB')).toBe('THB');
  expect(bookHoldsWallet('THB', 'BBL')).toBe(true);
  expect(bookHoldsWallet('THB', 'BCA')).toBe(false);
  expect(bookHoldsWallet('IDR', 'BCA')).toBe(true);
});

it('shows a transfer between two currencies in both books', () => {
  const transfer = { wallet: 'BBL', to_wallet: 'BCA' };
  expect(bookHoldsTransaction('IDR', transfer)).toBe(true);
  expect(bookHoldsTransaction('THB', transfer)).toBe(true);
  expect(bookHoldsTransaction('THB', { wallet: 'BCA', to_wallet: 'MANDIRI' })).toBe(false);
});

// ac: web-currency-books - a book exists for every currency in use, so a third currency has its own book
it('keeps the book of a third currency while an account holds it', () => {
  applyCatalog({
    accounts: [
      { id: 'BCA', name: 'BCA', currency: 'IDR' },
      { id: 'BBL', name: 'BBL', currency: 'THB' },
      { id: 'US', name: 'US', currency: 'USD' },
    ],
  });
  setBook('USD');
  expect(getBook()).toBe('USD');
  expect(bookHoldsWallet('USD', 'US')).toBe(true);
  expect(bookHoldsWallet('USD', 'BBL')).toBe(false);
});

// ac: web-currency-books - when the currency of the selected book is no longer in use, the book is Combined
it('falls back to Combined when the selected currency disappears', () => {
  applyCatalog({
    accounts: [
      { id: 'BCA', name: 'BCA', currency: 'IDR' },
      { id: 'US', name: 'US', currency: 'USD' },
    ],
  });
  setBook('USD');
  expect(getBook()).toBe('USD');
  applyCatalog({ accounts: [{ id: 'BCA', name: 'BCA', currency: 'IDR' }] });
  expect(getBook()).toBe(BOOK.COMBINED);
  applyCatalog({ accounts: [{ id: 'BCA', name: 'BCA', currency: 'IDR' }, { id: 'US', name: 'US', currency: 'USD' }] });
  expect(getBook()).toBe('USD');
});
