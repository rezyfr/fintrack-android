import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import NavBar from './NavBar';

it('renders Transactions and Add buttons', () => {
  render(<NavBar activeView="list" onNavigate={vi.fn()} />);
  expect(screen.getByRole('button', { name: /transactions/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /add/i })).toBeInTheDocument();
});

it('marks the active view with aria-current', () => {
  render(<NavBar activeView="list" onNavigate={vi.fn()} />);
  expect(screen.getByRole('button', { name: /transactions/i })).toHaveAttribute('aria-current', 'page');
  expect(screen.getByRole('button', { name: /add/i })).not.toHaveAttribute('aria-current');
});

it('calls onNavigate with the correct view on click', async () => {
  const onNavigate = vi.fn();
  const user = userEvent.setup();
  render(<NavBar activeView="list" onNavigate={onNavigate} />);
  await user.click(screen.getByRole('button', { name: /add/i }));
  expect(onNavigate).toHaveBeenCalledWith('add');
});
