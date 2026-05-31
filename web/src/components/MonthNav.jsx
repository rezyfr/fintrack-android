// ac: month-nav-buttons — clicking ‹ moves the selected month one month backward
// ac: month-nav-buttons — clicking › moves the selected month one month forward
// ac: month-nav-buttons — the › button is disabled when the displayed month equals the current calendar month
// ac: month-nav-buttons — the month text input between the buttons remains directly editable by hand
// ac: month-nav-buttons — both the transaction list and the wallet balance views use the month navigation controls

function currentMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function stepMonth(ym, delta) {
  const [y, m] = ym.split('-').map(Number);
  const date = new Date(y, m - 1 + delta, 1);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

export default function MonthNav({ value, onChange }) {
  const atCurrent = value >= currentMonth();
  return (
    <div className="month-nav">
      <button
        className="month-nav-btn"
        aria-label="Previous month"
        onClick={() => onChange(stepMonth(value, -1))}
      >
        ‹
      </button>
      <input
        className="month-input"
        type="month"
        value={value}
        onChange={e => onChange(e.target.value)}
      />
      <button
        className="month-nav-btn"
        aria-label="Next month"
        onClick={() => onChange(stepMonth(value, 1))}
        disabled={atCurrent}
      >
        ›
      </button>
    </div>
  );
}
