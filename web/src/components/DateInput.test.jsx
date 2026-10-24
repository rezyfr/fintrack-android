import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { expect, it } from 'vitest';
import DateInput from './DateInput';

function Harness({ initial = '2026-10-06' }) {
  const [value, setValue] = useState(initial);
  return (
    <>
      <label htmlFor="d">Date</label>
      <DateInput id="d" name="date" value={value} onChange={(e) => setValue(e.target.value)} />
      <output data-testid="iso">{value}</output>
    </>
  );
}

it('shows the date as dd MMM yyyy', () => {
  render(<Harness />);
  expect(screen.getByLabelText('Date')).toHaveValue('06 Oct 2026');
});

it('accepts a typed date and stores it as ISO', async () => {
  render(<Harness />);
  const input = screen.getByLabelText('Date');
  await userEvent.clear(input);
  await userEvent.type(input, '15 Nov 2026');
  await userEvent.tab();
  expect(screen.getByTestId('iso')).toHaveTextContent('2026-11-15');
  expect(input).toHaveValue('15 Nov 2026');
});

it('goes back to the last valid date when the typed text is not a date', async () => {
  render(<Harness />);
  const input = screen.getByLabelText('Date');
  await userEvent.clear(input);
  await userEvent.type(input, 'not a date');
  await userEvent.tab();
  expect(screen.getByTestId('iso')).toHaveTextContent('2026-10-06');
  expect(input).toHaveValue('06 Oct 2026');
});
