import { useSyncExternalStore } from 'react';
import { currenciesInUse } from '../constants/currency';
import { walletCurrency } from '../constants/transaction';

// ac: web-currency-books - the same books as Android's BookStore. The selected book is COMBINED, the
// summary of every currency in the home currency, or a currency code, the book of that currency. One
// switch under the navbar picks the book and every view follows it. The app opens on COMBINED.
export const BOOK = { COMBINED: 'COMBINED' };

let current = BOOK.COMBINED;
const listeners = new Set();

// The selected book. A code that is no longer in use (its last account was removed) counts as COMBINED.
export function getBook() {
  return current === BOOK.COMBINED || currenciesInUse().includes(current) ? current : BOOK.COMBINED;
}

export function setBook(book) {
  if (book === current) return;
  current = book;
  listeners.forEach(l => l());
}

function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useBook() {
  return useSyncExternalStore(subscribe, getBook, getBook);
}

// The currency of a book, or null for COMBINED.
export function bookCurrency(book) {
  return book === BOOK.COMBINED ? null : book;
}

// True when the wallet belongs to the book. COMBINED holds every wallet.
export function bookHoldsWallet(book, walletId) {
  const code = bookCurrency(book);
  return code == null || walletCurrency(walletId) === code;
}

// A transaction belongs to a book when it leaves or reaches one of the book's wallets, so a transfer
// between two currencies shows in both books.
export function bookHoldsTransaction(book, row) {
  return bookHoldsWallet(book, row.wallet) || (row.to_wallet != null && row.to_wallet !== '' && bookHoldsWallet(book, row.to_wallet));
}
