import { useState, useEffect } from 'react';
import { getTransactions } from '../api/supabase';

const TAB_OPTIONS = ['EXPENSES', 'IDR_EXPENSES', 'INCOME', 'IDR_INCOME'];

function currentMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

export default function TransactionList() {
  const [tab, setTab] = useState('');
  const [month, setMonth] = useState(currentMonth());
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    getTransactions({ tab: tab || null, month })
      .then(setRows)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [tab, month]);

  return (
    <div>
      <div>
        <label htmlFor="tab-filter">Tab</label>
        <select
          id="tab-filter"
          value={tab}
          onChange={(e) => setTab(e.target.value)}
        >
          <option value="">All</option>
          {TAB_OPTIONS.map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
        <label htmlFor="month-filter">Month</label>
        <input
          id="month-filter"
          type="month"
          value={month}
          onChange={(e) => setMonth(e.target.value)}
        />
      </div>
      {loading && <p>Loading...</p>}
      {error && <p role="alert">{error}</p>}
      {!loading && !error && (
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Merchant</th>
              <th>Item</th>
              <th>Amount</th>
              <th>Category</th>
              <th>Channel</th>
              <th>Tab</th>
              <th>Note</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>{row.date}</td>
                <td>{row.merchant}</td>
                <td>{row.item}</td>
                <td>{row.amount}</td>
                <td>{row.category}</td>
                <td>{row.channel}</td>
                <td>{row.tab}</td>
                <td>{row.note}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
