import { useState } from 'react';
import { addTransaction } from '../api/supabase';

const CATEGORIES = [
  'Bills', 'Subscriptions', 'Entertainment', 'Food & Drink',
  'Groceries', 'Health & Wellbeing', 'Other', 'Shopping',
  'Transport', 'Travel', 'Business', 'Gifts',
];
const CHANNELS = ['BillPayment', 'eWallet', 'PromptPay', 'BankTransfer', 'Manual', 'Unknown'];
const TABS = ['EXPENSES', 'IDR_EXPENSES', 'INCOME', 'IDR_INCOME'];

function today() {
  return new Date().toISOString().slice(0, 10);
}

const EMPTY = {
  merchant: '',
  item: '',
  amount: '',
  category: 'Food & Drink',
  channel: 'eWallet',
  tab: 'EXPENSES',
  date: today(),
  note: '',
};

export default function AddTransactionForm() {
  const [form, setForm] = useState(EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [status, setStatus] = useState(null); // null | 'success' | 'error'
  const [errorMsg, setErrorMsg] = useState('');

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setStatus(null);
    try {
      await addTransaction({ ...form, amount: Number(form.amount) });
      setStatus('success');
      setForm({ ...EMPTY, date: today() });
    } catch (err) {
      setStatus('error');
      setErrorMsg(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      {status === 'success' && <p role="status">Transaction added.</p>}
      {status === 'error' && <p role="alert">{errorMsg}</p>}

      <label htmlFor="merchant">Merchant</label>
      <input id="merchant" name="merchant" value={form.merchant} onChange={handleChange} required />

      <label htmlFor="item">Item</label>
      <input id="item" name="item" value={form.item} onChange={handleChange} required />

      <label htmlFor="amount">Amount</label>
      <input id="amount" name="amount" type="number" step="0.01" value={form.amount} onChange={handleChange} required />

      <label htmlFor="category">Category</label>
      <select id="category" name="category" value={form.category} onChange={handleChange}>
        {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
      </select>

      <label htmlFor="channel">Channel</label>
      <select id="channel" name="channel" value={form.channel} onChange={handleChange}>
        {CHANNELS.map((c) => <option key={c}>{c}</option>)}
      </select>

      <label htmlFor="tab">Tab</label>
      <select id="tab" name="tab" value={form.tab} onChange={handleChange}>
        {TABS.map((t) => <option key={t}>{t}</option>)}
      </select>

      <label htmlFor="date">Date</label>
      <input id="date" name="date" type="date" value={form.date} onChange={handleChange} required />

      <label htmlFor="note">Note</label>
      <textarea id="note" name="note" value={form.note} onChange={handleChange} />

      <button type="submit" disabled={submitting}>
        {submitting ? 'Saving...' : 'Add Transaction'}
      </button>
    </form>
  );
}
