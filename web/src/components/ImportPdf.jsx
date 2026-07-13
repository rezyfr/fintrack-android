import { useState } from 'react';
import { parseStatement } from '../utils/pdfParser';
import { addTransactions } from '../api/supabase';
import { WALLETS, TX_TYPES, categoriesFor, deriveTab } from '../constants/transaction';

export default function ImportPdf() {
  const [selectedWallet, setSelectedWallet] = useState('MANDIRI_CC');
  const [file, setFile]                     = useState(null);
  const [step, setStep]                     = useState('upload');
  const [format, setFormat]                 = useState('');
  const [rows, setRows]                     = useState([]);
  const [loading, setLoading]               = useState(false);
  const [status, setStatus]                 = useState(null);
  const [errorMsg, setErrorMsg]             = useState('');
  const [needsPassword, setNeedsPassword]   = useState(false);
  const [password, setPassword]             = useState('');

  async function handleExtract(pw = '') {
    setLoading(true);
    setStatus(null);
    setErrorMsg('');
    try {
      const result = await parseStatement(file, pw, selectedWallet);
      setFormat(result.format);
      setRows(result.rows.map((r, i) => ({ ...r, txType: r.tx_type, toWallet: '', selected: true, id: i })));
      setNeedsPassword(false);
      setPassword('');
      setStep('preview');
    } catch (err) {
      if (err.name === 'PasswordException' || /password/i.test(err.message)) {
        setNeedsPassword(true);
        setStatus(null);
      } else {
        setStatus('error');
        setErrorMsg(err.message);
      }
    } finally {
      setLoading(false);
    }
  }

  function updateRow(id, field, value) {
    setRows(rs => rs.map(r => r.id === id ? { ...r, [field]: value } : r));
  }

  async function handleImport() {
    setLoading(true);
    setStatus(null);
    setErrorMsg('');
    try {
      const payload = rows
        .filter(r => r.selected)
        .map(r => ({
          item:      r.item,
          amount:    r.amount,
          category:  r.category,
          date:      r.date,
          wallet:    selectedWallet,
          tx_type:   r.txType,
          to_wallet: r.txType === 'transfer' && r.toWallet ? r.toWallet : null,
          tab:       deriveTab(selectedWallet, r.txType),
          note:      null,
        }));
      await addTransactions(payload);
      setStatus('success');
      setRows([]);
      setFile(null);
      setStep('upload');
    } catch (err) {
      setStatus('error');
      setErrorMsg(err.message);
    } finally {
      setLoading(false);
    }
  }

  const selectedCount = rows.filter(r => r.selected).length;

  return (
    <div className="form-page">
      <div className={`form-card${step === 'preview' ? ' form-card-wide' : ''}`}>
        <h1 className="form-title">Import PDF Statement</h1>

        {status === 'success' && (
          <div className="toast toast-success" role="status">
            {selectedCount === 0 ? 'Imported successfully.' : `${selectedCount} transactions imported successfully.`}
          </div>
        )}
        {status === 'error' && (
          <div className="toast toast-error" role="alert">
            {errorMsg}
          </div>
        )}

        {needsPassword && (
          <div className="form-group" style={{ marginBottom: '1rem' }}>
            <p style={{ marginBottom: '0.5rem', fontSize: '0.875rem' }}>
              This PDF is password-protected. Enter the password to continue.
            </p>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <input
                className="form-input"
                type="password"
                placeholder="PDF password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleExtract(password)}
                autoFocus
              />
              <button
                className="btn-primary"
                onClick={() => handleExtract(password)}
                disabled={!password || loading}
              >
                {loading ? 'Unlocking...' : 'Unlock'}
              </button>
            </div>
          </div>
        )}

        {step === 'upload' && !needsPassword && (
          <div className="form-grid">
            <div className="form-group">
              <label className="form-label" htmlFor="wallet">Wallet</label>
              <select
                className="form-select"
                id="wallet"
                value={selectedWallet}
                onChange={e => setSelectedWallet(e.target.value)}
              >
                {WALLETS.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="pdf-file">PDF Statement</label>
              <input
                className="form-input"
                id="pdf-file"
                type="file"
                accept=".pdf"
                onChange={e => setFile(e.target.files[0] || null)}
              />
            </div>

            <div className="form-footer">
              <button
                className="btn-primary"
                onClick={() => handleExtract()}
                disabled={!file || loading}
              >
                {loading ? 'Extracting...' : 'Extract Transactions'}
              </button>
            </div>
          </div>
        )}

        {step === 'preview' && (
          <div>
            <div style={{ marginBottom: '0.75rem' }}>
              <span className="wallet-chip">{format}</span>
              <span style={{ marginLeft: '0.5rem', fontSize: '0.875rem', opacity: 0.7 }}>
                {rows.length} rows found
              </span>
            </div>

            <div style={{ overflowX: 'auto' }}>
              <table className="tx-table">
                <thead>
                  <tr>
                    <th></th>
                    <th>Date</th>
                    <th>Item</th>
                    <th>Amount</th>
                    <th>Type</th>
                    <th>To Wallet</th>
                    <th>Category</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map(r => (
                    <tr key={r.id}>
                      <td>
                        <input
                          type="checkbox"
                          checked={r.selected}
                          onChange={e => updateRow(r.id, 'selected', e.target.checked)}
                        />
                      </td>
                      <td>
                        <input
                          className="form-input"
                          type="date"
                          value={r.date}
                          onChange={e => updateRow(r.id, 'date', e.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          className="form-input"
                          type="text"
                          value={r.item}
                          onChange={e => updateRow(r.id, 'item', e.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          className="form-input"
                          type="number"
                          value={r.amount}
                          onChange={e => updateRow(r.id, 'amount', Number(e.target.value))}
                        />
                      </td>
                      <td>
                        <select
                          className="form-select"
                          value={r.txType}
                          onChange={e => updateRow(r.id, 'txType', e.target.value)}
                        >
                          {TX_TYPES.map(t => <option key={t.id} value={t.id}>{t.label}</option>)}
                        </select>
                      </td>
                      <td>
                        {r.txType === 'transfer' ? (
                          <select
                            className="form-select"
                            value={r.toWallet}
                            onChange={e => updateRow(r.id, 'toWallet', e.target.value)}
                          >
                            <option value="">Select…</option>
                            {WALLETS.filter(w => w.id !== selectedWallet).map(w => (
                              <option key={w.id} value={w.id}>{w.name}</option>
                            ))}
                          </select>
                        ) : (
                          <span style={{ opacity: 0.4 }}>—</span>
                        )}
                      </td>
                      <td>
                        <select
                          className="form-select"
                          value={r.category}
                          onChange={e => updateRow(r.id, 'category', e.target.value)}
                        >
                          {categoriesFor(r.txType).map(c => <option key={c}>{c}</option>)}
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="form-footer" style={{ gap: '0.5rem', display: 'flex' }}>
              <button className="btn-secondary" onClick={() => setStep('upload')}>
                Back
              </button>
              <button
                className="btn-primary"
                onClick={handleImport}
                disabled={loading || selectedCount === 0}
              >
                {loading ? 'Importing...' : `Import ${selectedCount} transactions`}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
