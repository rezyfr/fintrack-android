import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect } from 'vitest';
import MerchantInput from './MerchantInput';

const DEFAULT_HISTORY = ['Grab', 'LINE MAN', 'Shopee'];

function renderInput(props = {}) {
  const defaults = {
    id: 'merchant',
    name: 'merchant',
    value: '',
    onChange: vi.fn(),
    history: DEFAULT_HISTORY,
    onSelect: vi.fn(),
    placeholder: 'e.g. LINE MAN',
    required: true,
  };
  return render(<MerchantInput {...defaults} {...props} />);
}

describe('MerchantInput', () => {
  it('renders input with correct props', () => {
    renderInput();
    const input = screen.getByRole('textbox');
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('id', 'merchant');
    expect(input).toHaveAttribute('name', 'merchant');
    expect(input).toHaveAttribute('placeholder', 'e.g. LINE MAN');
    expect(input).toBeRequired();
  });

  it('dropdown appears on focus with all history items', async () => {
    renderInput({ value: '' });
    const input = screen.getByRole('textbox');
    fireEvent.focus(input);
    expect(screen.getByText('Grab')).toBeInTheDocument();
    expect(screen.getByText('LINE MAN')).toBeInTheDocument();
    expect(screen.getByText('Shopee')).toBeInTheDocument();
  });

  it('dropdown filters as user types (case-insensitive)', () => {
    renderInput({ value: 'line' });
    const input = screen.getByRole('textbox');
    fireEvent.focus(input);
    expect(screen.getByText('LINE MAN')).toBeInTheDocument();
    expect(screen.queryByText('Grab')).not.toBeInTheDocument();
    expect(screen.queryByText('Shopee')).not.toBeInTheDocument();
  });

  it('clicking a suggestion calls onSelect with the merchant name', () => {
    const onSelect = vi.fn();
    renderInput({ onSelect, value: '' });
    const input = screen.getByRole('textbox');
    fireEvent.focus(input);
    fireEvent.mouseDown(screen.getByText('Grab'));
    expect(onSelect).toHaveBeenCalledWith('Grab');
  });

  it('dropdown is hidden when history is empty', () => {
    renderInput({ history: [], value: '' });
    const input = screen.getByRole('textbox');
    fireEvent.focus(input);
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
  });

  it('dropdown is hidden when no items match the typed text', () => {
    renderInput({ value: 'zzznomatch' });
    const input = screen.getByRole('textbox');
    fireEvent.focus(input);
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
  });
});
