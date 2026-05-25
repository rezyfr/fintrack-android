import { useState } from 'react';
import NavBar from './components/NavBar';
import TransactionList from './components/TransactionList';
import AddTransactionForm from './components/AddTransactionForm';

export default function App() {
  const [activeView, setActiveView] = useState('list');
  return (
    <>
      <NavBar activeView={activeView} onNavigate={setActiveView} />
      {activeView === 'list' ? <TransactionList /> : <AddTransactionForm />}
    </>
  );
}
