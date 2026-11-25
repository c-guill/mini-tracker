import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTicketStore } from './ticketStore'

vi.mock('../services/ticketApi', () => ({
    ticketApi: {
        getTickets: vi.fn().mockResolvedValue([]),
        getTicketById: vi.fn(),
        getStats: vi.fn().mockResolvedValue({ total: 0, todo: 0, inProgress: 0, done: 0 }),
        createTicket: vi.fn().mockImplementation((payload) => Promise.resolve({
            id: Date.now(),
            ...payload,
            status: 'TODO',
            createdAt: Date.now(),
            comments: [],
            history: [],
            links: []
        })),
        updateTicket: vi.fn(),
        patchTicket: vi.fn().mockResolvedValue(null),
        updateStatus: vi.fn().mockResolvedValue(null),
        toggleStatus: vi.fn().mockResolvedValue(null),
        updatePriority: vi.fn().mockResolvedValue(null),
        updateAssignee: vi.fn().mockResolvedValue(null),
        updateDueDate: vi.fn().mockResolvedValue(null),
        toggleLabel: vi.fn().mockResolvedValue(null),
        deleteTicket: vi.fn().mockResolvedValue(undefined),
        resetToDemoData: vi.fn().mockResolvedValue([]),
        addComment: vi.fn().mockImplementation((ticketId, content, author) => Promise.resolve({
            id: Date.now(),
            author,
            content,
            createdAt: Date.now()
        })),
        deleteComment: vi.fn().mockResolvedValue(undefined),
        addIssueLink: vi.fn().mockImplementation((sourceId, targetTicketId, type) => Promise.resolve({
            id: Date.now(),
            targetTicketId,
            type
        })),
        removeIssueLink: vi.fn().mockResolvedValue(undefined),
        getHistory: vi.fn().mockResolvedValue([]),
        getUsers: vi.fn().mockResolvedValue([])
    }
}))

