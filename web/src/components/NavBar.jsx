function SunIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="5"/>
      <line x1="12" y1="1" x2="12" y2="3"/>
      <line x1="12" y1="21" x2="12" y2="23"/>
      <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
      <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
      <line x1="1" y1="12" x2="3" y2="12"/>
      <line x1="21" y1="12" x2="23" y2="12"/>
      <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
      <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
    </svg>
  );
}

function MoonIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
    </svg>
  );
}

export default function NavBar({ activeView, onNavigate, darkMode, onToggleTheme }) {
  return (
    <nav className="navbar">
      <span className="navbar-brand">FinTrack</span>
      <div className="navbar-right">
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
          <button
            className="nav-btn"
            onClick={() => onNavigate('insights')}
            aria-current={activeView === 'insights' ? 'page' : undefined}
          >
            Insights
          </button>
          <button
            className="nav-btn"
            onClick={() => onNavigate('installments')}
            aria-current={activeView === 'installments' ? 'page' : undefined}
          >
            Installments
          </button>
          <button
            className="nav-btn"
            onClick={() => onNavigate('card-debt')}
            aria-current={activeView === 'card-debt' ? 'page' : undefined}
          >
            Card Debt
          </button>
          <button
            className="nav-btn"
            onClick={() => onNavigate('budget')}
            aria-current={activeView === 'budget' ? 'page' : undefined}
          >
            Budget
          </button>
          <button
            className="nav-btn"
            onClick={() => onNavigate('calendar')}
            aria-current={activeView === 'calendar' ? 'page' : undefined}
          >
            Calendar
          </button>
        </div>
        <button
          className="theme-toggle"
          onClick={onToggleTheme}
          aria-label={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          {darkMode ? <SunIcon /> : <MoonIcon />}
        </button>
      </div>
    </nav>
  );
}
