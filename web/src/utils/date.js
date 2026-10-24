// ac: date-field-display-format — the date shown in the Add and Edit forms is dd MMM yyyy, for example
// 06 Oct 2026. Stored values stay ISO (yyyy-mm-dd). Month names are fixed, so the text does not depend
// on the browser locale.
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export function formatDisplayDate(iso) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso ?? '');
  if (!m) return '';
  const month = MONTHS[Number(m[2]) - 1];
  return month ? `${m[3]} ${month} ${m[1]}` : '';
}

function isRealDate(y, mo, d) {
  const dt = new Date(Date.UTC(y, mo - 1, d));
  return dt.getUTCFullYear() === y && dt.getUTCMonth() === mo - 1 && dt.getUTCDate() === d;
}

// Reads "06 Oct 2026" (day with or without a leading zero, month in any case) or an ISO date.
// Returns the ISO date, or null when the text is not a real date.
export function parseDisplayDate(text) {
  const t = (text ?? '').trim();
  const iso = /^(\d{4})-(\d{2})-(\d{2})$/.exec(t);
  if (iso) return isRealDate(+iso[1], +iso[2], +iso[3]) ? t : null;
  const m = /^(\d{1,2})\s+([A-Za-z]{3})\s+(\d{4})$/.exec(t);
  if (!m) return null;
  const mo = MONTHS.findIndex((x) => x.toLowerCase() === m[2].toLowerCase()) + 1;
  if (!mo || !isRealDate(+m[3], mo, +m[1])) return null;
  return `${m[3]}-${String(mo).padStart(2, '0')}-${m[1].padStart(2, '0')}`;
}
