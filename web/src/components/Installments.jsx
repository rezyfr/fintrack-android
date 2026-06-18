import { useState, useEffect, useRef } from 'react';
import { getInstallments, updateInstallmentMerchant, incrementInstallmentStep, setInstallmentExcluded } from '../api/supabase';

const IDR = (n) => `Rp ${Math.round(Number(n)).toLocaleString('en')}`;

// ac: installment-name-edit — MANDIRI_CC shown as Mandiri Credit Card; BCA_CC as BCA Credit Card
const WALLET_LABELS = { MANDIRI_CC: 'Mandiri Credit Card', BCA_CC: 'BCA Credit Card' };
function walletLabel(id) { return WALLET_LABELS[id] || id; }

const WALLET_COLORS = { BCA_CC: '#2a6de8', MANDIRI_CC: '#f5a623' };

function nextDueDate(dueDay) {
  const today = new Date();
  const d = new Date(today.getFullYear(), today.getMonth(), dueDay);
  if (d <= today) d.setMonth(d.getMonth() + 1);
  return d;
}
function estimatedFinish(dueDay, remaining) {
  const d = nextDueDate(dueDay);
  d.setMonth(d.getMonth() + remaining - 1);
  return d;
}
function fmtDate(d) { return d.toLocaleDateString('en', { month: 'short', day: 'numeric', year: 'numeric' }); }

function EditIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
    </svg>
  );
}

// ac: installment-overview, ac: installment-name-edit, ac: installment-step-controls
function InstallmentCard({ item, onRename, onIncrementStep, onToggleExclude }) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(item.merchant);
  const [saving, setSaving] = useState(false);
  const inputRef = useRef(null);

  function startEdit() {
    setName(item.merchant);
    setEditing(true);
    setTimeout(() => inputRef.current?.focus(), 0);
  }
  async function save() {
    const trimmed = name.trim();
    if (!trimmed || trimmed === item.merchant) { setEditing(false); return; }
    setSaving(true);
    await onRename(item.id, trimmed);
    setSaving(false);
    setEditing(false);
  }
  function onKeyDown(e) {
    if (e.key === 'Enter') save();
    if (e.key === 'Escape') setEditing(false);
  }

  const remaining = item.total_installments - item.current_step;
  const progress = item.current_step / item.total_installments;
  const nextDue = nextDueDate(item.due_day);
  const finish = estimatedFinish(item.due_day, remaining);
  const walletColor = WALLET_COLORS[item.wallet] || 'var(--text-2)';
  const isExcluded = item.excluded;

  return (
    <div style={{
      background: isExcluded ? 'var(--surface)' : 'var(--card)',
      border: `1px solid ${isExcluded ? 'var(--border)' : 'var(--border)'}`,
      borderRadius: 12,
      padding: '16px 18px',
      display: 'flex',
      flexDirection: 'column',
      gap: 10,
      opacity: isExcluded ? 0.7 : 1,
    }}>
      {/* Merchant + amount */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{ flex: 1, marginRight: 12 }}>
          {editing ? (
            <input
              ref={inputRef}
              value={name}
              onChange={e => setName(e.target.value)}
              onKeyDown={onKeyDown}
              onBlur={save}
              disabled={saving}
              style={{ fontSize: 15, fontWeight: 600, background: 'var(--surface)', border: '1px solid var(--border-hover)', borderRadius: 6, padding: '3px 8px', color: 'var(--text)', outline: 'none', width: '100%' }}
            />
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ fontWeight: 600, fontSize: 15, color: 'var(--text)' }}>{item.merchant}</span>
              <button onClick={startEdit} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-2)', padding: 2, lineHeight: 0 }} aria-label="Edit name">
                <EditIcon />
              </button>
            </div>
          )}
          <div style={{ display: 'inline-block', marginTop: 4, fontSize: 11, fontWeight: 600, padding: '2px 7px', borderRadius: 4, background: walletColor + '22', color: walletColor }}>
            {walletLabel(item.wallet)}
          </div>
          {/* ac: installment-step-controls — excluded badge */}
          {isExcluded && (
            <span style={{ marginLeft: 6, fontSize: 10, fontWeight: 600, padding: '2px 6px', borderRadius: 4, background: 'var(--border)', color: 'var(--text-2)' }}>
              excluded
            </span>
          )}
        </div>
        <div style={{ textAlign: 'right', flexShrink: 0 }}>
          <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--text)' }}>{IDR(item.installment_amount)}</div>
          <div style={{ fontSize: 12, color: 'var(--text-2)' }}>/month</div>
        </div>
      </div>

      {/* Progress */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 5 }}>
          <span style={{ fontSize: 12, color: 'var(--text-2)' }}>Step {item.current_step} of {item.total_installments}</span>
          <span style={{ fontSize: 12, color: 'var(--text-2)' }}>{remaining} remaining</span>
        </div>
        <div style={{ height: 6, borderRadius: 3, background: 'var(--border)', overflow: 'hidden' }}>
          <div style={{ height: '100%', borderRadius: 3, width: `${progress * 100}%`, background: isExcluded ? 'var(--text-2)' : 'var(--blue)', transition: 'width 0.4s ease' }} />
        </div>
      </div>

      {/* Dates */}
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--text-2)' }}>
        <span>Next: <strong style={{ color: 'var(--text)' }}>{fmtDate(nextDue)}</strong></span>
        <span>Done by: <strong style={{ color: 'var(--text)' }}>{fmtDate(finish)}</strong></span>
      </div>

      {/* Divider */}
      <div style={{ borderTop: '1px solid var(--border)' }} />

      {/* Controls: +1 step + exclude toggle */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        {/* ac: installment-step-controls */}
        <button
          onClick={() => onIncrementStep(item)}
          style={{
            fontSize: 12,
            fontWeight: 600,
            padding: '5px 12px',
            borderRadius: 6,
            border: '1px solid var(--border-hover)',
            background: 'none',
            color: 'var(--text)',
            cursor: 'pointer',
          }}
        >
          +1 Step
        </button>

        {/* ac: installment-step-controls */}
        <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: 12, color: 'var(--text-2)' }}>
          <span>{isExcluded ? 'Excluded' : 'Include'}</span>
          <div
            onClick={() => onToggleExclude(item)}
            style={{
              width: 36,
              height: 20,
              borderRadius: 10,
              background: isExcluded ? 'var(--border-hover)' : 'var(--blue)',
              position: 'relative',
              cursor: 'pointer',
              transition: 'background 0.2s',
            }}
          >
            <div style={{
              position: 'absolute',
              top: 2,
              left: isExcluded ? 2 : 18,
              width: 16,
              height: 16,
              borderRadius: '50%',
              background: '#fff',
              transition: 'left 0.2s',
            }} />
          </div>
        </label>
      </div>
    </div>
  );
}

