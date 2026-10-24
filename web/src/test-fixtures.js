// The app has no built-in accounts. Tests start with the accounts of the original installation, and a
// test that needs another list calls applyCatalog itself.
export const LEGACY_ACCOUNTS = [
  { id: 'BBL',        name: 'Bangkok Bank',        currency: 'THB', type: 'checking',   is_default: true },
  { id: 'BCA',        name: 'BCA Account',         currency: 'IDR', type: 'checking',   is_default: true },
  { id: 'BCA_CC',     name: 'BCA Credit Card',     currency: 'IDR', type: 'credit',     is_default: true },
  { id: 'MANDIRI',    name: 'Mandiri Account',     currency: 'IDR', type: 'checking',   is_default: true },
  { id: 'MANDIRI_CC', name: 'Mandiri Credit Card', currency: 'IDR', type: 'credit',     is_default: true },
  { id: 'INVESTMENT', name: 'Investments',         currency: 'IDR', type: 'investment', is_default: true },
];
