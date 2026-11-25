<script setup lang="ts">
import { ref } from 'vue'
import {
  useTicketStore,
  type TicketStatus,
  type Ticket
} from '../stores/ticketStore'

const ticketStore = useTicketStore()

// Drag & Drop State
const draggedTicketId = ref<number | null>(null)
const activeDropTarget = ref<TicketStatus | null>(null)

const columnsConfig: { status: TicketStatus; label: string; headerColor: string }[] = [
  { status: 'TODO', label: 'À FAIRE', headerColor: '#42526e' },
  { status: 'IN_PROGRESS', label: 'EN COURS', headerColor: '#0052cc' },
  { status: 'DONE', label: 'TERMINÉ', headerColor: '#006644' }
]

function onDragStart(event: DragEvent, id: number) {
  draggedTicketId.value = id
  if (event.dataTransfer) {
    event.dataTransfer.setData('text/plain', String(id))
    event.dataTransfer.effectAllowed = 'move'
  }
}

function onDragEnd() {
  draggedTicketId.value = null
  activeDropTarget.value = null
}

function onDragOver(event: DragEvent, status: TicketStatus) {
  event.preventDefault()
  if (activeDropTarget.value !== status) {
    activeDropTarget.value = status
  }
}

function onDragLeave(status: TicketStatus) {
  if (activeDropTarget.value === status) {
    activeDropTarget.value = null
  }
}

function onDrop(status: TicketStatus) {
  if (draggedTicketId.value !== null) {
    ticketStore.moveTicket(draggedTicketId.value, status)
  }
  draggedTicketId.value = null
  activeDropTarget.value = null
}

function formatDueDate(dueDate?: string): string {
  if (!dueDate) return ''
  const parts = dueDate.split('-')
  if (parts.length < 3) return dueDate
  const month = parseInt(parts[1] || '1', 10)
  const day = parseInt(parts[2] || '1', 10)
  const months = ['janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin', 'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.']
  const monthName = months[month - 1] || ''
  return `${day} ${monthName}`
}
</script>

<template>
  <div class="kanban-board">
    <div
      v-for="col in columnsConfig"
      :key="col.status"
      class="kanban-column"
      :class="{ 'drop-active': activeDropTarget === col.status }"
      @dragover="onDragOver($event, col.status)"
      @dragenter.prevent
      @dragleave="onDragLeave(col.status)"
      @drop="onDrop(col.status)"
    >
      <!-- En-tête de colonne -->
      <div class="column-header">
        <div class="col-title-group">
          <span class="col-color-bar" :style="{ backgroundColor: col.headerColor }"></span>
          <h2 class="col-title">{{ col.label }}</h2>
          <span class="col-count">{{ ticketStore.kanbanColumns[col.status].length }}</span>
        </div>
      </div>

      <!-- Zone des cartes -->
      <div class="cards-container">
        <div
          v-if="ticketStore.kanbanColumns[col.status].length === 0"
          class="column-empty"
        >
          Glissez un ticket ici
        </div>

        <div
          v-for="ticket in ticketStore.kanbanColumns[col.status]"
          :key="ticket.id"
          class="kanban-card"
          :class="{
            dragging: draggedTicketId === ticket.id,
            completed: ticket.status === 'DONE',
            overdue: ticketStore.isOverdue(ticket)
          }"
          draggable="true"
          @dragstart="onDragStart($event, ticket.id)"
          @dragend="onDragEnd"
          @click="ticketStore.selectTicket(ticket.id)"
        >
          <!-- En-tête carte : Clé et Type -->
          <div class="card-top-row">
            <div class="card-key-group">
              <svg class="task-icon-sm" viewBox="0 0 16 16" fill="currentColor">
                <path d="M2 3a1 1 0 011-1h10a1 1 0 011 1v10a1 1 0 01-1 1H3a1 1 0 01-1-1V3zm10 2.5a.75.75 0 00-1.06-1.06L6.5 8.88 5.06 7.44a.75.75 0 00-1.06 1.06l2 2a.75.75 0 001.06 0l4.94-5z" />
              </svg>
              <span class="card-key">KAN-{{ ticket.id }}</span>
            </div>

            <!-- Date d'échéance -->
            <div
              v-if="ticket.dueDate"
              class="card-due-date"
              :class="{ 'due-alert': ticketStore.isOverdue(ticket) }"
              :title="ticketStore.isOverdue(ticket) ? 'Échéance dépassée !' : `Échéance le ${ticket.dueDate}`"
            >
              <span class="clock-icon">🕒</span>
              <span>{{ formatDueDate(ticket.dueDate) }}</span>
            </div>
          </div>

          <!-- Titre (Résumé) -->
          <p class="card-title">{{ ticket.title }}</p>

          <!-- Labels / Tags -->
          <div v-if="ticket.labels?.length" class="card-labels">
            <span
              v-for="label in ticket.labels"
              :key="label"
              class="label-badge"
              :class="`label-${label.toLowerCase()}`"
            >
              {{ label }}
            </span>
          </div>

          <!-- Pied de carte : Priorité, Compteurs, Assignee -->
          <div class="card-footer-row">
            <div class="footer-left">
              <!-- Priorité Jira -->
              <span class="prio-glyph" :class="ticket.priority.toLowerCase()" :title="`Priorité : ${ticket.priority}`">
                <svg v-if="ticket.priority === 'HIGH'" viewBox="0 0 16 16" fill="currentColor">
                  <path d="M8 2l5 6H3l5-6zm0 7l5 5H3l5-5z" />
                </svg>
                <svg v-else viewBox="0 0 16 16" fill="currentColor">
                  <path d="M8 14l-5-6h10l-5 6zm0-7l-5-5h10l-5 5z" />
                </svg>
              </span>

              <!-- Liens -->
              <span v-if="ticket.links?.length" class="card-meta-item" :title="`${ticket.links.length} issue(s) liée(s)`">
                🔗 {{ ticket.links.length }}
              </span>

              <!-- Commentaires -->
              <span v-if="ticket.comments?.length" class="card-meta-item" :title="`${ticket.comments.length} commentaire(s)`">
                💬 {{ ticket.comments.length }}
              </span>
            </div>

            <!-- Avatar Assignee -->
            <div class="footer-right">
              <div
                v-if="ticketStore.getUserById(ticket.assigneeId)"
                class="user-avatar-mini"
                :style="{ backgroundColor: ticketStore.getUserById(ticket.assigneeId)!.color }"
                :title="`Assigné à : ${ticketStore.getUserById(ticket.assigneeId)!.name}`"
              >
                {{ ticketStore.getUserById(ticket.assigneeId)!.initials }}
              </div>
              <div v-else class="unassigned-avatar" title="Non assigné">
                ?
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.kanban-board {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  align-items: start;
}

