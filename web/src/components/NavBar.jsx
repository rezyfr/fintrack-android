export default function NavBar({ activeView, onNavigate }) {
  return (
    <nav className="navbar">
      <span className="navbar-brand">FinTrack</span>
      <div className="navbar-nav">
        <button
          className="nav-btn"
          onClick={() => onNavigate('list')}
          aria-current={activeView === 'list' ? 'page' : undefined}
        >
          Transactions
        </button>
        <button
          className="nav-btn"
          onClick={() => onNavigate('balances')}
          aria-current={activeView === 'balances' ? 'page' : undefined}
        >
          Balances
        </button>
        <button
          className="nav-btn"
          onClick={() => onNavigate('add')}
          aria-current={activeView === 'add' ? 'page' : undefined}
        >
          Add
        </button>
      </div>
    </nav>
  );
}
