<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import {
  useTicketStore,
  type TicketStatus,
  type TicketPriority,
  type IssueLinkType,
  type Ticket,
  type TicketLabel,
  TEAM_MEMBERS,
  ALL_LABELS
} from '../stores/ticketStore'

const ticketStore = useTicketStore()

// États d'édition
const isEditingTitle = ref(false)
const editedTitle = ref('')
const isEditingDescription = ref(false)
const editedDescription = ref('')

// Onglet d'activité actif ('comments' | 'history')
const activeTab = ref<'comments' | 'history'>('comments')

// Nouveau commentaire
const newCommentText = ref('')
const isCommentInputFocused = ref(false)

// Boîte de liaison de demande
const isLinkModalOpen = ref(false)
const selectedLinkType = ref<IssueLinkType>('relates_to')
const selectedTargetId = ref<number | ''>('')

watch(
  () => ticketStore.selectedTicket,
  (ticket) => {
    if (ticket) {
      editedTitle.value = ticket.title
      editedDescription.value = ticket.description || ''
      isEditingTitle.value = false
      isEditingDescription.value = false
      newCommentText.value = ''
      isCommentInputFocused.value = false
      isLinkModalOpen.value = false
      selectedTargetId.value = ''
    }
  },
  { immediate: true }
)

function saveTitle() {
  const trimmed = editedTitle.value.trim()
  if (ticketStore.selectedTicket && trimmed) {
    ticketStore.updateTicket(ticketStore.selectedTicket.id, { title: trimmed })
  }
  isEditingTitle.value = false
}

function saveDescription() {
  if (ticketStore.selectedTicket) {
    ticketStore.updateTicket(ticketStore.selectedTicket.id, {
      description: editedDescription.value.trim()
    })
  }
  isEditingDescription.value = false
}

function handleAddComment() {
  if (!ticketStore.selectedTicket || !newCommentText.value.trim()) return
  ticketStore.addComment(ticketStore.selectedTicket.id, newCommentText.value)
  newCommentText.value = ''
  isCommentInputFocused.value = false
}

function handleDeleteComment(commentId: number) {
  if (!ticketStore.selectedTicket) return
  ticketStore.deleteComment(ticketStore.selectedTicket.id, commentId)
}

// Tickets éligibles pour un lien (tous sauf le ticket courant et ceux déjà liés)
const availableTicketsToLink = computed<Ticket[]>(() => {
  if (!ticketStore.selectedTicket) return []
  const currentId = ticketStore.selectedTicket.id
  const alreadyLinkedIds = (ticketStore.selectedTicket.links || []).map(l => l.targetTicketId)
  return ticketStore.tickets.filter(t => t.id !== currentId && !alreadyLinkedIds.includes(t.id))
})

function handleAddLink() {
  if (!ticketStore.selectedTicket || selectedTargetId.value === '') return
  ticketStore.addIssueLink(
    ticketStore.selectedTicket.id,
    Number(selectedTargetId.value),
    selectedLinkType.value
  )
  isLinkModalOpen.value = false
  selectedTargetId.value = ''
}

function handleRemoveLink(targetId: number) {
  if (!ticketStore.selectedTicket) return
  ticketStore.removeIssueLink(ticketStore.selectedTicket.id, targetId)
}

function getLinkedTicket(targetId: number): Ticket | undefined {
  return ticketStore.getTicketById(targetId)
}

const linkTypeLabels: Record<IssueLinkType, string> = {
  blocks: 'bloque',
  is_blocked_by: 'est bloqué par',
  relates_to: 'est lié à'
}

const lozengeConfig: Record<TicketStatus, { label: string; class: string }> = {
  TODO: { label: 'À FAIRE', class: 'lozenge-todo' },
  IN_PROGRESS: { label: 'EN COURS', class: 'lozenge-progress' },
  DONE: { label: 'TERMINÉ', class: 'lozenge-done' }
}

