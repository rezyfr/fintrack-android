export default function NavBar({ activeView, onNavigate }) {
  return (
    <nav>
      <button
        onClick={() => onNavigate('list')}
        aria-current={activeView === 'list' ? 'page' : undefined}
      >
        Transactions
      </button>
      <button
        onClick={() => onNavigate('add')}
        aria-current={activeView === 'add' ? 'page' : undefined}
      >
        Add
      </button>
    </nav>
  );
}
