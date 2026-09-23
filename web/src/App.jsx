import { useState, useEffect } from 'react';
import NavBar from './components/NavBar';
import TransactionList from './components/TransactionList';
import AddTransactionForm from './components/AddTransactionForm';
import WalletBalances from './components/WalletBalances';
import Insights from './components/Insights';
import Installments from './components/Installments';
import CardDebt from './components/CardDebt';
import Budget from './components/Budget';
import Calendar from './components/Calendar';

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
      {activeView === 'installments'  && <Installments />}
      {activeView === 'card-debt'     && <CardDebt />}
      {activeView === 'budget'        && <Budget />}
      {activeView === 'calendar'      && <Calendar />}
    </>
  );
}
