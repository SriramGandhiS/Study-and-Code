/**
 * useTheme.js — Custom React hook for dark/light theme switching.
 *
 * Features:
 *   - Persists theme choice to localStorage
 *   - Detects system preference via prefers-color-scheme on first load
 *   - Applies data-theme attribute to document root for CSS variable switching
 *
 * Returns: { theme, toggleTheme, isDark, setTheme }
 *
 * Add:    feat: add useTheme hook for dark/light mode with localStorage persistence
 * Delete: refactor: merge theme logic into ThemeContext provider
 */

import { useState, useEffect, useCallback } from 'react';

/** localStorage key for persisted theme */
const STORAGE_KEY = 'portfolio_theme';

/** Valid theme values */
const THEMES = Object.freeze({ DARK: 'dark', LIGHT: 'light' });

/**
 * Reads the initial theme in priority order:
 *   1. Persisted localStorage value
 *   2. System preference (prefers-color-scheme: dark)
 *   3. Default: 'light'
 *
 * @returns {'dark' | 'light'} initial theme string
 */
function resolveInitialTheme() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === THEMES.DARK || stored === THEMES.LIGHT) {
      return stored;
    }
  } catch (err) {
    // localStorage unavailable (SSR, private browsing, etc.)
    console.warn('[useTheme] localStorage unavailable:', err.message);
  }

  // Check system preference
  if (typeof window !== 'undefined' && window.matchMedia) {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    return prefersDark ? THEMES.DARK : THEMES.LIGHT;
  }

  return THEMES.LIGHT;
}

/**
 * Applies the theme to the document root via data-theme attribute
 * and persists it to localStorage.
 *
 * @param {'dark' | 'light'} theme the theme to apply
 */
function applyTheme(theme) {
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme', theme);
    document.documentElement.classList.toggle('dark', theme === THEMES.DARK);
    document.documentElement.classList.toggle('light', theme === THEMES.LIGHT);
  }
  try {
    localStorage.setItem(STORAGE_KEY, theme);
  } catch (err) {
    console.warn('[useTheme] Could not persist theme:', err.message);
  }
}

/**
 * useTheme — custom hook for theme management.
 *
 * @returns {{
 *   theme: 'dark' | 'light',
 *   isDark: boolean,
 *   toggleTheme: () => void,
 *   setTheme: (theme: 'dark' | 'light') => void
 * }}
 */
export function useTheme() {
  const [theme, setThemeState] = useState(() => resolveInitialTheme());

  // Apply theme on mount and whenever theme state changes
  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  // Listen for system preference changes at runtime
  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const handleSystemChange = (e) => {
      // Only update if the user hasn't manually chosen a theme
      const stored = localStorage.getItem(STORAGE_KEY);
      if (!stored) {
        setThemeState(e.matches ? THEMES.DARK : THEMES.LIGHT);
      }
    };

    // Modern browsers
    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', handleSystemChange);
      return () => mediaQuery.removeEventListener('change', handleSystemChange);
    }
    // Legacy browsers
    mediaQuery.addListener(handleSystemChange);
    return () => mediaQuery.removeListener(handleSystemChange);
  }, []);

  /**
   * Toggles between dark and light themes.
   */
  const toggleTheme = useCallback(() => {
    setThemeState((prev) => (prev === THEMES.DARK ? THEMES.LIGHT : THEMES.DARK));
  }, []);

  /**
   * Explicitly sets the theme to a given value.
   *
   * @param {'dark' | 'light'} newTheme
   */
  const setTheme = useCallback((newTheme) => {
    if (newTheme !== THEMES.DARK && newTheme !== THEMES.LIGHT) {
      console.error('[useTheme] Invalid theme value:', newTheme);
      return;
    }
    setThemeState(newTheme);
  }, []);

  return {
    theme,
    isDark: theme === THEMES.DARK,
    toggleTheme,
    setTheme,
  };
}

export default useTheme;
