import { useRef, useState } from 'react';
import { formatDisplayDate, parseDisplayDate } from '../utils/date';

// ac: date-field-display-format — shows the date as dd MMM yyyy. The value stays ISO. The user can type
// a date or press the calendar button to pick one. onChange gets an event-like object, so the form's
// handleChange works as it does for the other fields.
export default function DateInput({ id, name, value, onChange, required }) {
  // draft holds what the user is typing. It is null otherwise, so the field shows the value itself.
  const [draft, setDraft] = useState(null);
  const pickerRef = useRef(null);

  function commit(iso) {
    onChange({ target: { name, value: iso } });
  }

  function handleBlur() {
    const iso = draft === null ? null : parseDisplayDate(draft);
    if (iso) commit(iso);
    setDraft(null);
  }

  return (
    <div className="date-input">
      <input
        className="form-input"
        id={id}
        name={name}
        type="text"
        inputMode="text"
        placeholder="dd MMM yyyy"
        autoComplete="off"
        value={draft ?? formatDisplayDate(value)}
        onChange={(e) => setDraft(e.target.value)}
        onBlur={handleBlur}
        required={required}
      />
      <button
        type="button"
        className="date-input-button"
        aria-label="Pick a date"
        onClick={() => pickerRef.current?.showPicker?.()}
      >
        &#128197;
      </button>
      <input
        ref={pickerRef}
        className="date-input-native"
        type="date"
        tabIndex={-1}
        aria-hidden="true"
        value={value || ''}
        onChange={(e) => { if (e.target.value) commit(e.target.value); }}
      />
    </div>
  );
}
