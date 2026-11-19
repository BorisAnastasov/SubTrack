import { useState } from "react";

export function SubscriptionForm({ initialSubscription, categories, busy, onSubmit, onCancel }) {
  const [form, setForm] = useState(() => initialSubscription ? {
    name: initialSubscription.name,
    price: initialSubscription.price,
    currency: initialSubscription.currency,
    billingCycle: initialSubscription.billingCycle,
    nextBillingDate: initialSubscription.nextBillingDate,
    paymentMethod: initialSubscription.paymentMethod || "",
    categoryId: initialSubscription.categoryId || "",
    notes: initialSubscription.notes || "",
  } : {
    name: "",
    price: "",
    currency: "USD",
    billingCycle: "MONTHLY",
    nextBillingDate: "",
    paymentMethod: "",
    categoryId: "",
    notes: "",
  });
  const [error, setError] = useState("");

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    try {
      await onSubmit({
        ...form,
        price: Number(form.price),
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        paymentMethod: form.paymentMethod.trim(),
        notes: form.notes.trim(),
      });
    } catch (requestError) {
      setError(requestError.message || "Could not save this subscription.");
    }
  }

  return (
    <div className="subscription-modal" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget && !busy) onCancel();
    }}>
      <section className="subscription-form-card" role="dialog" aria-modal="true" aria-labelledby="subscription-form-title">
        <div className="subscription-form-heading">
          <div>
            <span className="form-kicker">{initialSubscription ? "SUBSCRIPTION DETAILS" : "NEW RECURRING PAYMENT"}</span>
            <h2 id="subscription-form-title">{initialSubscription ? "Edit subscription" : "Add a subscription"}</h2>
          </div>
          <button className="icon-button" type="button" aria-label="Close form" onClick={onCancel}>×</button>
        </div>
        <form className="subscription-form" onSubmit={handleSubmit}>
          <label className="field">
            <span>Name</span>
            <input name="name" value={form.name} onChange={updateField} maxLength={100} required />
          </label>
          <div className="form-grid">
            <label className="field">
              <span>Price</span>
              <input name="price" type="number" min="0.01" step="0.01" value={form.price} onChange={updateField} required />
            </label>
            <label className="field">
              <span>Currency</span>
              <input name="currency" value={form.currency} onChange={updateField} maxLength={3} minLength={3} pattern="[A-Z]{3}" title="Use a 3-letter uppercase code, such as USD" required />
            </label>
          </div>
          <div className="form-grid">
            <label className="field">
              <span>Billing cycle</span>
              <select name="billingCycle" value={form.billingCycle} onChange={updateField}>
                <option value="WEEKLY">Weekly</option>
                <option value="MONTHLY">Monthly</option>
                <option value="YEARLY">Yearly</option>
              </select>
            </label>
            <label className="field">
              <span>Next billing date</span>
              <input name="nextBillingDate" type="date" value={form.nextBillingDate} onChange={updateField} required />
            </label>
          </div>
          <div className="form-grid">
            <label className="field">
              <span>Category</span>
              <select name="categoryId" value={form.categoryId} onChange={updateField}>
                <option value="">Uncategorized</option>
                {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
              </select>
            </label>
            <label className="field">
              <span>Payment method</span>
              <input name="paymentMethod" value={form.paymentMethod} onChange={updateField} maxLength={50} placeholder="e.g. Visa ending 1234" />
            </label>
          </div>
          <label className="field">
            <span>Notes</span>
            <textarea name="notes" value={form.notes} onChange={updateField} rows="3" maxLength="2000" placeholder="Anything you want to remember" />
          </label>
          {error && <div className="form-error" role="alert">{error}</div>}
          <div className="form-actions">
            <button className="secondary-button" type="button" onClick={onCancel} disabled={busy}>Cancel</button>
            <button className="primary-button" type="submit" disabled={busy}>
              <span>{busy ? "Saving…" : initialSubscription ? "Save changes" : "Add subscription"}</span>
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
export function CategoryForm({ busy, onSubmit, onCancel }) {
  const [name, setName] = useState("");
  const [color, setColor] = useState("#10b981");
  const [error, setError] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    try {
      await onSubmit({ name: name.trim(), color });
    } catch (requestError) {
      setError(requestError.message || "Could not create this category.");
    }
  }

  return (
    <div className="subscription-modal" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget && !busy) onCancel();
    }}>
      <section className="subscription-form-card category-form-card" role="dialog" aria-modal="true" aria-labelledby="category-form-title">
        <div className="subscription-form-heading">
          <div>
            <span className="form-kicker">MAKE IT YOURS</span>
            <h2 id="category-form-title">Create a category</h2>
          </div>
          <button className="icon-button" type="button" aria-label="Close form" onClick={onCancel}>×</button>
        </div>
        <form className="subscription-form" onSubmit={handleSubmit}>
          <label className="field">
            <span>Name</span>
            <input value={name} onChange={(event) => setName(event.target.value)} maxLength={50} required />
          </label>
          <label className="field color-field">
            <span>Color</span>
            <input type="color" value={color} onChange={(event) => setColor(event.target.value)} />
          </label>
          {error && <div className="form-error" role="alert">{error}</div>}
          <div className="form-actions">
            <button className="secondary-button" type="button" onClick={onCancel} disabled={busy}>Cancel</button>
            <button className="primary-button" type="submit" disabled={busy}>
              <span>{busy ? "Saving…" : "Create category"}</span>
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
