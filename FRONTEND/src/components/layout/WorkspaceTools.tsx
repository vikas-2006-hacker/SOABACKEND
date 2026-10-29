import { useCallback, useEffect, useState } from 'react'
import axios from 'axios'
import { Bell, Bot, Check, CircleAlert, LoaderCircle, Send, Sparkles, X } from 'lucide-react'
import type { UserRole } from '../../types/auth'
import { assistantEnabled, assistantService, notificationService, notificationsEnabled, type WorkspaceNotification } from '../../services/workspaceService'
import { getApiErrorMessage } from '../../services/api'

const prompts: Record<UserRole, string[]> = {
  STUDENT: ['How is my attendance?', 'What is my current CGPA?', 'Which subject is my strongest?'],
  FACULTY: ['Show my assigned student count.', 'Summarize a student’s academic record.'],
  ADMIN: ['How many students are enrolled?', 'Summarize a student’s academic performance by ID.'],
}

export function WorkspaceTools({ role }: { role: UserRole }) {
  const [panel, setPanel] = useState<'notifications' | 'assistant' | null>(null)
  const [items, setItems] = useState<WorkspaceNotification[] | null>(null)
  const [unread, setUnread] = useState(0)
  const [notificationError, setNotificationError] = useState('')
  const [assistantError, setAssistantError] = useState('')
  const [question, setQuestion] = useState('')
  const [conversation, setConversation] = useState<Array<{ role: 'user' | 'assistant'; text: string; sources?: string[] }>>([])
  const [assistantLoading, setAssistantLoading] = useState(false)
  const notificationLoading = panel === 'notifications' && notificationsEnabled && items === null && !notificationError

  const refreshUnreadCount = useCallback(async () => {
    try { setUnread(await notificationService.unreadCount()) }
    catch (error: unknown) { setNotificationError(getApiErrorMessage(error)) }
  }, [])

  useEffect(() => {
    if (!notificationsEnabled) return
    const initial = window.setTimeout(() => void refreshUnreadCount(), 0)
    const timer = window.setInterval(() => void refreshUnreadCount(), 45_000)
    return () => { window.clearTimeout(initial); window.clearInterval(timer) }
  }, [refreshUnreadCount])

  useEffect(() => {
    if (panel !== 'notifications' || !notificationsEnabled) return
    let active = true
    notificationService.list().then((records) => { if (active) setItems(records) })
      .catch((error: unknown) => { if (active) setNotificationError(getApiErrorMessage(error)) })
    return () => { active = false }
  }, [panel])

  useEffect(() => {
    if (!panel) return
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') setPanel(null) }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [panel])

  const markRead = async (item: WorkspaceNotification) => {
    try {
      await notificationService.markRead(item.id)
      setItems((current) => current?.map((entry) => entry.id === item.id ? { ...entry, readAt: new Date().toISOString() } : entry) ?? null)
      setUnread((count) => Math.max(0, count - 1))
    } catch (error: unknown) { setNotificationError(getApiErrorMessage(error)) }
  }
  const markAllRead = async () => {
    try {
      await notificationService.markAllRead()
      setItems((current) => current?.map((item) => ({ ...item, readAt: item.readAt ?? new Date().toISOString() })) ?? null)
      setUnread(0)
    } catch (error: unknown) { setNotificationError(getApiErrorMessage(error)) }
  }
  const ask = async (prompt: string) => {
    const trimmed = prompt.trim()
    if (!trimmed || assistantLoading) return
    setConversation((messages) => [...messages, { role: 'user', text: trimmed }])
    setQuestion(''); setAssistantLoading(true); setAssistantError('')
    try {
      const reply = await assistantService.ask(trimmed)
      setConversation((messages) => [...messages, { role: 'assistant', text: reply.answer, sources: reply.sources }])
    } catch (error: unknown) {
      const responseMessage = axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
        ? error.response.data.message as string
        : ''
      setAssistantError(error instanceof Error && error.message.includes('not configured') ? error.message : responseMessage || getApiErrorMessage(error))
    } finally { setAssistantLoading(false) }
  }

  return <div className="workspace-tools">
    <button type="button" className="workspace-tool-button" aria-label={unread ? `Notifications, ${unread} unread` : 'Notifications'} aria-expanded={panel === 'notifications'} onClick={() => { setPanel(panel === 'notifications' ? null : 'notifications'); setNotificationError('') }}><Bell size={18}/>{notificationsEnabled && unread > 0 && <span className="workspace-unread-count">{unread > 9 ? '9+' : unread}</span>}</button>
    <button type="button" className="workspace-assistant-button" aria-expanded={panel === 'assistant'} onClick={() => { setPanel(panel === 'assistant' ? null : 'assistant'); setAssistantError(''); setConversation([]) }}><Sparkles size={16}/><span>CampusCore Assistant</span></button>
    {panel && <section className="workspace-popover glass-popover" role="dialog" aria-modal="false" aria-labelledby="workspace-panel-title">
      <header><span className="workspace-panel-icon">{panel === 'notifications' ? <Bell size={18}/> : <Bot size={18}/>}</span><div><h2 id="workspace-panel-title">{panel === 'notifications' ? 'Notifications' : 'CampusCore Assistant'}</h2><p>{panel === 'notifications' ? 'Updates for your account' : `${role.charAt(0)}${role.slice(1).toLowerCase()} workspace`}</p></div><button type="button" aria-label="Close panel" onClick={() => setPanel(null)}><X size={17}/></button></header>
      {panel === 'notifications' ? <div className="workspace-panel-body">{!notificationsEnabled ? <Unavailable icon={<Bell size={18}/>} title="Notifications aren’t connected" detail="This deployment has no notification service configured. No unread count or sample messages are shown."/> : notificationError ? <Unavailable icon={<CircleAlert size={18}/>} title="Unable to load notifications" detail={notificationError}/> : notificationLoading ? <div className="workspace-loading" role="status"><LoaderCircle className="spin" size={18}/> Loading notifications…</div> : items?.length ? <><div className="workspace-panel-actions"><span>{unread} unread</span><button type="button" disabled={!unread} onClick={() => void markAllRead()}>Mark all read</button></div><div className="workspace-notification-list">{items.map((item) => <article className={item.readAt ? '' : 'unread'} key={item.id}><span className="notification-category">{item.category}</span><strong>{item.title}</strong><p>{item.message}</p><small>{new Date(item.createdAt).toLocaleString()}</small>{!item.readAt && <button type="button" onClick={() => void markRead(item)}>Mark as read <Check size={13}/></button>}</article>)}</div></> : <Unavailable icon={<Bell size={18}/>} title="You’re all caught up" detail="There are no notifications for your account."/>}</div> : <div className="workspace-panel-body">{!assistantEnabled ? <Unavailable icon={<Bot size={18}/>} title="Assistant service isn’t configured" detail="No AI backend is connected to this deployment. Your data stays private, and no generated answers are shown."/> : <><div className="assistant-prompt-list">{prompts[role].map((prompt) => <button type="button" key={prompt} disabled={assistantLoading} onClick={() => void ask(prompt)}>{prompt}</button>)}</div><div className="assistant-conversation" aria-live="polite">{conversation.map((entry,index)=><div className={`assistant-message ${entry.role}`} key={`${index}-${entry.role}`}><p>{entry.text}</p>{entry.role === 'assistant' && entry.sources?.length ? <small>Based on: {entry.sources.join(' · ')}</small> : null}</div>)}</div>{assistantLoading && <div className="workspace-loading" role="status"><LoaderCircle className="spin" size={18}/> Checking your authorized workspace…</div>}{assistantError && <Unavailable icon={<CircleAlert size={18}/>} title="Assistant unavailable" detail={assistantError}/ >}<form className="assistant-composer" onSubmit={(event)=>{event.preventDefault();void ask(question)}}><input className="glass-input" aria-label="Ask CampusCore Assistant" value={question} onChange={(event)=>setQuestion(event.target.value)} placeholder="Ask about your academic data" maxLength={1000}/><button type="submit" aria-label="Send question" disabled={assistantLoading||!question.trim()}><Send size={16}/></button></form></>}</div>}
    </section>}
  </div>
}

function Unavailable({ icon, title, detail }: { icon: React.ReactNode; title: string; detail: string }) { return <div className="workspace-unavailable"><span>{icon}</span><strong>{title}</strong><p>{detail}</p></div> }
