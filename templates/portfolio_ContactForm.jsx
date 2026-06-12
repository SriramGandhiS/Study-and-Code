/**
 * ContactForm.jsx — Portfolio contact form with client-side validation.
 *
 * Features:
 *   - Fields: name, email, message
 *   - Per-field validation (required, email format, message min length)
 *   - Loading state during submit
 *   - Toast notification on success/error
 *   - POSTs to /api/contact via fetch
 *
 * Add:    feat: add contact form component with client-side validation
 * Delete: refactor: replace ContactForm with react-hook-form implementation
 */

import React, { useState, useCallback, useRef } from 'react';

// ─── Validation helpers ──────────────────────────────────────────────────────

const EMAIL_REGEX = /^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/;
const MIN_MESSAGE_LENGTH = 20;
const MAX_MESSAGE_LENGTH = 1000;

function validateForm(fields) {
  const errors = {};
  if (!fields.name.trim()) {
    errors.name = 'Name is required.';
  } else if (fields.name.trim().length < 2) {
    errors.name = 'Name must be at least 2 characters.';
  }
  if (!fields.email.trim()) {
    errors.email = 'Email is required.';
  } else if (!EMAIL_REGEX.test(fields.email.trim())) {
    errors.email = 'Please enter a valid email address.';
  }
  if (!fields.message.trim()) {
    errors.message = 'Message is required.';
  } else if (fields.message.trim().length < MIN_MESSAGE_LENGTH) {
    errors.message = `Message must be at least ${MIN_MESSAGE_LENGTH} characters.`;
  } else if (fields.message.trim().length > MAX_MESSAGE_LENGTH) {
    errors.message = `Message must not exceed ${MAX_MESSAGE_LENGTH} characters.`;
  }
  return errors;
}

// ─── Toast component ─────────────────────────────────────────────────────────

function Toast({ message, type, onDismiss }) {
  const bg   = type === 'success' ? '#22c55e' : '#ef4444';
  const icon = type === 'success' ? '✅' : '❌';
  return (
    <div
      role="alert"
      aria-live="polite"
      style={{
        position: 'fixed',
        bottom: '28px',
        right: '28px',
        background: bg,
        color: '#fff',
        padding: '14px 22px',
        borderRadius: '12px',
        boxShadow: '0 8px 32px rgba(0,0,0,0.35)',
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        fontSize: '0.9rem',
        fontWeight: 600,
        zIndex: 9999,
        animation: 'slideInToast 0.3s ease',
        maxWidth: '340px',
        fontFamily: "'Inter', sans-serif",
      }}
    >
      <span>{icon}</span>
      <span style={{ flexGrow: 1 }}>{message}</span>
      <button
        onClick={onDismiss}
        aria-label="Dismiss notification"
        style={{
          background: 'transparent',
          border: 'none',
          color: '#fff',
          cursor: 'pointer',
          fontSize: '1rem',
          padding: '0 0 0 8px',
          lineHeight: 1,
        }}
      >
        ✕
      </button>
    </div>
  );
}

// ─── Input field component ────────────────────────────────────────────────────

function FormField({ id, label, type, value, onChange, onBlur, error, placeholder, rows, maxLength }) {
  const sharedStyle = {
    width: '100%',
    padding: '12px 14px',
    background: '#0f172a',
    border: `1.5px solid ${error ? '#f87171' : '#1e293b'}`,
    borderRadius: '10px',
    color: '#e2e8f0',
    fontSize: '0.9rem',
    fontFamily: "'Inter', sans-serif",
    outline: 'none',
    boxSizing: 'border-box',
    transition: 'border-color 0.2s',
    resize: type === 'textarea' ? 'vertical' : undefined,
    minHeight: type === 'textarea' ? '130px' : undefined,
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
      <label
        htmlFor={id}
        style={{ fontSize: '0.82rem', fontWeight: 600, color: '#94a3b8', letterSpacing: '0.04em' }}
      >
        {label}
      </label>
      {type === 'textarea' ? (
        <textarea
          id={id}
          name={id}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          placeholder={placeholder}
          rows={rows || 5}
          maxLength={maxLength}
          style={sharedStyle}
          aria-describedby={error ? `${id}-error` : undefined}
        />
      ) : (
        <input
          id={id}
          name={id}
          type={type}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          placeholder={placeholder}
          style={sharedStyle}
          aria-describedby={error ? `${id}-error` : undefined}
        />
      )}
      {error && (
        <span
          id={`${id}-error`}
          role="alert"
          style={{ fontSize: '0.78rem', color: '#f87171', marginTop: '2px' }}
        >
          {error}
        </span>
      )}
    </div>
  );
}

