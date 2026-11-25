import { ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import { ticketApi } from '../services/ticketApi'

// 1. Les types TypeScript
export type TicketStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'
export type TicketPriority = 'LOW' | 'HIGH'
export type SortField = 'id' | 'title' | 'priority' | 'status' | 'dueDate'
export type SortOrder = 'asc' | 'desc'
export type IssueLinkType = 'blocks' | 'is_blocked_by' | 'relates_to'
export type TicketLabel = 'Frontend' | 'Backend' | 'Bug' | 'Feature' | 'UI/UX' | 'DevOps'
export type ViewMode = 'kanban' | 'list'

export interface User {
    id: string
    name: string
    initials: string
    color: string
}

export const TEAM_MEMBERS: User[] = [
    { id: 'user-jd', name: 'John Doe', initials: 'JD', color: '#00875a' },
    { id: 'user-sc', name: 'Sarah Connor', initials: 'SC', color: '#6554c0' },
    { id: 'user-ad', name: 'Alex Dev', initials: 'AD', color: '#0052cc' },
    { id: 'user-pm', name: 'Chef de projet', initials: 'MT', color: '#ffab00' }
]

export const ALL_LABELS: TicketLabel[] = [
    'Frontend',
    'Backend',
    'Bug',
    'Feature',
    'UI/UX',
    'DevOps'
]

export interface TicketComment {
    id: number
    author: string
    content: string
    createdAt: number
}

export interface TicketHistoryItem {
    id: number
    author: string
    action: string
    details?: string
    timestamp: number
}

export interface IssueLink {
    id: number
    targetTicketId: number
    type: IssueLinkType
}

export interface Ticket {
    id: number
    title: string
    description?: string
    status: TicketStatus
    priority: TicketPriority
    createdAt: number
    dueDate?: string // YYYY-MM-DD
    assigneeId?: string // User.id
    labels: TicketLabel[]
    comments: TicketComment[]
    history: TicketHistoryItem[]
    links: IssueLink[]
}

const STORAGE_KEY = 'mini_tracker_tickets_v2'
const VIEW_MODE_KEY = 'mini_tracker_view_mode'

export function getInitialDemoTickets(): Ticket[] {
    const now = Date.now()
    const tomorrow = new Date(now + 86400000).toISOString().slice(0, 10)
    const nextWeek = new Date(now + 86400000 * 7).toISOString().slice(0, 10)
    const yesterday = new Date(now - 86400000).toISOString().slice(0, 10)

    return [
        {
            id: 1,
            title: 'Configurer le repo Git',
            description: 'Initialiser le dépôt GitHub, configurer la branche main, ajouter le .gitignore et les règles de commit.',
            status: 'DONE',
            priority: 'HIGH',
            createdAt: now - 3600000 * 48,
            dueDate: yesterday,
            assigneeId: 'user-jd',
            labels: ['DevOps'],
            comments: [
                {
                    id: 101,
                    author: 'John Doe',
                    content: 'Le repository a été configuré avec les hooks de pre-commit.',
                    createdAt: now - 3600000 * 36
                }
            ],
            history: [
                { id: 201, author: 'John Doe', action: 'a créé la demande', timestamp: now - 3600000 * 48 },
                { id: 202, author: 'John Doe', action: 'a changé l\'état', details: 'À FAIRE → EN COURS', timestamp: now - 3600000 * 40 },
                { id: 203, author: 'John Doe', action: 'a changé l\'état', details: 'EN COURS → TERMINÉ', timestamp: now - 3600000 * 30 }
            ],
            links: [{ id: 301, targetTicketId: 2, type: 'blocks' }]
        },
        {
            id: 2,
            title: 'Créer le formulaire Vue 3',
            description: 'Mettre en place le composant de saisie avec validation du titre et gestion de la priorité.',
            status: 'IN_PROGRESS',
            priority: 'HIGH',
            createdAt: now - 3600000 * 12,
            dueDate: tomorrow,
            assigneeId: 'user-jd',
            labels: ['Frontend', 'UI/UX'],
            comments: [
                { id: 102, author: 'Chef de projet', content: 'Pense bien à ajouter la validation sur le champ résumé.', createdAt: now - 3600000 * 8 },
                { id: 103, author: 'John Doe', content: 'C\'est en cours, les tests unitaires suivent dans le ticket KAN-3.', createdAt: now - 3600000 * 4 }
            ],
            history: [
                { id: 204, author: 'Chef de projet', action: 'a créé la demande', timestamp: now - 3600000 * 12 },
                { id: 205, author: 'John Doe', action: 'a changé l\'état', details: 'À FAIRE → EN COURS', timestamp: now - 3600000 * 6 }
            ],
            links: [
                { id: 302, targetTicketId: 1, type: 'is_blocked_by' },
                { id: 303, targetTicketId: 3, type: 'relates_to' }
            ]
        },
        {
            id: 3,
            title: 'Écrire les tests Vitest',
            description: 'Couvrir les actions du store Pinia (addTicket, toggleStatus, deleteTicket, filtres et tris).',
            status: 'TODO',
            priority: 'LOW',
            createdAt: now - 3600000 * 3,
            dueDate: nextWeek,
            assigneeId: 'user-sc',
            labels: ['Frontend', 'Feature'],
            comments: [],
            history: [
                { id: 206, author: 'John Doe', action: 'a créé la demande', timestamp: now - 3600000 * 3 }
            ],
            links: [{ id: 304, targetTicketId: 2, type: 'relates_to' }]
        }
    ]
}

// 2. Le Store Pinia
export const useTicketStore = defineStore('ticket', () => {
    const currentUserId = 'user-jd'
    const currentUser = TEAM_MEMBERS.find(u => u.id === currentUserId)!

    // --- CHARGEMENT INITIAL (LocalStorage) ---
    function loadSavedTickets(): Ticket[] {
        if (typeof window === 'undefined') return getInitialDemoTickets()
        try {
            const raw = localStorage.getItem(STORAGE_KEY)
            if (raw) {
                const parsed = JSON.parse(raw)
                if (Array.isArray(parsed) && parsed.length > 0) {
                    return parsed
                }
            }
        } catch {
            // fallback
        }
        return getInitialDemoTickets()
    }

    function loadSavedViewMode(): ViewMode {
        if (typeof window === 'undefined') return 'kanban'
        try {
            const raw = localStorage.getItem(VIEW_MODE_KEY)
            if (raw === 'list' || raw === 'kanban') return raw
        } catch {
            // fallback
        }
        return 'kanban'
    }

    // --- STATE ---
    const tickets = ref<Ticket[]>(loadSavedTickets())
    const viewMode = ref<ViewMode>(loadSavedViewMode())
    const selectedTicketId = ref<number | null>(null)

    // Filtres
    const filterStatus = ref<TicketStatus | 'ALL'>('ALL')
    const filterOnlyMine = ref(false)
    const filterLabel = ref<TicketLabel | 'ALL'>('ALL')
    const filterAssignee = ref<string | 'ALL'>('ALL')
    const searchQuery = ref('')
    const sortBy = ref<SortField>('id')
    const sortOrder = ref<SortOrder>('desc')

    // État réseau / synchronisation Backend
    const isLoading = ref(false)
    const isBackendConnected = ref(false)
    const apiError = ref<string | null>(null)

    // --- PERSISTANCE AUTOMATIQUE (LocalStorage) ---
    if (typeof window !== 'undefined') {
        watch(
            tickets,
            (newTickets) => {
                try {
                    localStorage.setItem(STORAGE_KEY, JSON.stringify(newTickets))
                } catch (e) {
                    console.error('Erreur sauvegarde localStorage', e)
                }
            },
            { deep: true }
        )

        watch(viewMode, (newMode) => {
            try {
                localStorage.setItem(VIEW_MODE_KEY, newMode)
            } catch (e) {
                console.error('Erreur sauvegarde viewMode', e)
            }
        })
    }

    // --- GETTERS ---
    const selectedTicket = computed(() => {
        if (selectedTicketId.value === null) return null
        return tickets.value.find(t => t.id === selectedTicketId.value) ?? null
    })

    const filteredTickets = computed(() => {
        let result = tickets.value

        // Filtre par statut
        if (filterStatus.value !== 'ALL') {
            result = result.filter(t => t.status === filterStatus.value)
        }

        // Filtre "Mes tickets"
        if (filterOnlyMine.value) {
            result = result.filter(t => t.assigneeId === currentUserId)
        }

        // Filtre par Assignee spécifique
        if (filterAssignee.value !== 'ALL') {
            if (filterAssignee.value === 'UNASSIGNED') {
                result = result.filter(t => !t.assigneeId)
            } else {
                result = result.filter(t => t.assigneeId === filterAssignee.value)
            }
        }

        // Filtre par Label
        if (filterLabel.value !== 'ALL') {
            result = result.filter(t => t.labels?.includes(filterLabel.value as TicketLabel))
        }

        // Recherche textuelle
        if (searchQuery.value.trim()) {
            const query = searchQuery.value.toLowerCase().trim()
            result = result.filter(t =>
                t.title.toLowerCase().includes(query) ||
                (t.description && t.description.toLowerCase().includes(query)) ||
                `kan-${t.id}`.includes(query)
            )
        }

        return result
    })

    const priorityRank: Record<TicketPriority, number> = {
        HIGH: 2,
        LOW: 1
    }

    const statusRank: Record<TicketStatus, number> = {
        TODO: 1,
        IN_PROGRESS: 2,
        DONE: 3
    }

    const sortedTickets = computed(() => {
        const list = [...filteredTickets.value]

        return list.sort((a, b) => {
            let comparison = 0

            switch (sortBy.value) {
                case 'id':
                    comparison = a.id - b.id
                    break
                case 'title':
                    comparison = a.title.localeCompare(b.title, undefined, { sensitivity: 'base' })
                    break
                case 'priority':
                    comparison = priorityRank[a.priority] - priorityRank[b.priority]
                    break
                case 'status':
                    comparison = statusRank[a.status] - statusRank[b.status]
                    break
                case 'dueDate':
                    comparison = (a.dueDate || '9999-99-99').localeCompare(b.dueDate || '9999-99-99')
                    break
            }

            return sortOrder.value === 'asc' ? comparison : -comparison
        })
    })

    // Groupements pour le Kanban
    const kanbanColumns = computed(() => {
        return {
            TODO: sortedTickets.value.filter(t => t.status === 'TODO'),
            IN_PROGRESS: sortedTickets.value.filter(t => t.status === 'IN_PROGRESS'),
            DONE: sortedTickets.value.filter(t => t.status === 'DONE')
        }
    })

    const stats = computed(() => {
        const total = tickets.value.length
        const todo = tickets.value.filter(t => t.status === 'TODO').length
        const inProgress = tickets.value.filter(t => t.status === 'IN_PROGRESS').length
        const done = tickets.value.filter(t => t.status === 'DONE').length

        return { total, todo, inProgress, done }
    })

    // --- HELPER LOG HISTORY ---
    function logHistory(ticket: Ticket, action: string, details?: string, author: string = currentUser.name) {
        if (!ticket.history) {
            ticket.history = []
        }
        ticket.history.unshift({
            id: Date.now() + Math.floor(Math.random() * 1000),
            author,
            action,
            details,
            timestamp: Date.now()
        })
    }

    // --- ACTIONS ---
    async function fetchTickets() {
        isLoading.value = true
        apiError.value = null
        try {
            const remoteTickets = await ticketApi.getTickets()
            if (Array.isArray(remoteTickets)) {
                tickets.value = remoteTickets
                isBackendConnected.value = true
            }
        } catch (e: any) {
            isBackendConnected.value = false
            apiError.value = e?.message || 'Connexion backend indisponible'
        } finally {
            isLoading.value = false
        }
    }

    async function addTicket(
        title: string,
        priority: TicketPriority,
        description?: string,
        assigneeId?: string,
        dueDate?: string,
        labels: TicketLabel[] = []
    ) {
        const tempId = Date.now()
        const newTicket: Ticket = {
            id: tempId,
            title: title.trim(),
            description: description?.trim() || undefined,
            status: 'TODO',
            priority,
            createdAt: Date.now(),
            dueDate: dueDate || undefined,
            assigneeId: assigneeId || undefined,
            labels: [...labels],
            comments: [],
            history: [
                {
                    id: Date.now() + 1,
                    author: currentUser.name,
                    action: 'a créé la demande',
                    timestamp: Date.now()
                }
            ],
            links: []
        }
        tickets.value.push(newTicket)

        try {
            const created = await ticketApi.createTicket({
                title: title.trim(),
                priority,
                description: description?.trim() || undefined,
                assigneeId: assigneeId || undefined,
                dueDate: dueDate || undefined,
                labels: [...labels]
            })
            if (created && created.id) {
                const idx = tickets.value.findIndex(t => t.id === tempId)
                if (idx !== -1) {
                    tickets.value[idx] = created
                }
                isBackendConnected.value = true
            }
        } catch (e: any) {
            console.warn('Backend sync warning (addTicket):', e?.message)
        }
    }

    async function toggleStatus(id: number) {
        const ticket = tickets.value.find(t => t.id === id)
        if (!ticket) return

        const oldStatus = ticket.status
        const nextStatus: Record<TicketStatus, TicketStatus> = {
            TODO: 'IN_PROGRESS',
            IN_PROGRESS: 'DONE',
            DONE: 'TODO'
        }
        ticket.status = nextStatus[ticket.status]

        const statusLabels: Record<TicketStatus, string> = {
            TODO: 'À FAIRE',
            IN_PROGRESS: 'EN COURS',
            DONE: 'TERMINÉ'
        }
        logHistory(
            ticket,
            'a changé l\'état',
            `${statusLabels[oldStatus]} → ${statusLabels[ticket.status]}`
        )

        try {
            const updated = await ticketApi.toggleStatus(id)
            if (updated) {
                const idx = tickets.value.findIndex(t => t.id === id)
                if (idx !== -1) {
                    tickets.value[idx] = updated
                }
                isBackendConnected.value = true
            }
        } catch (e: any) {
            console.warn('Backend sync warning (toggleStatus):', e?.message)
        }
    }

    async function setStatus(id: number, status: TicketStatus) {
        const ticket = tickets.value.find(t => t.id === id)
        if (ticket && ticket.status !== status) {
            const oldStatus = ticket.status
            ticket.status = status
            const statusLabels: Record<TicketStatus, string> = {
                TODO: 'À FAIRE',
                IN_PROGRESS: 'EN COURS',
                DONE: 'TERMINÉ'
            }
            logHistory(
                ticket,
                'a changé l\'état',
                `${statusLabels[oldStatus]} → ${statusLabels[status]}`
            )

            try {
                const updated = await ticketApi.updateStatus(id, status)
                if (updated) {
                    const idx = tickets.value.findIndex(t => t.id === id)
                    if (idx !== -1) {
                        tickets.value[idx] = updated
                    }
                    isBackendConnected.value = true
                }
            } catch (e: any) {
                console.warn('Backend sync warning (setStatus):', e?.message)
            }
        }
    }

    function moveTicket(ticketId: number, targetStatus: TicketStatus) {
        setStatus(ticketId, targetStatus)
    }

    async function setPriority(id: number, priority: TicketPriority) {
        const ticket = tickets.value.find(t => t.id === id)
        if (ticket && ticket.priority !== priority) {
            const oldPrio = ticket.priority === 'HIGH' ? 'Haute' : 'Basse'
            const newPrio = priority === 'HIGH' ? 'Haute' : 'Basse'
            ticket.priority = priority
            logHistory(ticket, 'a changé la priorité', `${oldPrio} → ${newPrio}`)

            try {
                const updated = await ticketApi.updatePriority(id, priority)
                if (updated) {
                    const idx = tickets.value.findIndex(t => t.id === id)
                    if (idx !== -1) {
                        tickets.value[idx] = updated
                    }
                    isBackendConnected.value = true
                }
            } catch (e: any) {
                console.warn('Backend sync warning (setPriority):', e?.message)
            }
        }
    }

    async function setAssignee(id: number, assigneeId?: string) {
        const ticket = tickets.value.find(t => t.id === id)
        if (!ticket) return

        const targetUser = TEAM_MEMBERS.find(u => u.id === assigneeId)
        const name = targetUser ? targetUser.name : 'Non assigné'
        ticket.assigneeId = assigneeId || undefined
        logHistory(ticket, 'a assigné la demande à', name)

        try {
            const updated = await ticketApi.updateAssignee(id, assigneeId)
            if (updated) {
                const idx = tickets.value.findIndex(t => t.id === id)
                if (idx !== -1) {
                    tickets.value[idx] = updated
                }
                isBackendConnected.value = true
            }
        } catch (e: any) {
            console.warn('Backend sync warning (setAssignee):', e?.message)
        }
    }

    async function setDueDate(id: number, dueDate?: string) {
        const ticket = tickets.value.find(t => t.id === id)
        if (!ticket) return

        ticket.dueDate = dueDate || undefined
        logHistory(ticket, 'a mis à jour la date d\'échéance', dueDate ? `Échéance : ${dueDate}` : 'Retirée')

        try {
            const updated = await ticketApi.updateDueDate(id, dueDate)
            if (updated) {
                const idx = tickets.value.findIndex(t => t.id === id)
                if (idx !== -1) {
                    tickets.value[idx] = updated
                }
                isBackendConnected.value = true
            }
        } catch (e: any) {
            console.warn('Backend sync warning (setDueDate):', e?.message)
        }
    }

    async function toggleLabel(id: number, label: TicketLabel) {
        const ticket = tickets.value.find(t => t.id === id)
        if (!ticket) return

        if (!ticket.labels) ticket.labels = []
        const idx = ticket.labels.indexOf(label)
        if (idx !== -1) {
            ticket.labels.splice(idx, 1)
            logHistory(ticket, `a retiré l'étiquette « ${label} »`)
        } else {
            ticket.labels.push(label)
            logHistory(ticket, `a ajouté l'étiquette « ${label} »`)
        }

        try {
            const updated = await ticketApi.toggleLabel(id, label)
            if (updated) {
                const index = tickets.value.findIndex(t => t.id === id)
                if (index !== -1) {
                    tickets.value[index] = updated
                }
                isBackendConnected.value = true
            }
        } catch (e: any) {
            console.warn('Backend sync warning (toggleLabel):', e?.message)
        }
    }

    async function updateTicket(
        id: number,
        updates: Partial<Omit<Ticket, 'id' | 'comments' | 'history' | 'links'>>
    ) {
        const ticket = tickets.value.find(t => t.id === id)
        if (!ticket) return

        if (updates.title && updates.title !== ticket.title) {
            logHistory(ticket, 'a modifié le résumé', `« ${updates.title} »`)
        }
        if (updates.description !== undefined && updates.description !== ticket.description) {
            logHistory(ticket, 'a mis à jour la description')
        }

        Object.assign(ticket, updates)

        try {
            const updated = await ticketApi.patchTicket(id, updates)
            if (updated) {
                const index = tickets.value.findIndex(t => t.id === id)
                if (index !== -1) {
                    tickets.value[index] = updated
                }
                isBackendConnected.value = true
            }
        } catch (e: any) {
            console.warn('Backend sync warning (updateTicket):', e?.message)
        }
    }

    async function deleteTicket(id: number) {
        tickets.value.forEach(t => {
            if (t.links) {
                t.links = t.links.filter(l => l.targetTicketId !== id)
            }
        })

        const index = tickets.value.findIndex(t => t.id === id)
        if (index !== -1) {
            tickets.value.splice(index, 1)
            if (selectedTicketId.value === id) {
                selectedTicketId.value = null
            }
        }

        try {
            await ticketApi.deleteTicket(id)
            isBackendConnected.value = true
        } catch (e: any) {
            console.warn('Backend sync warning (deleteTicket):', e?.message)
        }
    }

    function selectTicket(id: number | null) {
        selectedTicketId.value = id
    }

    function toggleSortOrder() {
        sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
    }

    async function resetToDemoData() {
        tickets.value = getInitialDemoTickets()
        filterStatus.value = 'ALL'
        filterOnlyMine.value = false
        filterLabel.value = 'ALL'
        filterAssignee.value = 'ALL'
        searchQuery.value = ''
        selectedTicketId.value = null
        if (typeof window !== 'undefined') {
            try {
                localStorage.removeItem(STORAGE_KEY)
            } catch {
                // ignore
            }
        }

        try {
            const resetTickets = await ticketApi.resetToDemoData()
            if (Array.isArray(resetTickets) && resetTickets.length > 0) {
                tickets.value = resetTickets
                isBackendConnected.value = true
            }
        } catch (e: any) {
            console.warn('Backend sync warning (resetToDemoData):', e?.message)
        }
    }

    // --- COMMENTAIRES ---
    async function addComment(ticketId: number, content: string, author: string = currentUser.name) {
        const ticket = tickets.value.find(t => t.id === ticketId)
        if (!ticket || !content.trim()) return

        const tempCommentId = Date.now()
        if (!ticket.comments) ticket.comments = []
        ticket.comments.push({
            id: tempCommentId,
            author,
            content: content.trim(),
            createdAt: Date.now()
        })
        logHistory(ticket, 'a ajouté un commentaire')

        try {
            const createdComment = await ticketApi.addComment(ticketId, content.trim(), author)
            if (createdComment) {
                const cIdx = ticket.comments.findIndex(c => c.id === tempCommentId)
                if (cIdx !== -1) {
                    ticket.comments[cIdx] = createdComment
                }
                isBackendConnected.value = true
            }
        } catch (e: any) {
            console.warn('Backend sync warning (addComment):', e?.message)
        }
    }

    async function deleteComment(ticketId: number, commentId: number) {
        const ticket = tickets.value.find(t => t.id === ticketId)
        if (!ticket || !ticket.comments) return

        const index = ticket.comments.findIndex(c => c.id === commentId)
        if (index !== -1) {
            ticket.comments.splice(index, 1)
            logHistory(ticket, 'a supprimé un commentaire')
        }

        try {
            await ticketApi.deleteComment(ticketId, commentId)
            isBackendConnected.value = true
        } catch (e: any) {
            console.warn('Backend sync warning (deleteComment):', e?.message)
        }
    }

    // --- LIER DES ISSUES (LINKED ISSUES) ---
    async function addIssueLink(sourceId: number, targetId: number, type: IssueLinkType) {
        if (sourceId === targetId) return
        const sourceTicket = tickets.value.find(t => t.id === sourceId)
        const targetTicket = tickets.value.find(t => t.id === targetId)
        if (!sourceTicket || !targetTicket) return

        if (!sourceTicket.links) sourceTicket.links = []
        if (!targetTicket.links) targetTicket.links = []

        const alreadyLinked = sourceTicket.links.some(l => l.targetTicketId === targetId)
        if (alreadyLinked) return

        const mirrorType: Record<IssueLinkType, IssueLinkType> = {
            blocks: 'is_blocked_by',
            is_blocked_by: 'blocks',
            relates_to: 'relates_to'
        }

        const tempLinkId = Date.now()
        sourceTicket.links.push({
            id: tempLinkId,
            targetTicketId: targetId,
            type
        })

        targetTicket.links.push({
            id: tempLinkId + 1,
            targetTicketId: sourceId,
            type: mirrorType[type]
        })

        const typeLabels: Record<IssueLinkType, string> = {
            blocks: 'bloque',
            is_blocked_by: 'est bloqué par',
            relates_to: 'est lié à'
        }

        logHistory(sourceTicket, `a lié cette demande (${typeLabels[type]} KAN-${targetId})`)
        logHistory(targetTicket, `a été liée (${typeLabels[mirrorType[type]]} KAN-${sourceId})`)

        try {
            await ticketApi.addIssueLink(sourceId, targetId, type)
            isBackendConnected.value = true
        } catch (e: any) {
            console.warn('Backend sync warning (addIssueLink):', e?.message)
        }
    }

    async function removeIssueLink(sourceId: number, targetId: number) {
        const sourceTicket = tickets.value.find(t => t.id === sourceId)
        const targetTicket = tickets.value.find(t => t.id === targetId)

        if (sourceTicket && sourceTicket.links) {
            sourceTicket.links = sourceTicket.links.filter(l => l.targetTicketId !== targetId)
            logHistory(sourceTicket, `a supprimé le lien avec KAN-${targetId}`)
        }

        if (targetTicket && targetTicket.links) {
            targetTicket.links = targetTicket.links.filter(l => l.targetTicketId !== sourceId)
            logHistory(targetTicket, `a supprimé le lien avec KAN-${sourceId}`)
        }

        try {
            await ticketApi.removeIssueLink(sourceId, targetId)
            isBackendConnected.value = true
        } catch (e: any) {
            console.warn('Backend sync warning (removeIssueLink):', e?.message)
        }
    }

    function getTicketById(id: number): Ticket | undefined {
        return tickets.value.find(t => t.id === id)
    }

    function getUserById(id?: string): User | undefined {
        if (!id) return undefined
        return TEAM_MEMBERS.find(u => u.id === id)
    }

    function isOverdue(ticket: Ticket): boolean {
        if (!ticket.dueDate || ticket.status === 'DONE') return false
        const today = new Date().toISOString().slice(0, 10)
        return ticket.dueDate < today
    }

    return {
        // State
        tickets,
        viewMode,
        selectedTicketId,
        selectedTicket,
        filterStatus,
        filterOnlyMine,
        filterLabel,
        filterAssignee,
        searchQuery,
        sortBy,
        sortOrder,
        filteredTickets,
        sortedTickets,
        kanbanColumns,
        stats,
        currentUserId,
        currentUser,
        isLoading,
        isBackendConnected,
        apiError,
        // Actions
        fetchTickets,
        addTicket,
        toggleStatus,
        setStatus,
        moveTicket,
        setPriority,
        setAssignee,
        setDueDate,
        toggleLabel,
        updateTicket,
        deleteTicket,
        selectTicket,
        toggleSortOrder,
        resetToDemoData,
        addComment,
        deleteComment,
        addIssueLink,
        removeIssueLink,
        getTicketById,
        getUserById,
        isOverdue
    }
})