function CompletedCard({ item }) {
  return (
    <div style={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, padding: '14px 18px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', opacity: 0.65 }}>
      <div>
        <div style={{ fontWeight: 600, fontSize: 14, color: 'var(--text)' }}>{item.merchant}</div>
        <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 2 }}>{walletLabel(item.wallet)} · {item.total_installments} payments</div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--green)', fontSize: 13, fontWeight: 600 }}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
        Paid off
      </div>
    </div>
  );
}

export default function Installments() {
  const [installments, setInstallments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    getInstallments()
      .then(data => { setInstallments(data); setLoading(false); })
      .catch(e => { setError(e.message); setLoading(false); });
  }, []);

  // ac: installment-name-edit
  async function handleRename(id, newMerchant) {
    await updateInstallmentMerchant(id, newMerchant);
    setInstallments(prev => prev.map(i => i.id === id ? { ...i, merchant: newMerchant } : i));
  }

  // ac: installment-step-controls
  async function handleIncrementStep(item) {
    const newStep = item.current_step + 1;
    const newStatus = newStep >= item.total_installments ? 'completed' : item.status;
    setInstallments(prev => prev.map(i => i.id === item.id ? { ...i, current_step: newStep, status: newStatus } : i));
    await incrementInstallmentStep(item.id, newStep, newStatus).catch(() => {
      setInstallments(prev => prev.map(i => i.id === item.id ? item : i));
    });
  }

  // ac: installment-step-controls
  async function handleToggleExclude(item) {
    const excluded = !item.excluded;
    setInstallments(prev => prev.map(i => i.id === item.id ? { ...i, excluded } : i));
    await setInstallmentExcluded(item.id, excluded).catch(() => {
      setInstallments(prev => prev.map(i => i.id === item.id ? item : i));
    });
  }

  if (loading) return <div className="table-state"><div className="table-state-title">Loading installments...</div></div>;
  if (error)   return <div className="table-state"><div className="table-state-desc error">{error}</div></div>;

  // ac: installment-overview — sorted by next upcoming payment date
  const active = installments
    .filter(i => i.status === 'active')
    .sort((a, b) => nextDueDate(a.due_day) - nextDueDate(b.due_day));

  const completed = installments.filter(i => i.status === 'completed');

  // ac: installment-step-controls — totals exclude excluded installments
  const nonExcluded = active.filter(i => !i.excluded);
  const totalMonthly = nonExcluded.reduce((sum, i) => sum + Number(i.installment_amount), 0);
  const totalRemaining = nonExcluded.reduce((sum, i) => sum + (i.total_installments - i.current_step) * Number(i.installment_amount), 0);

  return (
    <div style={{ maxWidth: 680, margin: '0 auto', padding: '24px 16px' }}>
      {/* ac: installment-overview — accessible from top-level navigation */}
      {/* Summary card */}
      <div style={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 18px', marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div>
            <div style={{ fontSize: 13, color: 'var(--text-2)' }}>Total monthly</div>
            <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--text)', marginTop: 2 }}>{IDR(totalMonthly)}</div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 13, color: 'var(--text-2)' }}>Active plans</div>
            <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--text)', marginTop: 2 }}>{active.length}</div>
          </div>
        </div>
        {/* ac: installment-step-controls */}
        <div style={{ borderTop: '1px solid var(--border)', paddingTop: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: 13, color: 'var(--text-2)' }}>Total remaining commitment</span>
          <span style={{ fontSize: 15, fontWeight: 700, color: 'var(--text)' }}>{IDR(totalRemaining)}</span>
        </div>
      </div>

      <div className="section-header" style={{ marginBottom: 12 }}>
        <span className="section-title">Active</span>
        <span className="section-count">{active.length}</span>
      </div>
      {active.length === 0
        ? <div className="table-state"><div className="table-state-desc">No active installments</div></div>
        : <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 28 }}>
            {active.map(i => (
              <InstallmentCard
                key={i.id}
                item={i}
                onRename={handleRename}
                onIncrementStep={handleIncrementStep}
                onToggleExclude={handleToggleExclude}
              />
            ))}
          </div>
      }

      {completed.length > 0 && <>
        <div className="section-header" style={{ marginBottom: 12 }}>
          <span className="section-title">Completed</span>
          <span className="section-count">{completed.length}</span>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {completed.map(i => <CompletedCard key={i.id} item={i} />)}
        </div>
      </>}
    </div>
  );
}
