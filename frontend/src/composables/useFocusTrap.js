import { ref } from 'vue'

const FOCUSABLE_SELECTORS = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])'
].join(', ')

export function useFocusTrap() {
  const active = ref(false)
  let containerEl = null
  let previousFocusEl = null
  let keydownHandler = null

  function getFocusableElements() {
    if (!containerEl) return []
    return Array.from(containerEl.querySelectorAll(FOCUSABLE_SELECTORS)).filter(
      el => !el.hasAttribute('disabled') && el.tabIndex >= 0
    )
  }

  function trapFocus(e) {
    if (e.key !== 'Tab') return

    const focusable = getFocusableElements()
    if (focusable.length === 0) {
      e.preventDefault()
      return
    }

    const first = focusable[0]
    const last = focusable[focusable.length - 1]

    if (e.shiftKey) {
      if (document.activeElement === first) {
        e.preventDefault()
        last.focus()
      }
    } else {
      if (document.activeElement === last) {
        e.preventDefault()
        first.focus()
      }
    }
  }

  function activate(el) {
    if (active.value) deactivate()
    containerEl = el
    previousFocusEl = document.activeElement
    active.value = true

    keydownHandler = trapFocus
    document.addEventListener('keydown', keydownHandler)

    const focusable = getFocusableElements()
    if (focusable.length > 0) {
      focusable[0].focus()
    }
  }

  function deactivate() {
    if (!active.value) return
    active.value = false

    if (keydownHandler) {
      document.removeEventListener('keydown', keydownHandler)
      keydownHandler = null
    }

    if (previousFocusEl && typeof previousFocusEl.focus === 'function') {
      previousFocusEl.focus()
    }
    containerEl = null
    previousFocusEl = null
  }

  return { activate, deactivate, active }
}
