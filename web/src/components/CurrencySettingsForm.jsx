import { useState } from 'react';
import { getCurrencySettings, saveCurrencySettings, currenciesInUse, labelFor } from '../constants/currency';

// ac: web-configurable-currency-pair - Currency settings: the home currency code and label, the first
// day of the pay cycle, and a label and an optional exchange rate for each other currency in use.
// The other currencies come from the accounts, so there is no field to add one here.
// ac: web-first-run-setup - the settings are saved in the shared app_settings table, with a copy in
// this browser.
export default function CurrencySettingsForm() {
  const [form, setForm] = useState(() => toForm(getCurrencySettings()));
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const set = (field) => (e) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }));
    setMessage(null);
  };

  const setOther = (code, field) => (e) => {
    setForm(prev => ({
      ...prev,
      others: prev.others.map(o => (o.code === code ? { ...o, [field]: e.target.value } : o)),
    }));
    setMessage(null);
  };

  function handleSubmit(e) {
    e.preventDefault();
    const { others, ...rest } = form;
    // An override of a currency that no account uses now is kept, so its label comes back with it
    const kept = getCurrencySettings().currencies.filter(c => !others.some(o => o.code === c.code));
    const failure = saveCurrencySettings({ ...rest, currencies: [...others, ...kept] });
    setError(failure);
    setMessage(failure ? null : 'Saved');
    if (!failure) setForm(toForm(getCurrencySettings()));
  }

  return (
    <form className="currency-settings" onSubmit={handleSubmit}>
      <h2 className="manage-card-title">Currency settings</h2>
      <p className="currency-settings-note">
        The home currency is the one all combined totals use. The other currencies come from your accounts.
        Leave a rate empty to use the rate of your latest transfers, or the default rate.
        The pay cycle start day is a day from 1 to 28. The settings are shared with the Android app.
      </p>
      <div className="currency-settings-grid">
        <label className="form-group">
          <span className="form-label">Home currency code</span>
          <input className="form-input" value={form.homeCode} onChange={set('homeCode')} maxLength={3} />
        </label>
        <label className="form-group">
          <span className="form-label">Home currency label</span>
          <input className="form-input" value={form.homeLabel} onChange={set('homeLabel')} />
        </label>
        <label className="form-group">
          <span className="form-label">Pay cycle start day</span>
          <input className="form-input" type="number" min="1" max="28" value={form.cycleStartDay} onChange={set('cycleStartDay')} />
        </label>
        {form.others.map(o => (
          <div key={o.code} className="currency-settings-other">
            <label className="form-group">
              <span className="form-label">Label for {o.code}</span>
              <input className="form-input" value={o.label} onChange={setOther(o.code, 'label')} />
            </label>
            <label className="form-group">
              <span className="form-label">Rate for {o.code}</span>
              <input
                className="form-input" type="number" min="0" step="any" value={o.rate}
                placeholder={`${form.homeCode.trim().toUpperCase()} per 1 ${o.code}`}
                onChange={setOther(o.code, 'rate')}
              />
            </label>
          </div>
        ))}
      </div>
      {error && <p className="table-state-desc error" role="alert">{error}</p>}
      {message && <p className="currency-settings-note" role="status">{message}</p>}
      <button className="btn-small btn-small-primary" type="submit">Save currencies</button>
    </form>
  );
}

// Form fields hold text. The other currencies are the currencies in use except the home currency, each
// with its label (the code when it has none) and its rate (empty when it has none).
function toForm(settings) {
  return {
    homeCode: settings.homeCode,
    homeLabel: settings.homeLabel,
    cycleStartDay: String(settings.cycleStartDay),
    others: currenciesInUse()
      .filter(code => code !== settings.homeCode)
      .map(code => {
        const override = settings.currencies.find(c => c.code === code);
        return { code, label: labelFor(code), rate: override?.rate == null ? '' : String(override.rate) };
      }),
  };
}
