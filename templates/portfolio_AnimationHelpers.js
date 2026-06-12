/**
 * AnimationHelpers.js
 * Utility functions for scroll-triggered animations and entrance effects.
 * Used across Portfolio components for consistent motion design.
 */

/**
 * Creates an IntersectionObserver that triggers an animation class
 * when an element enters the viewport.
 * @param {string} selector - CSS selector for elements to observe
 * @param {string} animClass - CSS class to add on intersection
 * @param {number} threshold - 0 to 1, how much of element must be visible
 */
export function observeEntrance(selector, animClass = 'fade-in-up', threshold = 0.15) {
  const elements = document.querySelectorAll(selector);
  if (!elements.length) return null;

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add(animClass);
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold }
  );

  elements.forEach((el) => observer.observe(el));
  return observer;
}

/**
 * Stagger animation delays across a list of elements.
 * @param {NodeList|Element[]} elements
 * @param {number} baseDelay - ms
 * @param {number} increment - ms per element
 */
export function staggerDelay(elements, baseDelay = 100, increment = 80) {
  [...elements].forEach((el, i) => {
    el.style.animationDelay = `${baseDelay + i * increment}ms`;
  });
}

/**
 * Smooth scroll to a section by ID.
 */
export function scrollToSection(sectionId) {
  const el = document.getElementById(sectionId);
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

/**
 * Check if the user prefers reduced motion (accessibility).
 */
export function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * Typewriter effect — types text char by char into a DOM element.
 * @param {HTMLElement} el
 * @param {string} text
 * @param {number} speed - ms per character
 */
export async function typewriter(el, text, speed = 50) {
  el.textContent = '';
  for (const char of text) {
    el.textContent += char;
    await new Promise((r) => setTimeout(r, speed));
  }
}

/**
 * Fade out and remove an element from the DOM.
 * @param {HTMLElement} el
 * @param {number} duration - ms
 */
export function fadeOut(el, duration = 300) {
  el.style.transition = `opacity ${duration}ms ease`;
  el.style.opacity = '0';
  setTimeout(() => el.remove(), duration);
}

/**
 * Debounce a function call.
 * @param {Function} fn
 * @param {number} delay - ms
 */
export function debounce(fn, delay = 200) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}
