import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import ManageAccounts from './ManageAccounts';
import * as api from '../api/client';

vi.mock('../api/client');

const ROWS = [
  { id: 'BBL', name: 'Bangkok Bank', currency: 'THB', sort_order: 0, is_default: true },
  { id: 'KBANK', name: 'Kasikorn', currency: 'THB', sort_order: 1, is_default: false },
];

beforeEach(() => {
  api.getAccounts.mockResolvedValue(ROWS);
  api.getCategories.mockResolvedValue([]);
  api.getSubcategories.mockResolvedValue([]);
  api.saveAccount.mockResolvedValue();
  api.deleteAccount.mockResolvedValue();
});
afterEach(() => vi.resetAllMocks());

// ac: web-manage-accounts — Manage Accounts lists every account with its currency
it('lists every account with its currency', async () => {
  render(<ManageAccounts />);
  expect(await screen.findByText('Kasikorn')).toBeInTheDocument();
  expect(screen.getByText('Bangkok Bank')).toBeInTheDocument();
  expect(screen.getAllByText('THB')).toHaveLength(2);
});

// ac: web-manage-accounts — a default account has no Rename or Delete button
it('offers Rename and Delete only for custom accounts', async () => {
  render(<ManageAccounts />);
  await screen.findByText('Kasikorn');
  expect(screen.getAllByRole('button', { name: 'Delete' })).toHaveLength(1);
  expect(screen.getAllByRole('button', { name: 'Rename' })).toHaveLength(1);
});

// ac: web-manage-accounts — adding an account saves it with the id derived from its name
it('adds a custom account with an id derived from the name', async () => {
  render(<ManageAccounts />);
  await screen.findByText('Kasikorn');
  await userEvent.type(screen.getByLabelText('Account name'), 'Visa Gold');
  await userEvent.type(screen.getByLabelText('Currency'), 'usd');
  await userEvent.click(screen.getByRole('button', { name: 'Add account' }));
  await waitFor(() => expect(api.saveAccount).toHaveBeenCalledWith({
    id: 'VISA_GOLD', name: 'Visa Gold', currency: 'USD', type: 'checking', sort_order: 2, is_default: false,
  }));
});

// ac: web-manage-accounts — adding a name whose id already exists is rejected, not overwritten
it('rejects a new account whose id already exists', async () => {
  render(<ManageAccounts />);
  await screen.findByText('Kasikorn');
  await userEvent.type(screen.getByLabelText('Account name'), 'bbl');
  await userEvent.type(screen.getByLabelText('Currency'), 'THB');
  await userEvent.click(screen.getByRole('button', { name: 'Add account' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('BBL already exists');
  expect(api.saveAccount).not.toHaveBeenCalled();
});

// ac: web-manage-accounts — renaming keeps the id so stored transactions still resolve
it('renames a custom account and keeps its id', async () => {
  render(<ManageAccounts />);
  await screen.findByText('Kasikorn');
  await userEvent.click(screen.getByRole('button', { name: 'Rename' }));
  const input = screen.getByLabelText('New name for Kasikorn');
  await userEvent.clear(input);
  await userEvent.type(input, 'KBank Savings');
  await userEvent.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() => expect(api.saveAccount).toHaveBeenCalledWith({
    id: 'KBANK', name: 'KBank Savings', currency: 'THB', type: 'checking', sort_order: 1, is_default: false,
  }));
});

// ac: web-manage-accounts — a custom account can be deleted
it('deletes a custom account', async () => {
  render(<ManageAccounts />);
  await screen.findByText('Kasikorn');
  await userEvent.click(screen.getByRole('button', { name: 'Delete' }));
  await waitFor(() => expect(api.deleteAccount).toHaveBeenCalledWith('KBANK'));
});

// ac: web-first-run-setup - the add form saves the chosen account type
it('adds an account with the selected type', async () => {
  render(<ManageAccounts />);
  await screen.findByText('Kasikorn');
  await userEvent.type(screen.getByLabelText('Account name'), 'Visa Gold');
  await userEvent.type(screen.getByLabelText('Currency'), 'usd');
  await userEvent.selectOptions(screen.getByLabelText('Account type'), 'credit');
  await userEvent.click(screen.getByRole('button', { name: 'Add account' }));
  await waitFor(() => expect(api.saveAccount).toHaveBeenCalledWith(expect.objectContaining({ id: 'VISA_GOLD', type: 'credit' })));
});

// ac: web-first-run-setup - the edit form changes the account type and keeps the other fields
it('changes the type of an account', async () => {
  render(<ManageAccounts />);
  await screen.findByText('Kasikorn');
  await userEvent.click(screen.getByRole('button', { name: 'Rename' }));
  await userEvent.selectOptions(screen.getByLabelText('Type for Kasikorn'), 'investment');
  await userEvent.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() => expect(api.saveAccount).toHaveBeenCalledWith({
    id: 'KBANK', name: 'Kasikorn', currency: 'THB', type: 'investment', sort_order: 1, is_default: false,
  }));
});

// ac: web-first-run-setup - a default account can change its type but not its name
it('changes the type of a default account', async () => {
  render(<ManageAccounts />);
  await screen.findByText('Kasikorn');
  await userEvent.click(screen.getByRole('button', { name: 'Change type' }));
  await userEvent.selectOptions(screen.getByLabelText('Type for Bangkok Bank'), 'credit');
  await userEvent.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() => expect(api.saveAccount).toHaveBeenCalledWith({
    id: 'BBL', name: 'Bangkok Bank', currency: 'THB', type: 'credit', sort_order: 0, is_default: true,
  }));
});

// ac: web-first-run-setup - an empty accounts table shows an empty list, not the legacy accounts
it('shows no accounts when the table is empty', async () => {
  api.getAccounts.mockResolvedValue([]);
  render(<ManageAccounts />);
  await waitFor(() => expect(screen.queryByText('Loading accounts...')).not.toBeInTheDocument());
  expect(screen.queryByText('Bangkok Bank')).not.toBeInTheDocument();
});