describe('TicketStore (Pinia)', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        // Nettoyer le localStorage pour isoler les tests
        if (typeof window !== 'undefined') {
            window.localStorage.clear()
        }
    })

    it('initialise avec les tickets de démonstration', () => {
        const store = useTicketStore()
        expect(store.tickets.length).toBeGreaterThanOrEqual(3)
        expect(store.tickets[0].id).toBe(1)
        expect(store.tickets[0].status).toBe('DONE')
    })

    it('ajoute un nouveau ticket avec succès', () => {
        const store = useTicketStore()
        const initialCount = store.tickets.length

        store.addTicket(
            'Nouvelle fonctionnalité CI/CD',
            'HIGH',
            'Description de test',
            'user-jd',
            '2026-12-31',
            ['DevOps']
        )

        expect(store.tickets.length).toBe(initialCount + 1)
        const added = store.tickets[store.tickets.length - 1]
        expect(added.title).toBe('Nouvelle fonctionnalité CI/CD')
        expect(added.priority).toBe('HIGH')
        expect(added.status).toBe('TODO')
        expect(added.assigneeId).toBe('user-jd')
        expect(added.labels).toContain('DevOps')
        expect(added.history.length).toBe(1)
        expect(added.history[0].action).toBe('a créé la demande')
    })

    it('fait progresser le statut d\'un ticket avec toggleStatus', () => {
        const store = useTicketStore()
        const ticket = store.tickets.find(t => t.id === 3)! // TODO
        expect(ticket.status).toBe('TODO')

        store.toggleStatus(3)
        expect(ticket.status).toBe('IN_PROGRESS')

        store.toggleStatus(3)
        expect(ticket.status).toBe('DONE')

        store.toggleStatus(3)
        expect(ticket.status).toBe('TODO')
    })

    it('change directement le statut avec setStatus et consigne l\'historique', () => {
        const store = useTicketStore()
        const ticket = store.tickets.find(t => t.id === 3)!

        store.setStatus(3, 'DONE')
        expect(ticket.status).toBe('DONE')
        expect(ticket.history[0].action).toBe('a changé l\'état')
        expect(ticket.history[0].details).toContain('TERMINÉ')
    })

    it('permet de déplacer un ticket via moveTicket (Drag & Drop)', () => {
        const store = useTicketStore()
        store.moveTicket(3, 'IN_PROGRESS')
        const ticket = store.tickets.find(t => t.id === 3)!
        expect(ticket.status).toBe('IN_PROGRESS')
    })

    it('modifie la priorité et enregistre dans l\'historique', () => {
        const store = useTicketStore()
        store.setPriority(3, 'HIGH')
        const ticket = store.tickets.find(t => t.id === 3)!
        expect(ticket.priority).toBe('HIGH')
        expect(ticket.history[0].action).toBe('a changé la priorité')
    })

    it('assigne un membre de l\'équipe et met à jour l\'historique', () => {
        const store = useTicketStore()
        store.setAssignee(3, 'user-sc')
        const ticket = store.tickets.find(t => t.id === 3)!
        expect(ticket.assigneeId).toBe('user-sc')
        expect(ticket.history[0].action).toBe('a assigné la demande à')
        expect(ticket.history[0].details).toBe('Sarah Connor')
    })

    it('ajoute et supprime un label avec toggleLabel', () => {
        const store = useTicketStore()
        const ticket = store.tickets.find(t => t.id === 3)!
        expect(ticket.labels).not.toContain('Bug')

        store.toggleLabel(3, 'Bug')
        expect(ticket.labels).toContain('Bug')

        store.toggleLabel(3, 'Bug')
        expect(ticket.labels).not.toContain('Bug')
    })

    it('gère l\'ajout et la suppression de commentaires', () => {
        const store = useTicketStore()
        const ticket = store.tickets.find(t => t.id === 3)!
        const initialCommentCount = ticket.comments.length

        store.addComment(3, 'Premier commentaire de test')
        expect(ticket.comments.length).toBe(initialCommentCount + 1)
        expect(ticket.comments[ticket.comments.length - 1].content).toBe('Premier commentaire de test')

        const commentId = ticket.comments[ticket.comments.length - 1].id
        store.deleteComment(3, commentId)
        expect(ticket.comments.length).toBe(initialCommentCount)
    })

    it('gère les liens d\'issues de manière bidirectionnelle (relations miroirs)', () => {
        const store = useTicketStore()
        const ticket1 = store.tickets.find(t => t.id === 1)!
        const ticket3 = store.tickets.find(t => t.id === 3)!

        // Ticket 3 bloque Ticket 1
        store.addIssueLink(3, 1, 'blocks')

        const linkOn3 = ticket3.links.find(l => l.targetTicketId === 1)
        const linkOn1 = ticket1.links.find(l => l.targetTicketId === 3)

        expect(linkOn3).toBeDefined()
        expect(linkOn3?.type).toBe('blocks')

        expect(linkOn1).toBeDefined()
        expect(linkOn1?.type).toBe('is_blocked_by') // Miroir automatique

        // Suppression du lien
        store.removeIssueLink(3, 1)
        expect(ticket3.links.some(l => l.targetTicketId === 1)).toBe(false)
        expect(ticket1.links.some(l => l.targetTicketId === 3)).toBe(false)
    })

    it('filtre correctement par statut et par recherche', () => {
        const store = useTicketStore()

        // Filtre par statut
        store.filterStatus = 'TODO'
        expect(store.filteredTickets.every(t => t.status === 'TODO')).toBe(true)

        // Recherche par titre
        store.filterStatus = 'ALL'
        store.searchQuery = 'Git'
        expect(store.filteredTickets.length).toBe(1)
        expect(store.filteredTickets[0].title).toContain('Git')
    })

    it('filtre les tickets avec le filtre "Mes tickets"', () => {
        const store = useTicketStore()
        store.filterOnlyMine = true
        expect(store.filteredTickets.every(t => t.assigneeId === store.currentUserId)).toBe(true)
    })
})
