import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import CurrencySettingsForm from './CurrencySettingsForm';
import { DEFAULT_CURRENCY_SETTINGS, getCurrencySettings, saveCurrencySettings } from '../constants/currency';
import { applyCatalog } from '../constants/transaction';

beforeEach(() => {
  vi.stubGlobal('localStorage', { getItem: () => null, setItem: () => {} });
  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: true })));
});
afterEach(() => {
  saveCurrencySettings(DEFAULT_CURRENCY_SETTINGS);
  vi.unstubAllGlobals();
});

// ac: web-configurable-currency-pair - the form starts with the default home currency and the other
// currencies of the accounts
it('shows the home currency and a label and rate for each other currency in use', () => {
  render(<CurrencySettingsForm />);
  expect(screen.getByLabelText('Home currency code')).toHaveValue('IDR');
  expect(screen.getByLabelText('Home currency label')).toHaveValue('Indonesia');
  expect(screen.getByLabelText('Pay cycle start day')).toHaveValue(26);
  expect(screen.getByLabelText('Label for THB')).toHaveValue('Thailand');
  expect(screen.getByLabelText('Rate for THB')).toHaveValue(null);
  expect(screen.queryByLabelText('Foreign currency code')).not.toBeInTheDocument();
});

it('has no field for another currency when the accounts hold only the home currency', () => {
  applyCatalog({ accounts: [{ id: 'MAIN', name: 'Main', currency: 'IDR' }] });
  render(<CurrencySettingsForm />);
  expect(screen.queryByLabelText('Label for THB')).not.toBeInTheDocument();
});

it('lists every other currency of the accounts', () => {
  applyCatalog({
    accounts: [
      { id: 'A', name: 'A', currency: 'IDR' }, { id: 'B', name: 'B', currency: 'THB' },
      { id: 'C', name: 'C', currency: 'USD' }, { id: 'D', name: 'D', currency: 'EUR' },
    ],
  });
  render(<CurrencySettingsForm />);
  expect(screen.getByLabelText('Label for USD')).toHaveValue('USD');
  expect(screen.getByLabelText('Label for EUR')).toHaveValue('EUR');
});

// ac: web-configurable-currency-pair - editing and saving changes the label and the rate
it('saves an edited label and rate', async () => {
  applyCatalog({
    accounts: [{ id: 'A', name: 'A', currency: 'IDR' }, { id: 'B', name: 'B', currency: 'THB' }, { id: 'C', name: 'C', currency: 'USD' }],
  });
  render(<CurrencySettingsForm />);
  const label = screen.getByLabelText('Label for USD');
  await userEvent.clear(label);
  await userEvent.type(label, 'United States');
  await userEvent.type(screen.getByLabelText('Rate for USD'), '16000');
  await userEvent.type(screen.getByLabelText('Rate for THB'), '480');
  await userEvent.click(screen.getByRole('button', { name: 'Save currencies' }));
  expect(await screen.findByRole('status')).toHaveTextContent('Saved');
  expect(getCurrencySettings().currencies).toEqual([
    { code: 'THB', label: 'Thailand', rate: 480 },
    { code: 'USD', label: 'United States', rate: 16000 },
  ]);
});

it('shows an error when the home code is not a valid code', async () => {
  render(<CurrencySettingsForm />);
  const code = screen.getByLabelText('Home currency code');
  await userEvent.clear(code);
  await userEvent.type(code, 'ID');
  await userEvent.click(screen.getByRole('button', { name: 'Save currencies' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('three letters');
  expect(getCurrencySettings().homeCode).toBe('IDR');
});

it('shows an error when a rate is not above 0', async () => {
  render(<CurrencySettingsForm />);
  await userEvent.type(screen.getByLabelText('Rate for THB'), '0');
  await userEvent.click(screen.getByRole('button', { name: 'Save currencies' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('exchange rate for THB');
  expect(getCurrencySettings().currencies[0].rate).toBeNull();
});