function formatDate(timestamp?: number): string {
  if (!timestamp) return 'Non renseignée'
  return new Intl.DateTimeFormat('fr-FR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(timestamp))
}

function formatRelativeTime(timestamp: number): string {
  const diffMs = Date.now() - timestamp
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMins / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffMins < 1) return "À l'instant"
  if (diffMins < 60) return `Il y a ${diffMins} min`
  if (diffHours < 24) return `Il y a ${diffHours} h`
  return `Il y a ${diffDays} j`
}

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    if (isLinkModalOpen.value) {
      isLinkModalOpen.value = false
    } else {
      ticketStore.selectTicket(null)
    }
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<template>
  <div v-if="ticketStore.selectedTicket" class="jira-issue-view">
    <!-- Barre d'outils supérieure de l'issue -->
    <div class="issue-nav-bar">
      <div class="nav-left">
        <button
          type="button"
          class="jira-back-btn"
          @click="ticketStore.selectTicket(null)"
          title="Retourner au tableau (Échap)"
        >
          ← Revenir au tableau
        </button>
        <span class="issue-key-badge">
          <svg class="task-icon" viewBox="0 0 16 16" fill="currentColor">
            <path d="M2 3a1 1 0 011-1h10a1 1 0 011 1v10a1 1 0 01-1 1H3a1 1 0 01-1-1V3zm10 2.5a.75.75 0 00-1.06-1.06L6.5 8.88 5.06 7.44a.75.75 0 00-1.06 1.06l2 2a.75.75 0 001.06 0l4.94-5z" />
          </svg>
          KAN-{{ ticketStore.selectedTicket.id }}
        </span>
      </div>

      <div class="nav-right">
        <button
          type="button"
          class="jira-delete-action"
          @click="ticketStore.deleteTicket(ticketStore.selectedTicket.id)"
          title="Supprimer la demande"
        >
          Supprimer la demande
        </button>
      </div>
    </div>

    <!-- Layout 2 colonnes type Jira Cloud -->
    <div class="jira-issue-layout">
      <!-- Colonne principale (Gauche) -->
      <div class="main-column">
        <!-- Résumé (Titre éditable) -->
        <div class="summary-block">
          <div v-if="isEditingTitle" class="edit-summary-form">
            <input
              v-model="editedTitle"
              type="text"
              class="summary-input"
              autofocus
              @keydown.enter="saveTitle"
              @keydown.esc="isEditingTitle = false"
            />
            <div class="action-buttons">
              <button type="button" class="btn-check" @click="saveTitle">✓</button>
              <button type="button" class="btn-close" @click="isEditingTitle = false">✕</button>
            </div>
          </div>

          <h1
            v-else
            class="summary-heading"
            :class="{ strike: ticketStore.selectedTicket.status === 'DONE' }"
            @click="isEditingTitle = true"
            title="Cliquez pour modifier le résumé"
          >
            {{ ticketStore.selectedTicket.title }}
          </h1>
        </div>

        <!-- Boutons d'actions rapides Jira -->
        <div class="jira-quick-actions">
          <button
            type="button"
            class="quick-btn"
            @click="ticketStore.toggleStatus(ticketStore.selectedTicket.id)"
          >
            ↻ Faire avancer l'état
          </button>

          <button
            type="button"
            class="quick-btn"
            :class="{ active: isLinkModalOpen }"
            @click="isLinkModalOpen = !isLinkModalOpen"
          >
            🔗 Lier une issue
          </button>
        </div>

        <!-- Section Description -->
        <div class="description-block">
          <h3 class="section-title">Description</h3>

          <div v-if="isEditingDescription" class="desc-editor">
            <textarea
              v-model="editedDescription"
              class="jira-textarea"
              rows="6"
              placeholder="Ajoutez des détails, critères d'acceptation, etc..."
            ></textarea>
            <div class="desc-actions">
              <button type="button" class="btn-primary-sm" @click="saveDescription">
                Enregistrer
              </button>
              <button type="button" class="btn-subtle-sm" @click="isEditingDescription = false">
                Annuler
              </button>
            </div>
          </div>

          <div
            v-else
            class="desc-display"
            @click="isEditingDescription = true"
            title="Cliquez pour modifier la description"
          >
            <p v-if="ticketStore.selectedTicket.description" class="desc-content">
              {{ ticketStore.selectedTicket.description }}
            </p>
            <p v-else class="desc-placeholder">
              Ajouter une description...
            </p>
          </div>
        </div>

        <!-- Section Demandes liées (Linked Issues) -->
        <div class="linked-issues-block">
          <div class="section-header-row">
            <h3 class="section-title">
              Demandes associées
              <span class="count-badge">({{ ticketStore.selectedTicket.links?.length || 0 }})</span>
            </h3>
            <button
              v-if="!isLinkModalOpen"
              type="button"
              class="link-action-text"
              @click="isLinkModalOpen = true"
            >
              + Associer
            </button>
          </div>

          <!-- Formulaire inline pour créer un lien -->
          <div v-if="isLinkModalOpen" class="link-form-card">
            <h4 class="link-form-title">Associer une demande</h4>
            <div class="link-fields-grid">
              <div class="field-item">
                <label class="field-label">Cette demande :</label>
                <select v-model="selectedLinkType" class="jira-select-sm">
                  <option value="relates_to">est liée à</option>
                  <option value="blocks">bloque</option>
                  <option value="is_blocked_by">est bloquée par</option>
                </select>
              </div>

              <div class="field-item">
                <label class="field-label">Demande cible :</label>
                <select
                  v-model="selectedTargetId"
                  class="jira-select-sm"
                  :disabled="availableTicketsToLink.length === 0"
                >
                  <option value="" disabled>Sélectionner une issue...</option>
                  <option
                    v-for="target in availableTicketsToLink"
                    :key="target.id"
                    :value="target.id"
                  >
                    KAN-{{ target.id }} - {{ target.title }}
                  </option>
                </select>
              </div>
            </div>

            <p v-if="availableTicketsToLink.length === 0" class="no-tickets-hint">
              Toutes les autres demandes sont déjà associées à ce ticket.
            </p>

            <div class="link-form-actions">
              <button
                type="button"
                class="btn-primary-sm"
                :disabled="selectedTargetId === ''"
                @click="handleAddLink"
              >
                Lier
              </button>
              <button
                type="button"
                class="btn-subtle-sm"
                @click="isLinkModalOpen = false"
              >
                Annuler
              </button>
            </div>
          </div>

          <!-- Liste des demandes liées -->
          <div v-if="ticketStore.selectedTicket.links?.length" class="links-list">
            <div
              v-for="link in ticketStore.selectedTicket.links"
              :key="link.id"
              class="link-row"
            >
              <div class="link-relation">
                <span class="relation-tag" :class="link.type">
                  {{ linkTypeLabels[link.type] }}
                </span>
              </div>

              <div
                class="link-target"
                @click="ticketStore.selectTicket(link.targetTicketId)"
                title="Accéder directement à cette demande"
              >
                <svg class="task-icon-sm" viewBox="0 0 16 16" fill="currentColor">
                  <path d="M2 3a1 1 0 011-1h10a1 1 0 011 1v10a1 1 0 01-1 1H3a1 1 0 01-1-1V3zm10 2.5a.75.75 0 00-1.06-1.06L6.5 8.88 5.06 7.44a.75.75 0 00-1.06 1.06l2 2a.75.75 0 001.06 0l4.94-5z" />
                </svg>
                <span class="linked-key">KAN-{{ link.targetTicketId }}</span>
                <span class="linked-summary">
                  {{ getLinkedTicket(link.targetTicketId)?.title || 'Demande introuvable' }}
                </span>
              </div>

              <div class="link-meta">
                <span
                  v-if="getLinkedTicket(link.targetTicketId)"
                  class="jira-lozenge-sm"
                  :class="lozengeConfig[getLinkedTicket(link.targetTicketId)!.status].class"
                >
                  {{ lozengeConfig[getLinkedTicket(link.targetTicketId)!.status].label }}
                </span>

                <button
                  type="button"
                  class="remove-link-btn"
                  @click="handleRemoveLink(link.targetTicketId)"
                  title="Délier cette issue"
                >
                  ✕
                </button>
              </div>
            </div>
          </div>
          <div v-else-if="!isLinkModalOpen" class="empty-links-hint">
            Aucune demande associée pour le moment.
          </div>
        </div>

        <!-- Section Activité (Commentaires & Historique) -->
        <div class="activity-block">
          <div class="activity-header">
            <h3 class="section-title">Activité</h3>
            <div class="activity-tabs">
              <button
                type="button"
                class="act-tab-btn"
                :class="{ active: activeTab === 'comments' }"
                @click="activeTab = 'comments'"
              >
                Commentaires ({{ ticketStore.selectedTicket.comments?.length || 0 }})
              </button>
              <button
                type="button"
                class="act-tab-btn"
                :class="{ active: activeTab === 'history' }"
                @click="activeTab = 'history'"
              >
                Historique ({{ ticketStore.selectedTicket.history?.length || 0 }})
              </button>
            </div>
          </div>

          <!-- ONGLET 1 : COMMENTAIRES -->
          <div v-if="activeTab === 'comments'" class="tab-pane-comments">
            <!-- Boîte de saisie d'un nouveau commentaire -->
            <div class="new-comment-box">
              <div class="user-bubble">JD</div>
              <div class="comment-input-container" :class="{ focused: isCommentInputFocused }">
                <textarea
                  v-model="newCommentText"
                  class="comment-textarea"
                  rows="2"
                  placeholder="Ajouter un commentaire..."
                  @focus="isCommentInputFocused = true"
                ></textarea>

                <div v-if="isCommentInputFocused || newCommentText.trim()" class="comment-form-actions">
                  <button
                    type="button"
                    class="btn-primary-sm"
                    :disabled="!newCommentText.trim()"
                    @click="handleAddComment"
                  >
                    Enregistrer
                  </button>
                  <button
                    type="button"
                    class="btn-subtle-sm"
                    @click="isCommentInputFocused = false; newCommentText = ''"
                  >
                    Annuler
                  </button>
                </div>
              </div>
            </div>

            <!-- Liste des commentaires existants -->
            <div
              v-if="ticketStore.selectedTicket.comments?.length"
              class="comments-stream"
            >
              <div
                v-for="comment in ticketStore.selectedTicket.comments"
                :key="comment.id"
                class="comment-item"
              >
                <div class="user-bubble">{{ comment.author.slice(0, 2).toUpperCase() }}</div>
                <div class="comment-content-block">
                  <div class="comment-meta-row">
                    <span class="comment-author">{{ comment.author }}</span>
                    <span class="comment-time">{{ formatRelativeTime(comment.createdAt) }}</span>
                    <button
                      type="button"
                      class="delete-comment-btn"
                      @click="handleDeleteComment(comment.id)"
                      title="Supprimer ce commentaire"
                    >
                      Supprimer
                    </button>
                  </div>
                  <p class="comment-body">{{ comment.content }}</p>
                </div>
              </div>
            </div>
            <p v-else class="empty-comments-hint">
              Soyez le premier à commenter cette demande.
            </p>
          </div>

          <!-- ONGLET 2 : HISTORIQUE D'AUDIT -->
          <div v-else class="tab-pane-history">
            <div
              v-if="ticketStore.selectedTicket.history?.length"
              class="history-timeline"
            >
              <div
                v-for="event in ticketStore.selectedTicket.history"
                :key="event.id"
                class="history-item"
              >
                <div class="timeline-dot"></div>
                <div class="history-content">
                  <div class="history-header-line">
                    <span class="history-author">{{ event.author }}</span>
                    <span class="history-action">{{ event.action }}</span>
                    <span class="history-time">{{ formatRelativeTime(event.timestamp) }}</span>
                  </div>
                  <div v-if="event.details" class="history-details-badge">
                    {{ event.details }}
                  </div>
                </div>
              </div>
            </div>
            <p v-else class="empty-history-hint">
              Aucun événement dans l'historique pour le moment.
            </p>
          </div>
        </div>
      </div>

      <!-- Colonne latérale de détails (Droite) -->
      <aside class="side-column">
        <!-- État / Status Lozenge Jira -->
        <div class="sidebar-box">
          <div class="box-label">État</div>
          <div class="status-select-wrap">
            <select
              :value="ticketStore.selectedTicket.status"
              class="jira-status-dropdown"
              :class="`status-${ticketStore.selectedTicket.status.toLowerCase()}`"
              @change="ticketStore.setStatus(ticketStore.selectedTicket.id, ($event.target as HTMLSelectElement).value as TicketStatus)"
            >
              <option value="TODO">À FAIRE</option>
              <option value="IN_PROGRESS">EN COURS</option>
              <option value="DONE">TERMINÉ</option>
            </select>
          </div>
        </div>

        <!-- Panneau Détails Jira -->
        <div class="details-panel">
          <div class="panel-heading">Détails</div>

          <div class="detail-row">
            <span class="row-label">Responsable</span>
            <div class="row-value">
              <select
                :value="ticketStore.selectedTicket.assigneeId || ''"
                class="jira-inline-select"
                @change="ticketStore.setAssignee(ticketStore.selectedTicket.id, ($event.target as HTMLSelectElement).value || undefined)"
              >
                <option value="">Non assigné</option>
                <option
                  v-for="member in TEAM_MEMBERS"
                  :key="member.id"
                  :value="member.id"
                >
                  {{ member.name }}
                </option>
              </select>
            </div>
          </div>

          <div class="detail-row">
            <span class="row-label">Rapporteur</span>
            <div class="row-value">
              <span class="avatar-sm lead">MT</span>
              <span>Chef de projet</span>
            </div>
          </div>

          <div class="detail-row">
            <span class="row-label">Date d'échéance</span>
            <div class="row-value">
              <input
                type="date"
                :value="ticketStore.selectedTicket.dueDate || ''"
                class="jira-date-input"
                :class="{ 'overdue-input': ticketStore.isOverdue(ticketStore.selectedTicket) }"
                @change="ticketStore.setDueDate(ticketStore.selectedTicket.id, ($event.target as HTMLInputElement).value || undefined)"
              />
              <span
                v-if="ticketStore.isOverdue(ticketStore.selectedTicket)"
                class="overdue-tag"
              >
                En retard
              </span>
            </div>
          </div>

          <div class="detail-row">
            <span class="row-label">Étiquettes (Labels)</span>
            <div class="labels-editor">
              <button
                v-for="label in ALL_LABELS"
                :key="label"
                type="button"
                class="tag-toggle-btn"
                :class="{ active: ticketStore.selectedTicket.labels?.includes(label) }"
                @click="ticketStore.toggleLabel(ticketStore.selectedTicket.id, label)"
              >
                {{ label }}
              </button>
            </div>
          </div>

          <div class="detail-row">
            <span class="row-label">Priorité</span>
            <div class="row-value">
              <select
                :value="ticketStore.selectedTicket.priority"
                class="jira-inline-select"
                @change="ticketStore.setPriority(ticketStore.selectedTicket.id, ($event.target as HTMLSelectElement).value as TicketPriority)"
              >
                <option value="LOW">Basse (Low)</option>
                <option value="HIGH">Haute (High)</option>
              </select>
            </div>
          </div>

          <div class="detail-row">
            <span class="row-label">Créé</span>
            <div class="row-value text-muted">
              {{ formatDate(ticketStore.selectedTicket.createdAt) }}
            </div>
          </div>

          <div class="detail-row">
            <span class="row-label">Dernière activité</span>
            <div class="row-value text-muted">
              {{ ticketStore.selectedTicket.history?.[0] ? formatRelativeTime(ticketStore.selectedTicket.history[0].timestamp) : 'À la création' }}
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.jira-issue-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: var(--jira-surface);
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  padding: 24px;
  box-shadow: 0 1px 3px rgba(9, 30, 66, 0.08);
}

