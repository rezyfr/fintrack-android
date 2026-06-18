import { useState, useEffect } from 'react';
import NavBar from './components/NavBar';
import TransactionList from './components/TransactionList';
import AddTransactionForm from './components/AddTransactionForm';
import WalletBalances from './components/WalletBalances';
import Insights from './components/Insights';
import ImportPdf from './components/ImportPdf';
import Installments from './components/Installments';

export default function App() {
  const [activeView, setActiveView] = useState('list');
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('theme') !== 'light');

  useEffect(() => {
    document.documentElement.dataset.theme = darkMode ? 'dark' : 'light';
    localStorage.setItem('theme', darkMode ? 'dark' : 'light');
  }, [darkMode]);

  return (
    <>
      <NavBar activeView={activeView} onNavigate={setActiveView} darkMode={darkMode} onToggleTheme={() => setDarkMode(d => !d)} />
      {activeView === 'list'     && <TransactionList />}
      {activeView === 'balances' && <WalletBalances />}
      {activeView === 'add'      && <AddTransactionForm />}
      {activeView === 'insights' && <Insights />}
      {activeView === 'import'        && <ImportPdf />}
      {activeView === 'installments'  && <Installments />}
    </>
  );
}
