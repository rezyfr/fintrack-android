import { describe, it, expect } from 'vitest';
import { deriveAccountId } from './accounts';

describe('deriveAccountId', () => {
  it('uppercases and joins words with an underscore', () => {
    expect(deriveAccountId('Kasikorn Savings')).toBe('KASIKORN_SAVINGS');
  });

  it('collapses runs of symbols into one underscore', () => {
    expect(deriveAccountId('Visa -- Gold (TH)')).toBe('VISA_GOLD_TH');
  });

  it('trims leading and trailing underscores', () => {
    expect(deriveAccountId('  *Cash*  ')).toBe('CASH');
  });

  it('returns an empty id when the name has no letters or digits', () => {
    expect(deriveAccountId('---')).toBe('');
  });
});