/* Nav */
.issue-nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--jira-border-subtle);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.jira-back-btn {
  background: var(--jira-bg);
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  padding: 5px 10px;
  font-size: 13px;
  font-weight: 500;
  color: var(--jira-text);
  cursor: pointer;
}

.jira-back-btn:hover {
  background-color: var(--jira-surface-hover);
}

.issue-key-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-weight: 700;
  color: var(--jira-primary);
  font-size: 13px;
}

.task-icon {
  width: 14px;
  height: 14px;
  color: #4bade8;
}

.jira-delete-action {
  background: transparent;
  color: var(--jira-text-subtle);
  border: none;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--jira-radius);
}

.jira-delete-action:hover {
  color: #de350b;
  background-color: #ffebe6;
}

/* 2 Columns Layout */
.jira-issue-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 32px;
}

/* Main Column */
.main-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.summary-heading {
  font-size: 22px;
  font-weight: 600;
  color: var(--jira-text);
  margin: 0;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: var(--jira-radius);
  border: 1px solid transparent;
}

.summary-heading:hover {
  background-color: var(--jira-surface-hover);
}

.summary-heading.strike {
  text-decoration: line-through;
  color: var(--jira-text-muted);
}

.edit-summary-form {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-input {
  flex: 1;
  font-size: 18px;
  font-weight: 600;
  padding: 6px 10px;
  border: 2px solid var(--jira-border-focus);
  border-radius: var(--jira-radius);
  outline: none;
}

.btn-check,
.btn-close {
  padding: 6px 10px;
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  background: var(--jira-bg);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
}

.btn-check {
  color: #00875a;
}

.btn-close {
  color: #de350b;
}

.jira-quick-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.quick-btn {
  background-color: var(--jira-bg);
  color: var(--jira-text);
  font-size: 13px;
  font-weight: 500;
  padding: 5px 10px;
  border-radius: var(--jira-radius);
  border: 1px solid var(--jira-border);
  cursor: pointer;
  transition: all 0.15s ease;
}

.quick-btn:hover {
  background-color: var(--jira-surface-hover);
}

.quick-btn.active {
  background-color: #deebff;
  color: #0052cc;
  border-color: #b3d4ff;
}

/* Description */
.description-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--jira-text);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.desc-display {
  min-height: 70px;
  padding: 10px 12px;
  border-radius: var(--jira-radius);
  cursor: pointer;
  border: 1px solid transparent;
  transition: background-color 0.15s ease;
}

