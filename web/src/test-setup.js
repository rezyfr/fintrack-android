import '@testing-library/jest-dom';
import { applyCatalog } from './constants/transaction';
import { LEGACY_ACCOUNTS } from './test-fixtures';

if (typeof DOMMatrix === 'undefined') {
  global.DOMMatrix = class DOMMatrix {
    constructor() { this.a=1;this.b=0;this.c=0;this.d=1;this.e=0;this.f=0; }
    static fromMatrix() { return new DOMMatrix(); }
  };
}

// Every test starts with the accounts of the original installation
beforeEach(() => applyCatalog({ accounts: LEGACY_ACCOUNTS }));
