import { useAppStore } from '@/stores/app'
import { watch } from 'vue'

export function useTheme() {
  const appStore = useAppStore()

  function toggle() {
    appStore.toggleTheme()
  }

  function setDark() {
    appStore.setTheme('dark')
  }

  function setLight() {
    appStore.setTheme('light')
  }

  const isDark = () => appStore.theme === 'dark'

  return {
    theme: appStore.theme,
    toggle,
    setDark,
    setLight,
    isDark
  }
}