.desc-display:hover {
  background-color: var(--jira-bg);
}

.desc-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--jira-text);
  white-space: pre-wrap;
}

.desc-placeholder {
  color: var(--jira-text-subtle);
  font-size: 14px;
}

.jira-textarea {
  width: 100%;
  padding: 8px 10px;
  font-size: 14px;
  color: var(--jira-text);
  border: 2px solid var(--jira-border-focus);
  border-radius: var(--jira-radius);
  outline: none;
  font-family: inherit;
}

.desc-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.btn-primary-sm {
  background-color: var(--jira-primary);
  color: #ffffff;
  border: none;
  border-radius: var(--jira-radius);
  padding: 5px 12px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.btn-primary-sm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-subtle-sm {
  background: transparent;
  color: var(--jira-text-subtle);
  border: none;
  padding: 5px 10px;
  font-size: 13px;
  cursor: pointer;
}

/* LINKED ISSUES */
.linked-issues-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--jira-border-subtle);
}

.section-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.count-badge {
  font-size: 12px;
  color: var(--jira-text-subtle);
  font-weight: normal;
}

.link-action-text {
  background: none;
  border: none;
  color: var(--jira-primary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.link-action-text:hover {
  text-decoration: underline;
}

.link-form-card {
  background-color: var(--jira-bg);
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.link-form-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--jira-text);
  margin: 0;
}

.link-fields-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--jira-text-subtle);
}

