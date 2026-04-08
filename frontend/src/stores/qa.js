import { defineStore } from 'pinia'

const HISTORY_KEY = 'rag_chat_history'

export const useQaStore = defineStore('qa', {
  state: () => ({
    selectedKbId: Number(localStorage.getItem('rag_selected_kb') || 0),
    history: JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
  }),
  actions: {
    setKbId(kbId) {
      this.selectedKbId = kbId
      localStorage.setItem('rag_selected_kb', String(kbId))
    },
    addHistory(record) {
      this.history.unshift(record)
      this.history = this.history.slice(0, 50)
      localStorage.setItem(HISTORY_KEY, JSON.stringify(this.history))
    }
  }
})
