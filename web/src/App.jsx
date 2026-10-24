import { useState, useEffect } from 'react';
import NavBar from './components/NavBar';
import BookSwitch from './components/BookSwitch';
import TransactionList from './components/TransactionList';
import AddTransactionForm from './components/AddTransactionForm';
import WalletBalances from './components/WalletBalances';
import Insights from './components/Insights';
import Installments from './components/Installments';
import CardDebt from './components/CardDebt';
import Budget from './components/Budget';
import Calendar from './components/Calendar';
import ManageAccounts from './components/ManageAccounts';
import ManageCategories from './components/ManageCategories';
import { useCatalog } from './hooks/useCatalog';
import { WALLETS, areAccountsLoaded } from './constants/transaction';

// ac: retire-pdf-import-tab — no 'import' view among these; no browser PDF parsing code left in web/src
export default function App() {
  const [activeView, setActiveView] = useState('list');
  // ac: web-manage-accounts — loads the account list once on startup and re-renders every view when it changes
  useCatalog();
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('theme') === 'dark');

  useEffect(() => {
    document.documentElement.dataset.theme = darkMode ? 'dark' : 'light';
    localStorage.setItem('theme', darkMode ? 'dark' : 'light');
  }, [darkMode]);

  // ac: web-first-run-setup - a new installation has no accounts. The banner waits for the accounts
  // read, so it does not flash while the catalog loads.
  const noAccounts = areAccountsLoaded() && WALLETS.length === 0;

  return (
    <>
      <NavBar activeView={activeView} onNavigate={setActiveView} darkMode={darkMode} onToggleTheme={() => setDarkMode(d => !d)} />
      {/* ac: web-currency-books — one book switch above every view, so a single choice changes them all */}
      <BookSwitch />
      {noAccounts && activeView !== 'accounts' && (
        <div className="setup-banner" role="status">
          <span>No accounts yet. Add your accounts to start, or finish setup in the Android app.</span>
          <button className="btn-small btn-small-primary" onClick={() => setActiveView('accounts')}>Manage accounts</button>
        </div>
      )}
      {activeView === 'list'     && <TransactionList />}
      {activeView === 'balances' && <WalletBalances />}
      {activeView === 'add'      && <AddTransactionForm />}
      {activeView === 'insights' && <Insights onOpenTransactions={() => setActiveView('list')} />}
      {activeView === 'installments'  && <Installments />}
      {activeView === 'card-debt'     && <CardDebt />}
      {activeView === 'budget'        && <Budget />}
      {activeView === 'calendar'      && <Calendar />}
      {activeView === 'accounts'      && <ManageAccounts />}
      {activeView === 'categories'    && <ManageCategories />}
    </>
  );
}
