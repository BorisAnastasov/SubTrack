import { useEffect, useState } from "react";
import { categoryRequest, currencyRequest, subscriptionRequest } from "../api.js";
import { Brand } from "../components/Brand.jsx";
import { CategoryForm, SubscriptionForm } from "./SubscriptionForms.jsx";

function monthlyEquivalent(subscription) {
  const price = Number(subscription.price);
  if (subscription.billingCycle === "WEEKLY") return price * 52 / 12;
  if (subscription.billingCycle === "YEARLY") return price / 12;
  return price;
}

function formatCurrency(amount, currency) {
  try {
    return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(amount);
  } catch {
    return `${amount.toFixed(2)} ${currency}`;
  }
}

export function AccountPage({ auth, onLogout }) {
  const [subscriptions, setSubscriptions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [summary, setSummary] = useState(null);
  const [upcoming, setUpcoming] = useState([]);
  const [currencyCodes, setCurrencyCodes] = useState(["EUR", "USD"]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [featureError, setFeatureError] = useState("");
  const [formSubscription, setFormSubscription] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [showCategoryForm, setShowCategoryForm] = useState(false);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [cycleFilter, setCycleFilter] = useState("ALL");
  const [upcomingDays, setUpcomingDays] = useState(30);
  const [sortBy, setSortBy] = useState("date");
  const [summaryCurrency, setSummaryCurrency] = useState("");
  const [converter, setConverter] = useState({ amount: "1", from: "EUR", to: "USD" });
  const [conversion, setConversion] = useState(null);
  const [converting, setConverting] = useState(false);

  async function loadDashboard() {
    setError("");
    try {
      const [items, options] = await Promise.all([
        subscriptionRequest("", auth.accessToken),
        subscriptionRequest("/options", auth.accessToken),
      ]);
      setSubscriptions(items);
      setCategories(options.categories || []);
    } catch (requestError) {
      setError(requestError.message || "Could not load subscriptions.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDashboard();
  }, [auth.accessToken]);

  useEffect(() => {
    let active = true;
    const query = summaryCurrency ? `?currency=${encodeURIComponent(summaryCurrency)}` : "";
    subscriptionRequest(`/summary${query}`, auth.accessToken)
      .then((result) => { if (active) setSummary(result); })
      .catch((requestError) => { if (active) setFeatureError(requestError.message || "Could not load spending summary."); });
    return () => { active = false; };
  }, [auth.accessToken, summaryCurrency, subscriptions]);

  useEffect(() => {
    let active = true;
    subscriptionRequest(`/upcoming?days=${upcomingDays}`, auth.accessToken)
      .then((items) => { if (active) setUpcoming(items); })
      .catch((requestError) => { if (active) setFeatureError(requestError.message || "Could not load upcoming renewals."); });
    return () => { active = false; };
  }, [auth.accessToken, upcomingDays, subscriptions]);

  useEffect(() => {
    let active = true;
    currencyRequest("/rates?base=EUR", auth.accessToken)
      .then((result) => {
        if (!active) return;
        const codes = [...new Set([result.baseCurrency, ...Object.keys(result.rates || {})])].sort();
        setCurrencyCodes(codes);
        setConverter((current) => ({
          ...current,
          from: codes.includes(current.from) ? current.from : result.baseCurrency,
          to: codes.includes(current.to) ? current.to : (codes.includes("USD") ? "USD" : codes[0]),
        }));
      })
      .catch((requestError) => { if (active) setFeatureError(requestError.message || "Could not load currency rates."); });
    return () => { active = false; };
  }, [auth.accessToken]);

  async function saveSubscription(payload) {
    setBusy(true);
    try {
      const saved = await subscriptionRequest(
        formSubscription ? `/${formSubscription.id}` : "",
        auth.accessToken,
        { method: formSubscription ? "PUT" : "POST", body: JSON.stringify(payload) },
      );
      setSubscriptions((current) => formSubscription
        ? current.map((item) => item.id === saved.id ? saved : item)
        : [saved, ...current]);
      setShowForm(false);
      setFormSubscription(null);
    } finally {
      setBusy(false);
    }
  }

  async function changeStatus(subscription, status) {
    setError("");
    try {
      const updated = await subscriptionRequest(`/${subscription.id}/status`, auth.accessToken, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      });
      setSubscriptions((current) => current.map((item) => item.id === updated.id ? updated : item));
    } catch (requestError) {
      setError(requestError.message || "Could not update subscription status.");
    }
  }

  async function createCategory(payload) {
    setBusy(true);
    try {
      const created = await categoryRequest("", auth.accessToken, {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setCategories((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name)));
      setShowCategoryForm(false);
    } finally {
      setBusy(false);
    }
  }

  async function convertCurrency(event) {
    event.preventDefault();
    setFeatureError("");
    setConversion(null);
    setConverting(true);
    try {
      const params = new URLSearchParams({
        amount: converter.amount,
        from: converter.from,
        to: converter.to,
      });
      const result = await currencyRequest(`/convert?${params}`, auth.accessToken);
      setConversion(result);
    } catch (requestError) {
      setFeatureError(requestError.message || "Could not convert this amount.");
    } finally {
      setConverting(false);
    }
  }

  function openEdit(subscription) {
    setFormSubscription(subscription);
    setShowForm(true);
  }

  function exportCsv() {
    setFeatureError("");
    const csvValue = (value) => `"${String(value ?? "").replaceAll('"', '""')}"`;
    const rows = [
      ["name", "price", "currency", "billingCycle", "nextBillingDate", "paymentMethod", "category", "status", "notes"],
      ...filteredSubscriptions.map((item) => [
        item.name,
        item.price,
        item.currency,
        item.billingCycle,
        item.nextBillingDate,
        item.paymentMethod,
        item.categoryName,
        item.status,
        item.notes,
      ]),
    ];
    const csv = new Blob([rows.map((row) => row.map(csvValue).join(",")).join("\n") + "\n"], {
      type: "text/csv;charset=utf-8",
    });
    const url = URL.createObjectURL(csv);
    const link = document.createElement("a");
    link.href = url;
    link.download = "subtrack-subscriptions.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  const filteredSubscriptions = subscriptions.filter((item) => {
    const matchesSearch = `${item.name} ${item.categoryName || ""} ${item.paymentMethod || ""}`
      .toLowerCase().includes(search.toLowerCase());
    return matchesSearch &&
      (statusFilter === "ALL" || item.status === statusFilter) &&
      (categoryFilter === "ALL" || String(item.categoryId || "") === categoryFilter) &&
      (cycleFilter === "ALL" || item.billingCycle === cycleFilter);
  }).sort((a, b) => {
    if (sortBy === "name") return a.name.localeCompare(b.name);
    if (sortBy === "status") return a.status.localeCompare(b.status);
    if (sortBy === "category") return (a.categoryName || "").localeCompare(b.categoryName || "");
    if (sortBy === "cycle") return a.billingCycle.localeCompare(b.billingCycle);
    return a.nextBillingDate.localeCompare(b.nextBillingDate);
  });

  return (
    <main className="account-page dashboard-page">
      <header className="account-header dashboard-header">
        <Brand compact />
        <div className="dashboard-account">
          <span className="account-email">{auth.email}</span>
          <button className="text-button" type="button" onClick={onLogout}>Sign out</button>
        </div>
      </header>
      <section className="dashboard-content">
        <div className="dashboard-title">
          <div>
            <span className="form-kicker">YOUR RECURRING PAYMENTS</span>
            <h1>A clearer view of your subscriptions.</h1>
            <p>Keep track of what renews, when it renews, and what it costs.</p>
          </div>
          <button className="primary-button dashboard-add-button" type="button" onClick={() => {
            setFormSubscription(null);
            setShowForm(true);
          }}><span><span aria-hidden="true">+</span> Add subscription</span></button>
        </div>
        {error && <div className="form-error dashboard-error" role="alert">{error}</div>}
        {featureError && <div className="form-error dashboard-error" role="alert">{featureError}</div>}

        <section className="dashboard-metrics" aria-label="Subscription totals">
          <article className="metric-card">
            <span>MONTHLY COST</span>
            {summary?.monthlyCost?.length ? summary.monthlyCost.map((total) =>
              <strong key={total.currency}>{formatCurrency(Number(total.amount), total.currency)}</strong>) : <strong>{formatCurrency(0, summaryCurrency || "USD")}</strong>}
            <small>Active subscriptions · normalized</small>
          </article>
          <article className="metric-card">
            <span>ANNUAL COST</span>
            {summary?.annualCost?.length ? summary.annualCost.map((total) =>
              <strong key={total.currency}>{formatCurrency(Number(total.amount), total.currency)}</strong>) : <strong>{formatCurrency(0, summaryCurrency || "USD")}</strong>}
            <small>Estimated yearly spend</small>
          </article>
          <article className="metric-card category-metric">
            <span>MONTHLY BY CATEGORY</span>
            {summary?.spendingByCategory?.length ? summary.spendingByCategory.map((category) => (
              <div key={`${category.categoryId || "none"}-${category.currency}`}>
                <b>{category.categoryName}</b>
                <strong>{formatCurrency(Number(category.monthlyCost), category.currency)}</strong>
              </div>
            )) : <small>No active subscription spend yet</small>}
          </article>
        </section>

        <div className="dashboard-tools">
          <section className="tool-card">
            <div className="tool-card-heading">
              <div><h2>Spending currency</h2><p>Convert your totals to one currency</p></div>
            </div>
            <label className="field compact-field">
              <span>Display currency</span>
              <select value={summaryCurrency} onChange={(event) => setSummaryCurrency(event.target.value)}>
                <option value="">Show original currencies</option>
                {currencyCodes.map((currency) => <option key={currency} value={currency}>{currency}</option>)}
              </select>
            </label>
          </section>
          <section className="tool-card">
            <div className="tool-card-heading">
              <div><h2>Currency converter</h2><p>Exchange rates provided by Frankfurter</p></div>
            </div>
            <form className="converter-form" onSubmit={convertCurrency}>
              <label className="field compact-field">
                <span>Amount</span>
                <input type="number" min="0" step="0.01" value={converter.amount} onChange={(event) => setConverter((current) => ({ ...current, amount: event.target.value }))} required />
              </label>
              <label className="field compact-field">
                <span>From</span>
                <select value={converter.from} onChange={(event) => setConverter((current) => ({ ...current, from: event.target.value }))}>
                  {currencyCodes.map((currency) => <option key={currency} value={currency}>{currency}</option>)}
                </select>
              </label>
              <label className="field compact-field">
                <span>To</span>
                <select value={converter.to} onChange={(event) => setConverter((current) => ({ ...current, to: event.target.value }))}>
                  {currencyCodes.map((currency) => <option key={currency} value={currency}>{currency}</option>)}
                </select>
              </label>
              <button className="secondary-button convert-button" type="submit" disabled={converting}>
                {converting ? "Converting…" : "Convert"}
              </button>
            </form>
            {conversion && <p className="conversion-result">
              <strong>{formatCurrency(Number(conversion.convertedAmount), conversion.toCurrency)}</strong>
              <span>{formatCurrency(Number(conversion.amount), conversion.fromCurrency)} · rate date {conversion.rateDate || "same currency"}</span>
            </p>}
          </section>
          <section className="tool-card category-tool-card">
            <div className="tool-card-heading">
              <div><h2>Categories</h2><p>Organize subscriptions your way</p></div>
              <button className="text-button" type="button" onClick={() => setShowCategoryForm(true)}><span aria-hidden="true">+</span> Add</button>
            </div>
            <div className="category-chip-list">
              {categories.map((category) => (
                <span className="category-chip" key={category.id}>
                  <i style={category.color ? { backgroundColor: category.color } : undefined} />
                  {category.name}
                </span>
              ))}
            </div>
          </section>
        </div>

        <section className="upcoming-section">
          <div className="section-heading">
            <div><h2>Coming up</h2><p>Upcoming renewals for active subscriptions</p></div>
            <label className="compact-filter">
              <span className="visually-hidden">Upcoming payment range</span>
              <select value={upcomingDays} onChange={(event) => setUpcomingDays(Number(event.target.value))}>
                <option value="7">Next 7 days</option>
                <option value="30">Next 30 days</option>
                <option value="90">Next 90 days</option>
              </select>
            </label>
          </div>
          {loading ? <p className="empty-note">Loading renewals…</p> : upcoming.length ? <div className="upcoming-list">{upcoming.map((item) => (
            <div className="upcoming-item" key={item.id}>
              <span className="subscription-avatar">{item.name.slice(0, 1).toUpperCase()}</span>
              <span className="upcoming-name"><b>{item.name}</b><small>{item.nextBillingDate}</small></span>
              <strong>{formatCurrency(Number(item.price), item.currency)}</strong>
            </div>
          ))}</div> : <p className="empty-note">Nothing due in the next {upcomingDays} days.</p>}
        </section>

        <section className="subscriptions-section">
          <div className="section-heading">
            <div><h2>All subscriptions</h2><p>{subscriptions.length} saved recurring {subscriptions.length === 1 ? "payment" : "payments"}</p></div>
            <button className="secondary-button export-button" type="button" onClick={exportCsv} disabled={!filteredSubscriptions.length}>Export CSV</button>
          </div>
          <div className="subscription-filters">
            <input aria-label="Search subscriptions" placeholder="Search subscriptions…" value={search} onChange={(event) => setSearch(event.target.value)} />
            <select aria-label="Filter by status" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="PAUSED">Paused</option><option value="CANCELLED">Cancelled</option>
            </select>
            <select aria-label="Filter by category" value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}>
              <option value="ALL">All categories</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
            <select aria-label="Filter by billing cycle" value={cycleFilter} onChange={(event) => setCycleFilter(event.target.value)}>
              <option value="ALL">All cycles</option><option value="WEEKLY">Weekly</option><option value="MONTHLY">Monthly</option><option value="YEARLY">Yearly</option>
            </select>
            <select aria-label="Sort subscriptions" value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
              <option value="date">Sort: next date</option><option value="name">Sort: name</option>
              <option value="status">Sort: status</option><option value="category">Sort: category</option>
              <option value="cycle">Sort: billing cycle</option>
            </select>
          </div>
          {loading ? <p className="empty-note">Loading subscriptions…</p> : filteredSubscriptions.length ? (
            <div className="subscription-list">{filteredSubscriptions.map((item) => (
              <article className={`subscription-card status-${item.status.toLowerCase()}`} key={item.id}>
                <span className="subscription-avatar" style={item.categoryColor ? { backgroundColor: `${item.categoryColor}20`, color: item.categoryColor } : undefined}>
                  {item.name.slice(0, 1).toUpperCase()}
                </span>
                <div className="subscription-info">
                  <div className="subscription-name-line"><h3>{item.name}</h3><span className={`status-pill status-pill-${item.status.toLowerCase()}`}>{item.status.toLowerCase()}</span></div>
                  <p>{item.categoryName || "Uncategorized"} · {item.billingCycle.toLowerCase()} · Next {item.nextBillingDate}</p>
                  {item.paymentMethod && <small>{item.paymentMethod}</small>}
                  {item.notes && <small className="subscription-notes">{item.notes}</small>}
                </div>
                <div className="subscription-cost">
                  <strong>{formatCurrency(Number(item.price), item.currency)}</strong>
                  <small>per {item.billingCycle.toLowerCase()}</small>
                  <small>{formatCurrency(monthlyEquivalent(item), item.currency)} / mo</small>
                </div>
                <div className="subscription-actions">
                  <button className="text-button" type="button" onClick={() => openEdit(item)}>Edit</button>
                  {item.status === "ACTIVE" && <button className="text-button" type="button" onClick={() => changeStatus(item, "PAUSED")}>Pause</button>}
                  {item.status === "PAUSED" && <button className="text-button" type="button" onClick={() => changeStatus(item, "ACTIVE")}>Resume</button>}
                  {item.status !== "CANCELLED" && <button className="text-button danger-text" type="button" onClick={() => changeStatus(item, "CANCELLED")}>Cancel</button>}
                  {item.status === "CANCELLED" && <button className="text-button" type="button" onClick={() => changeStatus(item, "ACTIVE")}>Restore</button>}
                  {!!item.statusHistory?.length && <details className="history-details">
                    <summary>History</summary>
                    <ul>{item.statusHistory.map((entry, index) => (
                      <li key={`${entry.changedAt}-${index}`}>{entry.fromStatus ? `${entry.fromStatus.toLowerCase()} → ` : ""}{entry.toStatus.toLowerCase()} · {new Date(entry.changedAt).toLocaleDateString()}</li>
                    ))}</ul>
                  </details>}
                </div>
              </article>
            ))}</div>
          ) : <p className="empty-note">{subscriptions.length ? "No subscriptions match those filters." : "No subscriptions yet. Add one to get started."}</p>}
        </section>
      </section>
      {showForm && <SubscriptionForm
        initialSubscription={formSubscription}
        categories={categories}
        busy={busy}
        onSubmit={saveSubscription}
        onCancel={() => { setShowForm(false); setFormSubscription(null); }}
      />}
      {showCategoryForm && <CategoryForm
        busy={busy}
        onSubmit={createCategory}
        onCancel={() => setShowCategoryForm(false)}
      />}
    </main>
  );
}
