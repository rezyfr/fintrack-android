import { useState } from 'react';

export default function MerchantInput({ value, onChange, history, onSelect, id, name, placeholder, required }) {
  const [open, setOpen] = useState(false);

  const suggestions = open && history.length > 0
    ? history.filter((m) =>
        value.trim() === ''
          ? true
          : m.toLowerCase().includes(value.toLowerCase())
      )
    : [];

  function handleFocus() {
    setOpen(true);
  }

  function handleBlur() {
    setTimeout(() => setOpen(false), 100);
  }

  function handleChange(e) {
    onChange(e);
  }

  function handleSelect(merchant) {
    onSelect(merchant);
    setOpen(false);
  }

  return (
    <div style={{ position: 'relative' }}>
      <input
        className="form-input"
        id={id}
        name={name}
        value={value}
        onChange={handleChange}
        onFocus={handleFocus}
        onBlur={handleBlur}
        placeholder={placeholder}
        required={required}
        autoComplete="off"
      />
      {open && suggestions.length > 0 && (
        <ul className="merchant-suggestions">
          {suggestions.map((merchant) => (
            <li
              key={merchant}
              className="merchant-suggestion-item"
              onMouseDown={() => handleSelect(merchant)}
            >
              {merchant}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