.kanban-column {
  background-color: #f4f5f7;
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  display: flex;
  flex-direction: column;
  min-height: 480px;
  transition: all 0.15s ease;
}

.kanban-column.drop-active {
  background-color: #deebff;
  border-color: #0052cc;
  box-shadow: inset 0 0 0 1px #0052cc;
}

.column-header {
  padding: 12px 14px;
  border-bottom: 1px solid var(--jira-border-subtle);
  background-color: #fafbfc;
}

.col-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.col-color-bar {
  width: 4px;
  height: 14px;
  border-radius: 2px;
}

.col-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--jira-text);
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.col-count {
  font-size: 11px;
  font-weight: 700;
  color: var(--jira-text-subtle);
  background-color: rgba(9, 30, 66, 0.08);
  padding: 1px 6px;
  border-radius: 10px;
}

.cards-container {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.column-empty {
  padding: 32px 16px;
  text-align: center;
  font-size: 12px;
  color: var(--jira-text-muted);
  border: 1px dashed var(--jira-border);
  border-radius: var(--jira-radius);
}

/* KANBAN CARD */
.kanban-card {
  background-color: var(--jira-surface);
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  padding: 10px 12px;
  box-shadow: 0 1px 2px rgba(9, 30, 66, 0.12);
  cursor: grab;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
  user-select: none;
}

.kanban-card:hover {
  background-color: #ffffff;
  box-shadow: 0 3px 6px rgba(9, 30, 66, 0.18);
  border-color: #c1c7d0;
}

.kanban-card:active {
  cursor: grabbing;
}

.kanban-card.dragging {
  opacity: 0.4;
  transform: scale(0.98);
}

.kanban-card.completed .card-title {
  text-decoration: line-through;
  color: var(--jira-text-muted);
}

.kanban-card.overdue {
  border-left: 3px solid #de350b;
}

.card-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-key-group {
  display: flex;
  align-items: center;
  gap: 5px;
}

.task-icon-sm {
  width: 14px;
  height: 14px;
  color: #4bade8;
}

.card-key {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--jira-primary);
}

.card-due-date {
  font-size: 11px;
  color: var(--jira-text-subtle);
  display: flex;
  align-items: center;
  gap: 3px;
}

.card-due-date.due-alert {
  color: #de350b;
  font-weight: 700;
  background-color: #ffebe6;
  padding: 1px 5px;
  border-radius: 2px;
}

.clock-icon {
  font-size: 10px;
}

.card-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--jira-text);
  margin: 0;
  line-height: 1.4;
  word-break: break-word;
}

.card-labels {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.label-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 2px;
  background-color: rgba(9, 30, 66, 0.08);
  color: var(--jira-text-subtle);
}

.label-badge.label-frontend { background-color: #deebff; color: #0747a6; }
.label-badge.label-backend { background-color: #eae6ff; color: #403294; }
.label-badge.label-bug { background-color: #ffebe6; color: #bf2600; }
.label-badge.label-feature { background-color: #e3fcef; color: #006644; }
.label-badge.label-ui\/ux { background-color: #fff0b3; color: #172b4d; }
.label-badge.label-devops { background-color: #e6fcff; color: #008da6; }

.card-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 4px;
  border-top: 1px solid var(--jira-border-subtle);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.prio-glyph {
  display: flex;
  align-items: center;
  width: 14px;
  height: 14px;
}

.prio-glyph.high { color: var(--jira-prio-high); }
.prio-glyph.low { color: var(--jira-prio-low); }

.card-meta-item {
  font-size: 11px;
  color: var(--jira-text-muted);
}

.user-avatar-mini {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.unassigned-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px dashed var(--jira-border);
  color: var(--jira-text-muted);
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 900px) {
  .kanban-board {
    grid-template-columns: 1fr;
  }
}
</style>