.jira-select-sm {
  padding: 5px 8px;
  font-size: 13px;
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  background-color: #ffffff;
  color: var(--jira-text);
  outline: none;
}

.no-tickets-hint {
  font-size: 12px;
  color: #de350b;
  margin: 0;
}

.link-form-actions {
  display: flex;
  gap: 8px;
}

.links-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.link-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background-color: #fafbfc;
  border: 1px solid var(--jira-border-subtle);
  border-radius: var(--jira-radius);
  font-size: 13px;
  gap: 12px;
}

.link-row:hover {
  background-color: var(--jira-bg);
}

.link-relation {
  width: 110px;
  flex-shrink: 0;
}

.relation-tag {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  padding: 2px 6px;
  border-radius: 2px;
  background-color: rgba(9, 30, 66, 0.08);
  color: var(--jira-text-subtle);
}

.relation-tag.blocks {
  background-color: #ffebe6;
  color: #de350b;
}

.relation-tag.is_blocked_by {
  background-color: #fff0b3;
  color: #172b4d;
}

.link-target {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  min-width: 0;
}

.task-icon-sm {
  width: 14px;
  height: 14px;
  color: #4bade8;
  flex-shrink: 0;
}

.linked-key {
  font-family: var(--font-mono);
  font-weight: 600;
  color: var(--jira-primary);
  flex-shrink: 0;
}

