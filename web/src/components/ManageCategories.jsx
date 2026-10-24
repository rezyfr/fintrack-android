import { useState, useEffect, useCallback } from 'react';
import { getCategories, getSubcategories, saveCategory, deleteCategory, saveSubcategory, deleteSubcategory } from '../api/client';
import { DEFAULT_CATEGORIES, DEFAULT_SUBCATEGORIES } from '../constants/transaction';
import { refreshCatalog } from '../hooks/useCatalog';

function defaultRows() {
  const categories = DEFAULT_CATEGORIES.map((name, i) => ({ name, sort_order: i, is_default: true }));
  const subcategories = Object.entries(DEFAULT_SUBCATEGORIES).flatMap(([category, names]) =>
    names.map((name, i) => ({ category, name, sort_order: i })));
  return { categories, subcategories };
}

// ac: web-manage-categories — Manage Categories lists every category with its subcategories, and
// adds categories, deletes custom categories, and adds and deletes subcategories
export default function ManageCategories() {
  const [categories, setCategories] = useState([]);
  const [subcategories, setSubcategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [newCategory, setNewCategory] = useState('');
  const [newSub, setNewSub] = useState({});
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const [c, s] = await Promise.all([getCategories(), getSubcategories()]);
      // An empty table means the migration has not been run yet. Show the defaults the app is using.
      const rows = c.length ? { categories: c, subcategories: s } : defaultRows();
      setCategories(rows.categories);
      setSubcategories(rows.subcategories);
      setError(null);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  async function run(action) {
    setBusy(true);
    try {
      await action();
      await refreshCatalog();
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  // ac: web-manage-categories — the user can add a custom category. A name that already exists is
  // rejected, because saving would overwrite that category.
  function handleAddCategory(e) {
    e.preventDefault();
    const name = newCategory.trim();
    if (!name) return setError('Name is required');
    if (categories.some(c => c.name.toLowerCase() === name.toLowerCase())) return setError(`${name} already exists`);
    const sortOrder = categories.reduce((max, c) => Math.max(max, c.sort_order ?? 0), -1) + 1;
    run(async () => {
      await saveCategory({ name, sort_order: sortOrder, is_default: false });
      setNewCategory('');
    });
  }

  // ac: web-manage-categories — a default category cannot be deleted, because the Dashboard and
  // Budget category breakdowns are keyed to the original categories
  function handleDeleteCategory(category) {
    if (category.is_default) return setError(`${category.name} is a default category and cannot be deleted`);
    run(() => deleteCategory(category.name));
  }

  // ac: web-manage-categories — the user can add a subcategory under any category
  function handleAddSubcategory(category, e) {
    e.preventDefault();
    const name = (newSub[category] ?? '').trim();
    if (!name) return setError('Name is required');
    const existing = subcategories.filter(s => s.category === category);
    if (existing.some(s => s.name.toLowerCase() === name.toLowerCase())) return setError(`${name} already exists under ${category}`);
    const sortOrder = existing.reduce((max, s) => Math.max(max, s.sort_order ?? 0), -1) + 1;
    run(async () => {
      await saveSubcategory({ category, name, sort_order: sortOrder });
      setNewSub(prev => ({ ...prev, [category]: '' }));
    });
  }

  // ac: web-manage-categories — the user can delete a subcategory under any category
  function handleDeleteSubcategory(category, name) {
    run(() => deleteSubcategory(category, name));
  }

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Categories</h1>
        <span className="section-count">{categories.length}</span>
      </div>

      {error && (
        <p className="table-state-desc error" role="alert">
          {error} <button className="btn-small" onClick={() => setError(null)}>Dismiss</button>
        </p>
      )}

      <form className="manage-add" onSubmit={handleAddCategory}>
        <input
          className="form-input"
          placeholder="New category name"
          aria-label="New category name"
          value={newCategory}
          onChange={e => setNewCategory(e.target.value)}
        />
        <button className="btn-small btn-small-primary" type="submit" disabled={busy}>Add category</button>
      </form>

      {loading ? (
        <p className="table-state-desc">Loading categories...</p>
      ) : (
        <div className="manage-grid">
          {categories.map(category => (
            <section key={category.name} className="form-card manage-card">
              <header className="manage-card-header">
                <h2 className="manage-card-title">{category.name}</h2>
                {category.is_default
                  ? <span className="chip chip-other">Default</span>
                  : <button className="btn-small" disabled={busy} onClick={() => handleDeleteCategory(category)}>Delete</button>}
              </header>
              <ul className="manage-subs">
                {subcategories.filter(s => s.category === category.name).map(s => (
                  <li key={s.name}>
                    <span>{s.name}</span>
                    <button
                      className="btn-small"
                      aria-label={`Delete ${s.name} from ${category.name}`}
                      disabled={busy}
                      onClick={() => handleDeleteSubcategory(category.name, s.name)}
                    >
                      Delete
                    </button>
                  </li>
                ))}
              </ul>
              <form className="manage-add" onSubmit={e => handleAddSubcategory(category.name, e)}>
                <input
                  className="form-input"
                  placeholder="New subcategory"
                  aria-label={`New subcategory for ${category.name}`}
                  value={newSub[category.name] ?? ''}
                  onChange={e => setNewSub(prev => ({ ...prev, [category.name]: e.target.value }))}
                />
                <button className="btn-small" type="submit" disabled={busy}>Add</button>
              </form>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
