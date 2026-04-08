export function connectQaSocket(token, onMessage) {
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const ws = new WebSocket(`${protocol}://${location.host}/ws/qa?token=${token}`)
  ws.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      onMessage?.(payload)
    } catch {
      onMessage?.({ type: 'raw', data: event.data })
    }
  }
  return ws
}
