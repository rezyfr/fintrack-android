import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import ManageCategories from './ManageCategories';
import * as api from '../api/client';

vi.mock('../api/client');

const CATEGORIES = [
  { name: 'Bills', sort_order: 0, is_default: true },
  { name: 'Pets', sort_order: 1, is_default: false },
];
const SUBCATEGORIES = [
  { category: 'Bills', name: 'Rent', sort_order: 0 },
  { category: 'Pets', name: 'Vet', sort_order: 0 },
];

beforeEach(() => {
  api.getCategories.mockResolvedValue(CATEGORIES);
  api.getSubcategories.mockResolvedValue(SUBCATEGORIES);
  api.getAccounts.mockResolvedValue([]);
  api.saveCategory.mockResolvedValue();
  api.deleteCategory.mockResolvedValue();
  api.saveSubcategory.mockResolvedValue();
  api.deleteSubcategory.mockResolvedValue();
});
afterEach(() => vi.resetAllMocks());

// ac: web-manage-categories — Manage Categories lists every category with its subcategories
it('lists every category with its subcategories', async () => {
  render(<ManageCategories />);
  expect(await screen.findByText('Pets')).toBeInTheDocument();
  expect(screen.getByText('Bills')).toBeInTheDocument();
  expect(screen.getByText('Rent')).toBeInTheDocument();
  expect(screen.getByText('Vet')).toBeInTheDocument();
});

// ac: web-manage-categories — a default category cannot be deleted, so it has no Delete button
it('offers category Delete only for custom categories', async () => {
  render(<ManageCategories />);
  await screen.findByText('Pets');
  expect(screen.getAllByText('Default')).toHaveLength(1);
  // One category Delete button (Pets) and two subcategory Delete buttons (Rent, Vet).
  expect(screen.getAllByRole('button', { name: 'Delete' })).toHaveLength(1);
});

// ac: web-manage-categories — the user can add a custom category
it('adds a custom category', async () => {
  render(<ManageCategories />);
  await screen.findByText('Pets');
  await userEvent.type(screen.getByLabelText('New category name'), 'Education');
  await userEvent.click(screen.getByRole('button', { name: 'Add category' }));
  await waitFor(() => expect(api.saveCategory).toHaveBeenCalledWith({
    name: 'Education', sort_order: 2, is_default: false,
  }));
});

// ac: web-manage-categories — a custom category can be deleted
it('deletes a custom category', async () => {
  render(<ManageCategories />);
  await screen.findByText('Pets');
  await userEvent.click(screen.getByRole('button', { name: 'Delete' }));
  await waitFor(() => expect(api.deleteCategory).toHaveBeenCalledWith('Pets'));
});

// ac: web-manage-categories — the user can add a subcategory under any category
it('adds a subcategory under a category', async () => {
  render(<ManageCategories />);
  await screen.findByText('Pets');
  await userEvent.type(screen.getByLabelText('New subcategory for Pets'), 'Grooming');
  await userEvent.click(screen.getAllByRole('button', { name: 'Add' })[1]);
  await waitFor(() => expect(api.saveSubcategory).toHaveBeenCalledWith({
    category: 'Pets', name: 'Grooming', sort_order: 1,
  }));
});

// ac: web-manage-categories — the user can delete a subcategory under any category
it('deletes a subcategory', async () => {
  render(<ManageCategories />);
  await screen.findByText('Pets');
  await userEvent.click(screen.getByRole('button', { name: 'Delete Rent from Bills' }));
  await waitFor(() => expect(api.deleteSubcategory).toHaveBeenCalledWith('Bills', 'Rent'));
});
