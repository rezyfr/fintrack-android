import { useState, useEffect, useCallback } from 'react';
import { getAccounts, saveAccount, deleteAccount } from '../api/client';
import { ACCOUNT_TYPES, accountType } from '../constants/transaction';
import { refreshCatalog } from '../hooks/useCatalog';
import { deriveAccountId } from '../utils/accounts';
import CurrencySettingsForm from './CurrencySettingsForm';

// ac: web-manage-accounts — Manage Accounts lists every account with its currency, and adds,
// renames, and deletes custom accounts
export default function ManageAccounts() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [name, setName] = useState('');
  const [currency, setCurrency] = useState('');
  const [type, setType] = useState('checking');
  const [editingId, setEditingId] = useState(null);
  const [editName, setEditName] = useState('');
  const [editType, setEditType] = useState('checking');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await getAccounts();
      setRows(data);
      setError(null);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  async function run(action) {
    setBusy(true);
    try {
      await action();
      await refreshCatalog();
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  // ac: web-manage-accounts - the user can add a custom account with a name, a currency and a type
  // (checking, credit card or investment). The id is
  // derived from the name. An id that already exists is rejected, because saving would overwrite
  // that account.
  function handleAdd(e) {
    e.preventDefault();
    const trimmed = name.trim();
    const code = currency.trim().toUpperCase();
    if (!trimmed) return setError('Name is required');
    if (!code) return setError('Currency is required');
    const id = deriveAccountId(trimmed);
    if (!id) return setError('Name must contain a letter or a digit');
    if (rows.some(r => r.id === id)) return setError(`An account with the id ${id} already exists`);
    const sortOrder = rows.reduce((max, r) => Math.max(max, r.sort_order ?? 0), -1) + 1;
    run(async () => {
      await saveAccount({ id, name: trimmed, currency: code, type, sort_order: sortOrder, is_default: false });
      setName('');
      setCurrency('');
      setType('checking');
    });
  }

  // ac: web-manage-accounts - the user can rename a custom account and change the type of any account.
  // The id stays the same, so transactions already stored against it keep resolving to the same account.
  function handleRename(row) {
    const trimmed = row.is_default ? row.name : editName.trim();
    if (!trimmed) return setError('Name is required');
    run(async () => {
      await saveAccount({
        id: row.id, name: trimmed, currency: row.currency, type: editType,
        sort_order: row.sort_order ?? 0, is_default: !!row.is_default,
      });
      setEditingId(null);
    });
  }

  // ac: web-manage-accounts — the user can delete a custom account. A default account has no Delete
  // button, and the guard below stops a delete that reaches it anyway.
  function handleDelete(row) {
    if (row.is_default) return setError(`${row.name} is a default account and cannot be deleted`);
    run(() => deleteAccount(row.id));
  }

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Accounts</h1>
        <span className="section-count">{rows.length}</span>
      </div>

      <CurrencySettingsForm />

      {error && (
        <p className="table-state-desc error" role="alert">
          {error} <button className="btn-small" onClick={() => setError(null)}>Dismiss</button>
        </p>
      )}

      <form className="manage-add" onSubmit={handleAdd}>
        <input
          className="form-input"
          placeholder="Account name"
          aria-label="Account name"
          value={name}
          onChange={e => setName(e.target.value)}
        />
        <input
          className="form-input manage-currency"
          placeholder="Currency (for example USD)"
          aria-label="Currency"
          value={currency}
          onChange={e => setCurrency(e.target.value)}
        />
        <select
          className="form-select"
          aria-label="Account type"
          value={type}
          onChange={e => setType(e.target.value)}
        >
          {ACCOUNT_TYPES.map(t => <option key={t.id} value={t.id}>{t.label}</option>)}
        </select>
        <button className="btn-small btn-small-primary" type="submit" disabled={busy}>Add account</button>
      </form>

      {loading ? (
        <p className="table-state-desc">Loading accounts...</p>
      ) : (
        <div className="table-wrap">
          <table className="tx-table manage-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Id</th>
                <th>Currency</th>
                <th>Type</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {rows.map(row => (
                <tr key={row.id}>
                  <td>
                    {editingId === row.id && !row.is_default ? (
                      <input
                        className="form-input"
                        aria-label={`New name for ${row.name}`}
                        value={editName}
                        onChange={e => setEditName(e.target.value)}
                        autoFocus
                      />
                    ) : row.name}
                    {row.is_default && <span className="chip chip-other manage-badge">Default</span>}
                  </td>
                  <td>{row.id}</td>
                  <td>{row.currency}</td>
                  <td>
                    {editingId === row.id ? (
                      <select
                        className="form-select"
                        aria-label={`Type for ${row.name}`}
                        value={editType}
                        onChange={e => setEditType(e.target.value)}
                      >
                        {ACCOUNT_TYPES.map(t => <option key={t.id} value={t.id}>{t.label}</option>)}
                      </select>
                    ) : ACCOUNT_TYPES.find(t => t.id === accountType(row))?.label}
                  </td>
                  <td className="manage-actions">
                    {editingId === row.id && (
                      <>
                        <button className="btn-small btn-small-primary" disabled={busy} onClick={() => handleRename(row)}>Save</button>
                        <button className="btn-small" onClick={() => setEditingId(null)}>Cancel</button>
                      </>
                    )}
                    {!row.is_default && editingId !== row.id && (
                      <>
                        <button className="btn-small" onClick={() => { setEditingId(row.id); setEditName(row.name); setEditType(accountType(row)); }}>Rename</button>
                        <button className="btn-small" disabled={busy} onClick={() => handleDelete(row)}>Delete</button>
                      </>
                    )}
                    {row.is_default && editingId !== row.id && (
                      <button className="btn-small" onClick={() => { setEditingId(row.id); setEditName(row.name); setEditType(accountType(row)); }}>Change type</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
