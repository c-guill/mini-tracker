import type {
  Ticket,
  TicketStatus,
  TicketPriority,
  TicketLabel,
  IssueLinkType,
  TicketComment,
  IssueLink,
  TicketHistoryItem,
  User
} from '../stores/ticketStore'

const API_BASE =
  (import.meta.env?.VITE_API_URL as string) ||
  (import.meta.env?.VITE_API_BASE_URL as string) ||
  '/api/v1'

export interface CreateTicketPayload {
  title: string
  priority: TicketPriority
  description?: string
  assigneeId?: string
  dueDate?: string
  labels?: TicketLabel[]
}

export interface UpdateTicketPayload {
  title: string
  description?: string
  status: TicketStatus
  priority: TicketPriority
  dueDate?: string
  assigneeId?: string
  labels: TicketLabel[]
}

export interface PatchTicketPayload {
  title?: string
  description?: string
  status?: TicketStatus
  priority?: TicketPriority
  dueDate?: string
  assigneeId?: string
  labels?: TicketLabel[]
}

export interface TicketStats {
  total: number
  todo: number
  inProgress: number
  done: number
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_BASE}${endpoint}`
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  const response = await fetch(url, {
    ...options,
    headers
  })

  if (!response.ok) {
    let errorDetail = `Erreur HTTP ${response.status}`
    try {
      const errorJson = await response.json()
      if (errorJson.message) {
        errorDetail = errorJson.message
      }
    } catch {
      // ignore
    }
    throw new Error(errorDetail)
  }

  if (response.status === 204) {
    return undefined as unknown as T
  }

  return response.json()
}

export const ticketApi = {
  // --- TICKETS ---
  async getTickets(params?: {
    status?: TicketStatus
    assigneeId?: string
    label?: TicketLabel
    search?: string
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
  }): Promise<Ticket[]> {
    const query = new URLSearchParams()
    if (params) {
      if (params.status) query.set('status', params.status)
      if (params.assigneeId) query.set('assigneeId', params.assigneeId)
      if (params.label) query.set('label', params.label)
      if (params.search) query.set('search', params.search)
      if (params.sortBy) query.set('sortBy', params.sortBy)
      if (params.sortOrder) query.set('sortOrder', params.sortOrder)
    }
    const qs = query.toString() ? `?${query.toString()}` : ''
    return request<Ticket[]>(`/tickets${qs}`)
  },

  async getTicketById(id: number): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}`)
  },

  async getStats(): Promise<TicketStats> {
    return request<TicketStats>('/tickets/stats')
  },

  async createTicket(payload: CreateTicketPayload): Promise<Ticket> {
    return request<Ticket>('/tickets', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },

  async updateTicket(id: number, payload: UpdateTicketPayload): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },

  async patchTicket(id: number, payload: PatchTicketPayload): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(payload)
    })
  },

  async updateStatus(id: number, status: TicketStatus): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    })
  },

  async toggleStatus(id: number): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}/status/toggle`, {
      method: 'POST'
    })
  },

  async updatePriority(id: number, priority: TicketPriority): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}/priority`, {
      method: 'PATCH',
      body: JSON.stringify({ priority })
    })
  },

  async updateAssignee(id: number, assigneeId?: string): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}/assignee`, {
      method: 'PATCH',
      body: JSON.stringify({ assigneeId: assigneeId || null })
    })
  },

  async updateDueDate(id: number, dueDate?: string): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}/due-date`, {
      method: 'PATCH',
      body: JSON.stringify({ dueDate: dueDate || null })
    })
  },

  async toggleLabel(id: number, label: TicketLabel): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}/labels/toggle`, {
      method: 'POST',
      body: JSON.stringify({ label })
    })
  },

  async deleteTicket(id: number): Promise<void> {
    return request<void>(`/tickets/${id}`, {
      method: 'DELETE'
    })
  },

  async resetToDemoData(): Promise<Ticket[]> {
    return request<Ticket[]>('/tickets/reset', {
      method: 'POST'
    })
  },

  // --- COMMENTAIRES ---
  async addComment(ticketId: number, content: string, author: string = 'John Doe'): Promise<TicketComment> {
    return request<TicketComment>(`/tickets/${ticketId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ content, author })
    })
  },

  async deleteComment(ticketId: number, commentId: number): Promise<void> {
    return request<void>(`/tickets/${ticketId}/comments/${commentId}`, {
      method: 'DELETE'
    })
  },

  // --- LIENS ---
  async addIssueLink(
    sourceId: number,
    targetTicketId: number,
    type: IssueLinkType
  ): Promise<IssueLink> {
    return request<IssueLink>(`/tickets/${sourceId}/links`, {
      method: 'POST',
      body: JSON.stringify({ targetTicketId, type })
    })
  },

  async removeIssueLink(sourceId: number, targetTicketId: number): Promise<void> {
    return request<void>(`/tickets/${sourceId}/links/${targetTicketId}`, {
      method: 'DELETE'
    })
  },

  // --- HISTORIQUE ---
  async getHistory(ticketId: number): Promise<TicketHistoryItem[]> {
    return request<TicketHistoryItem[]>(`/tickets/${ticketId}/history`)
  },

  // --- UTILISATEURS ---
  async getUsers(): Promise<User[]> {
    return request<User[]>('/users')
  }
}
