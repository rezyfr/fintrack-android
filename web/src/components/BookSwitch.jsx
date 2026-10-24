import { BOOK, useBook, setBook } from '../utils/book';
import { currenciesInUse, labelFor } from '../constants/currency';

// ac: web-currency-books - the one switch for the whole app: Combined, then one book for each currency
// in use, home first, by the label from the currency settings. With one currency in use there is
// nothing to switch. The chips wrap onto more lines on a narrow phone.
export default function BookSwitch() {
  const book = useBook();
  const codes = currenciesInUse();
  if (codes.length < 2) return null;
  const options = [
    { id: BOOK.COMBINED, label: 'Combined' },
    ...codes.map(code => ({ id: code, label: labelFor(code) })),
  ];
  return (
    <div className="book-switch">
      <div className="filter-tabs" role="group" aria-label="Book">
        {options.map(o => (
          <button
            key={o.id}
            className={`filter-tab${book === o.id ? ' active' : ''}`}
            aria-pressed={book === o.id}
            onClick={() => setBook(o.id)}
          >
            {o.label}
          </button>
        ))}
      </div>
    </div>
  );
}