.linked-key:hover {
  text-decoration: underline;
}

.linked-summary {
  color: var(--jira-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.link-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.jira-lozenge-sm {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: var(--jira-radius);
  text-transform: uppercase;
}

.remove-link-btn {
  background: transparent;
  border: none;
  color: var(--jira-text-muted);
  font-size: 13px;
  cursor: pointer;
  padding: 2px 6px;
}

.remove-link-btn:hover {
  color: #de350b;
}

.empty-links-hint {
  font-size: 13px;
  color: var(--jira-text-subtle);
  font-style: italic;
  padding: 4px 0;
}

/* ACTIVITÉ */
.activity-block {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--jira-border-subtle);
}

.activity-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.activity-tabs {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid var(--jira-border-subtle);
}

.act-tab-btn {
  background: transparent;
  border: none;
  font-size: 13px;
  font-weight: 500;
  color: var(--jira-text-subtle);
  padding: 6px 12px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.15s ease;
}

.act-tab-btn.active {
  color: var(--jira-primary);
  font-weight: 600;
  border-bottom-color: var(--jira-primary);
}

/* COMMENTAIRES PANE */
.tab-pane-comments {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.new-comment-box {
  display: flex;
  gap: 12px;
}

.user-bubble {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background-color: #00875a;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.comment-input-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-textarea {
  width: 100%;
  padding: 8px 12px;
  font-size: 13px;
  background-color: var(--jira-bg);
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  outline: none;
  font-family: inherit;
  resize: vertical;
  transition: all 0.15s ease;
}

.comment-textarea:focus {
  background-color: #ffffff;
  border-color: var(--jira-border-focus);
  box-shadow: 0 0 0 1px var(--jira-border-focus);
}

.comment-form-actions {
  display: flex;
  gap: 8px;
}

.comments-stream {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-content-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.comment-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.comment-author {
  font-weight: 600;
  color: var(--jira-text);
}

.comment-time {
  color: var(--jira-text-muted);
}

.delete-comment-btn {
  background: none;
  border: none;
  color: var(--jira-text-subtle);
  cursor: pointer;
  padding: 0;
  font-size: 11px;
  margin-left: auto;
}

.delete-comment-btn:hover {
  color: #de350b;
  text-decoration: underline;
}

.comment-body {
  font-size: 13px;
  color: var(--jira-text);
  line-height: 1.5;
  margin: 0;
  background-color: #fafbfc;
  border: 1px solid var(--jira-border-subtle);
  border-radius: var(--jira-radius);
  padding: 8px 12px;
}

.empty-comments-hint,
.empty-history-hint {
  font-size: 13px;
  color: var(--jira-text-subtle);
  font-style: italic;
  padding: 8px 0;
}

/* HISTORIQUE PANE */
.tab-pane-history {
  display: flex;
  flex-direction: column;
}

.history-timeline {
  display: flex;
  flex-direction: column;
  position: relative;
  padding-left: 18px;
  border-left: 2px solid var(--jira-border);
  gap: 16px;
  margin-left: 8px;
}

.history-item {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.timeline-dot {
  position: absolute;
  left: -24px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: var(--jira-primary);
  border: 2px solid #ffffff;
}

.history-header-line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.history-author {
  font-weight: 600;
  color: var(--jira-text);
}

.history-action {
  color: var(--jira-text-subtle);
}

.history-time {
  font-size: 11px;
  color: var(--jira-text-muted);
  margin-left: auto;
}

.history-details-badge {
  display: inline-block;
  align-self: flex-start;
  font-size: 12px;
  font-weight: 500;
  color: var(--jira-text);
  background-color: var(--jira-bg);
  border: 1px solid var(--jira-border);
  border-radius: 2px;
  padding: 2px 6px;
  margin-top: 2px;
}

/* SIDEBAR COLUMN */
.side-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.box-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--jira-text-subtle);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.jira-status-dropdown {
  width: 100%;
  font-family: var(--font-jira);
  font-size: 12px;
  font-weight: 700;
  padding: 6px 10px;
  border-radius: var(--jira-radius);
  border: 1px solid var(--jira-border);
  cursor: pointer;
  outline: none;
  text-transform: uppercase;
}

.jira-status-dropdown.status-todo {
  background-color: var(--lozenge-todo-bg);
  color: var(--lozenge-todo-text);
}

.jira-status-dropdown.status-in_progress {
  background-color: var(--lozenge-prog-bg);
  color: var(--lozenge-prog-text);
}

.jira-status-dropdown.status-done {
  background-color: var(--lozenge-done-bg);
  color: var(--lozenge-done-text);
}

.details-panel {
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-heading {
  font-size: 13px;
  font-weight: 700;
  color: var(--jira-text);
  border-bottom: 1px solid var(--jira-border-subtle);
  padding-bottom: 8px;
}

.detail-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.row-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--jira-text-subtle);
}

.row-value {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--jira-text);
}

.avatar-sm {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: #00875a;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-sm.lead {
  background-color: #ffab00;
  color: #172b4d;
}

.jira-inline-select {
  padding: 4px 6px;
  font-size: 13px;
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  background-color: var(--jira-bg);
  color: var(--jira-text);
  cursor: pointer;
  width: 100%;
}

.jira-date-input {
  padding: 4px 6px;
  font-size: 12px;
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  background-color: var(--jira-bg);
  color: var(--jira-text);
  cursor: pointer;
}

.jira-date-input.overdue-input {
  border-color: #de350b;
  background-color: #ffebe6;
  color: #de350b;
}

.overdue-tag {
  font-size: 11px;
  font-weight: 700;
  color: #de350b;
  background-color: #ffebe6;
  padding: 2px 6px;
  border-radius: 2px;
  margin-left: 6px;
}

.labels-editor {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-toggle-btn {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 7px;
  border-radius: 2px;
  border: 1px solid var(--jira-border);
  background-color: var(--jira-bg);
  color: var(--jira-text-subtle);
  cursor: pointer;
  transition: all 0.12s ease;
}

.tag-toggle-btn:hover {
  background-color: var(--jira-surface-hover);
  color: var(--jira-text);
}

.tag-toggle-btn.active {
  background-color: #deebff;
  color: #0052cc;
  border-color: #4c9aff;
  font-weight: 600;
}

.text-muted {
  color: var(--jira-text-subtle);
  font-size: 12px;
}

@media (max-width: 900px) {
  .jira-issue-layout {
    grid-template-columns: 1fr;
  }
}
</style>
