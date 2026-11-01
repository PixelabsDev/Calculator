// Theme persistence & toggle logic for Pixelabs Legal Portal
(function () {
  const STORAGE_KEY = 'pixelabs_legal_theme';
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)');

  function getSavedTheme() {
    return localStorage.getItem(STORAGE_KEY) || (prefersDark.matches ? 'dark' : 'light');
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    const btn = document.getElementById('themeToggleBtn');
    if (btn) {
      btn.textContent = theme === 'dark' ? '☀️' : '🌙';
      btn.setAttribute('aria-label', `Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`);
    }
    localStorage.setItem(STORAGE_KEY, theme);
  }

  // Apply immediately to prevent flash
  applyTheme(getSavedTheme());

  window.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('themeToggleBtn');
    if (btn) {
      btn.textContent = getSavedTheme() === 'dark' ? '☀️' : '🌙';
      btn.addEventListener('click', () => {
        const current = document.documentElement.getAttribute('data-theme') || 'light';
        applyTheme(current === 'dark' ? 'light' : 'dark');
      });
    }
  });
})();