// ─── Main ContactForm component ───────────────────────────────────────────────

function ContactForm() {
  const [fields, setFields]   = useState({ name: '', email: '', message: '' });
  const [errors, setErrors]   = useState({});
  const [touched, setTouched] = useState({});
  const [loading, setLoading] = useState(false);
  const [toast, setToast]     = useState(null); // { message, type }
  const toastTimer = useRef(null);

  const handleChange = useCallback((e) => {
    const { name, value } = e.target;
    setFields((prev) => ({ ...prev, [name]: value }));
    // Clear field error on change if already touched
    if (touched[name]) {
      const newErrors = validateForm({ ...fields, [name]: value });
      setErrors((prev) => ({ ...prev, [name]: newErrors[name] }));
    }
  }, [fields, touched]);

  const handleBlur = useCallback((e) => {
    const { name } = e.target;
    setTouched((prev) => ({ ...prev, [name]: true }));
    const newErrors = validateForm(fields);
    setErrors((prev) => ({ ...prev, [name]: newErrors[name] }));
  }, [fields]);

  const showToast = useCallback((message, type) => {
    setToast({ message, type });
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 5000);
  }, []);

  const handleSubmit = useCallback(async (e) => {
    e.preventDefault();
    // Mark all fields as touched
    setTouched({ name: true, email: true, message: true });
    const formErrors = validateForm(fields);
    setErrors(formErrors);
    if (Object.keys(formErrors).length > 0) return;

    setLoading(true);
    try {
      const response = await fetch('/api/contact', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({
          name:    fields.name.trim(),
          email:   fields.email.trim(),
          message: fields.message.trim(),
        }),
      });
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.message || `Server error: ${response.status}`);
      }
      showToast("Message sent! I'll get back to you shortly. 🎉", 'success');
      setFields({ name: '', email: '', message: '' });
      setTouched({});
      setErrors({});
    } catch (err) {
      showToast(err.message || 'Something went wrong. Please try again.', 'error');
    } finally {
      setLoading(false);
    }
  }, [fields, showToast]);

  return (
    <>
      <style>{`
        @keyframes slideInToast {
          from { opacity: 0; transform: translateY(20px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}</style>

      <form
        id="contact-form"
        onSubmit={handleSubmit}
        noValidate
        aria-label="Contact form"
        style={{
          display:        'flex',
          flexDirection:  'column',
          gap:            '18px',
          maxWidth:       '520px',
          width:          '100%',
          fontFamily:     "'Inter', 'Outfit', sans-serif",
        }}
      >
        <FormField
          id="name"
          label="Your Name"
          type="text"
          value={fields.name}
          onChange={handleChange}
          onBlur={handleBlur}
          error={touched.name ? errors.name : ''}
          placeholder="Sriram Venkatesh"
        />
        <FormField
          id="email"
          label="Email Address"
          type="email"
          value={fields.email}
          onChange={handleChange}
          onBlur={handleBlur}
          error={touched.email ? errors.email : ''}
          placeholder="hello@example.com"
        />
        <FormField
          id="message"
          label={`Message (${fields.message.length} / ${MAX_MESSAGE_LENGTH})`}
          type="textarea"
          value={fields.message}
          onChange={handleChange}
          onBlur={handleBlur}
          error={touched.message ? errors.message : ''}
          placeholder="Hi! I'd love to discuss a project opportunity..."
          maxLength={MAX_MESSAGE_LENGTH}
        />
        <button
          id="contact-submit-btn"
          type="submit"
          disabled={loading}
          style={{
            padding:        '13px 28px',
            borderRadius:   '10px',
            border:         'none',
            background:     loading
              ? '#334155'
              : 'linear-gradient(135deg, #6366f1, #8b5cf6)',
            color:          '#fff',
            fontSize:       '0.9rem',
            fontWeight:     700,
            cursor:         loading ? 'not-allowed' : 'pointer',
            transition:     'all 0.25s ease',
            display:        'flex',
            alignItems:     'center',
            justifyContent: 'center',
            gap:            '8px',
            boxShadow:      loading ? 'none' : '0 4px 20px rgba(99,102,241,0.4)',
            alignSelf:      'flex-start',
          }}
          aria-busy={loading}
        >
          {loading ? (
            <>
              <span style={{ display: 'inline-block', animation: 'spin 1s linear infinite' }}>⏳</span>
              Sending…
            </>
          ) : (
            '✉️  Send Message'
          )}
        </button>
      </form>

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onDismiss={() => setToast(null)}
        />
      )}
    </>
  );
}

export default ContactForm;
