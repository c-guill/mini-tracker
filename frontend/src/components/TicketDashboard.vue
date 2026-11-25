<script setup lang="ts">
import {
  useTicketStore,
  type TicketStatus,
  type SortField,
  type TicketLabel,
  TEAM_MEMBERS,
  ALL_LABELS
} from '../stores/ticketStore'
import TicketKanban from './TicketKanban.vue'

const emit = defineEmits<{
  (e: 'open-create'): void
}>()

const ticketStore = useTicketStore()

const statusFilters: { value: TicketStatus | 'ALL'; label: string; countKey: 'total' | 'todo' | 'inProgress' | 'done' }[] = [
  { value: 'ALL', label: 'Toutes les demandes', countKey: 'total' },
  { value: 'TODO', label: 'À faire', countKey: 'todo' },
  { value: 'IN_PROGRESS', label: 'En cours', countKey: 'inProgress' },
  { value: 'DONE', label: 'Terminées', countKey: 'done' }
]

const sortOptions: { value: SortField; label: string }[] = [
  { value: 'id', label: 'Clé de demande' },
  { value: 'priority', label: 'Priorité' },
  { value: 'title', label: 'Résumé' },
  { value: 'status', label: 'État' },
  { value: 'dueDate', label: 'Date d\'échéance' }
]

const lozengeConfig: Record<TicketStatus, { label: string; class: string }> = {
  TODO: { label: 'À FAIRE', class: 'lozenge-todo' },
  IN_PROGRESS: { label: 'EN COURS', class: 'lozenge-progress' },
  DONE: { label: 'TERMINÉ', class: 'lozenge-done' }
}

function handleResetData() {
  if (confirm('Voulez-vous réinitialiser toutes les données aux valeurs de démonstration ?')) {
    ticketStore.resetToDemoData()
  }
}
</script>

<template>
  <div class="jira-board-container">
    <!-- En-tête du tableau Jira -->
    <div class="board-header">
      <div class="board-title-group">
        <h1 class="board-title">Demandes du tableau KAN</h1>
        <span class="board-subtitle">Gestion agile, suivi du sprint et collaboration</span>
      </div>

      <div class="header-right-actions">
        <!-- Switch de Vue (Kanban / Liste) -->
        <div class="view-switch-control" role="group" aria-label="Mode d'affichage">
          <button
            type="button"
            class="switch-btn"
            :class="{ active: ticketStore.viewMode === 'kanban' }"
            @click="ticketStore.viewMode = 'kanban'"
            title="Vue Tableau Kanban (Drag & Drop)"
          >
            <svg class="switch-icon" viewBox="0 0 16 16" fill="currentColor">
              <path d="M2 3a1 1 0 011-1h2a1 1 0 011 1v10a1 1 0 01-1 1H3a1 1 0 01-1-1V3zm5 0a1 1 0 011-1h2a1 1 0 011 1v10a1 1 0 01-1 1H8a1 1 0 01-1-1V3zm5 0a1 1 0 011-1h2a1 1 0 011 1v10a1 1 0 01-1 1h-2a1 1 0 01-1-1V3z" />
            </svg>
            <span>Tableau</span>
          </button>

          <button
            type="button"
            class="switch-btn"
            :class="{ active: ticketStore.viewMode === 'list' }"
            @click="ticketStore.viewMode = 'list'"
            title="Vue Liste détaillée"
          >
            <svg class="switch-icon" viewBox="0 0 16 16" fill="currentColor">
              <path d="M2.5 4a.75.75 0 000 1.5h11a.75.75 0 000-1.5h-11zm0 4a.75.75 0 000 1.5h11a.75.75 0 000-1.5h-11zm0 4a.75.75 0 000 1.5h11a.75.75 0 000-1.5h-11z" />
            </svg>
            <span>Liste</span>
          </button>
        </div>

        <button
          type="button"
          class="jira-action-btn-primary"
          @click="emit('open-create')"
        >
          <span>+ Créer une demande</span>
        </button>
      </div>
    </div>

    <!-- Barre d'outils Jira & Filtres complets -->
    <div class="jira-toolbar">
      <!-- Recherche textuelle -->
      <div class="search-wrapper">
        <svg class="search-icon" viewBox="0 0 16 16" fill="currentColor">
          <path d="M11.742 10.344a6.5 6.5 0 10-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 001.415-1.414l-3.85-3.85a1.007 1.007 0 00-.115-.1zM12 6.5a5.5 5.5 0 11-11 0 5.5 5.5 0 0111 0z" />
        </svg>
        <input
          v-model="ticketStore.searchQuery"
          type="search"
          placeholder="Rechercher (titre, KAN-X)..."
          class="jira-search-input"
        />
        <button
          v-if="ticketStore.searchQuery"
          type="button"
          class="clear-btn"
          @click="ticketStore.searchQuery = ''"
          title="Effacer"
        >
          ✕
        </button>
      </div>

      <!-- Filtre rapide Jira "Mes tickets" -->
      <button
        type="button"
        class="filter-chip special-chip"
        :class="{ active: ticketStore.filterOnlyMine }"
        @click="ticketStore.filterOnlyMine = !ticketStore.filterOnlyMine"
      >
        <span class="user-dot"></span>
        <span>Mes tickets uniquement</span>
      </button>

      <!-- Filtres d'état -->
      <div class="quick-filters">
        <button
          v-for="filter in statusFilters"
          :key="filter.value"
          type="button"
          class="filter-chip"
          :class="{ active: ticketStore.filterStatus === filter.value }"
          @click="ticketStore.filterStatus = filter.value"
        >
          <span>{{ filter.label }}</span>
          <span class="chip-count">{{ ticketStore.stats[filter.countKey] }}</span>
        </button>
      </div>

      <!-- Filtre par Assignee -->
      <div class="select-filter-group">
        <label for="filter-assignee" class="filter-mini-label">Membre :</label>
        <select
          id="filter-assignee"
          v-model="ticketStore.filterAssignee"
          class="jira-select-compact"
        >
          <option value="ALL">Tous les membres</option>
          <option
            v-for="member in TEAM_MEMBERS"
            :key="member.id"
            :value="member.id"
          >
            {{ member.name }}
          </option>
          <option value="UNASSIGNED">Non assigné</option>
        </select>
      </div>

      <!-- Filtre par Label -->
      <div class="select-filter-group">
        <label for="filter-label" class="filter-mini-label">Étiquette :</label>
        <select
          id="filter-label"
          v-model="ticketStore.filterLabel"
          class="jira-select-compact"
        >
          <option value="ALL">Toutes les étiquettes</option>
          <option
            v-for="label in ALL_LABELS"
            :key="label"
            :value="label"
          >
            {{ label }}
          </option>
        </select>
      </div>

      <!-- Tri Jira & Reset -->
      <div class="toolbar-right-side">
        <div class="sort-wrapper">
          <label for="jira-sort" class="sort-caption">Trier :</label>
          <select
            id="jira-sort"
            v-model="ticketStore.sortBy"
            class="jira-sort-select"
          >
            <option
              v-for="opt in sortOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </option>
          </select>

          <button
            type="button"
            class="jira-sort-btn"
            :title="ticketStore.sortOrder === 'asc' ? 'Trier par ordre croissant' : 'Trier par ordre décroissant'"
            @click="ticketStore.toggleSortOrder"
          >
            <span class="sort-icon">{{ ticketStore.sortOrder === 'asc' ? '↑' : '↓' }}</span>
          </button>
        </div>

        <button
          type="button"
          class="reset-demo-btn"
          @click="handleResetData"
          title="Réinitialiser les données de démo"
        >
          🔄 Réinitialiser
        </button>
      </div>
    </div>

    <!-- VUE 1 : TABLEAU KANBAN (DRAG & DROP) -->
    <TicketKanban v-if="ticketStore.viewMode === 'kanban'" />

    <!-- VUE 2 : TABLE DES ISSUES JIRA (LISTE) -->
    <div v-else class="jira-table-card">
      <div class="table-header-row">
        <span class="col-type" title="Type de demande">T</span>
        <span class="col-key">Clé</span>
        <span class="col-summary">Résumé</span>
        <span class="col-priority">P</span>
        <span class="col-status">État</span>
        <span class="col-assignee">Responsable</span>
        <span class="col-actions">Actions</span>
      </div>

      <div v-if="ticketStore.sortedTickets.length === 0" class="jira-empty-state">
        <div class="empty-icon-box">📋</div>
        <p class="empty-msg">Aucune demande trouvée avec les filtres actuels</p>
        <p class="empty-sub">
          Ajustez vos filtres ou réinitialisez les critères de recherche.
        </p>
      </div>

      <ul v-else class="issue-list">
        <li
          v-for="ticket in ticketStore.sortedTickets"
          :key="ticket.id"
          class="issue-row"
          @click="ticketStore.selectTicket(ticket.id)"
        >
          <!-- Type d'issue : Task (Carré bleu) -->
          <div class="col-type">
            <span class="type-icon-box" title="Tâche">
              <svg viewBox="0 0 16 16" fill="currentColor">
                <path d="M2 3a1 1 0 011-1h10a1 1 0 011 1v10a1 1 0 01-1 1H3a1 1 0 01-1-1V3zm10 2.5a.75.75 0 00-1.06-1.06L6.5 8.88 5.06 7.44a.75.75 0 00-1.06 1.06l2 2a.75.75 0 001.06 0l4.94-5z" />
              </svg>
            </span>
          </div>

          <!-- Clé Jira (KAN-X) -->
          <div class="col-key">
            <span class="jira-key">KAN-{{ ticket.id }}</span>
          </div>

          <!-- Résumé & badges -->
          <div class="col-summary">
            <span class="summary-text" :class="{ strike: ticket.status === 'DONE' }">
              {{ ticket.title }}
            </span>
            <div class="summary-indicators">
              <span
                v-if="ticket.dueDate"
                class="indicator-chip"
                :class="{ 'due-alert': ticketStore.isOverdue(ticket) }"
                :title="`Échéance : ${ticket.dueDate}`"
              >
                📅 {{ ticket.dueDate.slice(5) }}
              </span>
              <span v-if="ticket.links?.length" class="indicator-chip" :title="`${ticket.links.length} issue(s) liée(s)`">
                🔗 {{ ticket.links.length }}
              </span>
              <span v-if="ticket.comments?.length" class="indicator-chip" :title="`${ticket.comments.length} commentaire(s)`">
                💬 {{ ticket.comments.length }}
              </span>
            </div>
          </div>

          <!-- Priorité Jira -->
          <div class="col-priority" :title="`Priorité : ${ticket.priority === 'HIGH' ? 'Haute' : 'Basse'}`">
            <span class="prio-glyph" :class="ticket.priority.toLowerCase()">
              <svg v-if="ticket.priority === 'HIGH'" viewBox="0 0 16 16" fill="currentColor">
                <path d="M8 2l5 6H3l5-6zm0 7l5 5H3l5-5z" />
              </svg>
              <svg v-else viewBox="0 0 16 16" fill="currentColor">
                <path d="M8 14l-5-6h10l-5 6zm0-7l-5-5h10l-5 5z" />
              </svg>
            </span>
          </div>

          <!-- Lozenge de Statut Jira cliquable -->
          <div class="col-status">
            <button
              type="button"
              class="jira-lozenge"
              :class="lozengeConfig[ticket.status].class"
              @click.stop="ticketStore.toggleStatus(ticket.id)"
              title="Cliquer pour faire avancer l'état"
            >
              {{ lozengeConfig[ticket.status].label }}
            </button>
          </div>

          <!-- Responsable -->
          <div class="col-assignee">
            <div v-if="ticketStore.getUserById(ticket.assigneeId)" class="assignee-pill">
              <span
                class="avatar-tiny"
                :style="{ backgroundColor: ticketStore.getUserById(ticket.assigneeId)!.color }"
              >
                {{ ticketStore.getUserById(ticket.assigneeId)!.initials }}
              </span>
              <span class="assignee-name">{{ ticketStore.getUserById(ticket.assigneeId)!.name }}</span>
            </div>
            <span v-else class="unassigned-text">Non assigné</span>
          </div>

          <!-- Actions -->
          <div class="col-actions">
            <button
              type="button"
              class="row-delete-btn"
              @click.stop="ticketStore.deleteTicket(ticket.id)"
              title="Supprimer la demande"
            >
              <svg viewBox="0 0 16 16" fill="currentColor">
                <path d="M5.5 5.5A.5.5 0 016 6v6a.5.5 0 01-1 0V6a.5.5 0 01.5-.5zm2.5 0a.5.5 0 01.5.5v6a.5.5 0 01-1 0V6a.5.5 0 01.5-.5zm3 .5a.5.5 0 00-1 0v6a.5.5 0 001 0V6z" />
                <path fill-rule="evenodd" d="M14.5 3a1 1 0 01-1 1H13v9a2 2 0 01-2 2H5a2 2 0 01-2-2V4h-.5a1 1 0 01-1-1V2a1 1 0 011-1H6a1 1 0 011-1h2a1 1 0 011 1h3.5a1 1 0 011 1v1zM4.118 4L4 4.059V13a1 1 0 001 1h6a1 1 0 001-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z" clip-rule="evenodd" />
              </svg>
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.jira-board-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.board-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.board-title-group {
  display: flex;
  flex-direction: column;
}

.board-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--jira-text);
  margin: 0;
}

.board-subtitle {
  font-size: 13px;
  color: var(--jira-text-subtle);
}

.header-right-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Switch Vue (Kanban / Liste) */
.view-switch-control {
  display: inline-flex;
  background-color: var(--jira-bg);
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  padding: 2px;
  gap: 2px;
}

.switch-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  font-size: 13px;
  font-weight: 500;
  color: var(--jira-text-subtle);
  background: transparent;
  border: none;
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.switch-btn:hover {
  color: var(--jira-text);
}

.switch-btn.active {
  background-color: var(--jira-surface);
  color: var(--jira-primary);
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(9, 30, 66, 0.12);
}

.switch-icon {
  width: 14px;
  height: 14px;
}

.jira-action-btn-primary {
  background-color: var(--jira-primary);
  color: #ffffff;
  border: none;
  border-radius: var(--jira-radius);
  font-size: 14px;
  font-weight: 500;
  padding: 8px 14px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.jira-action-btn-primary:hover {
  background-color: var(--jira-primary-hover);
}

/* Toolbar */
.jira-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  background-color: var(--jira-surface);
  padding: 12px 16px;
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
}

.search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  width: 14px;
  height: 14px;
  color: var(--jira-text-muted);
  pointer-events: none;
}

.jira-search-input {
  padding: 6px 26px 6px 30px;
  font-size: 13px;
  width: 180px;
  color: var(--jira-text);
  background-color: var(--jira-bg);
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  outline: none;
  transition: all 0.15s ease;
}

.jira-search-input:focus {
  width: 210px;
  background-color: #ffffff;
  border-color: var(--jira-border-focus);
  box-shadow: 0 0 0 1px var(--jira-border-focus);
}

.clear-btn {
  position: absolute;
  right: 6px;
  background: none;
  border: none;
  color: var(--jira-text-muted);
  font-size: 12px;
  cursor: pointer;
}

.special-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.user-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #00875a;
}

.quick-filters {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
  color: var(--jira-text-subtle);
  background-color: var(--jira-bg);
  border: 1px solid transparent;
  border-radius: var(--jira-radius);
  cursor: pointer;
  transition: all 0.15s ease;
}

.filter-chip:hover {
  background-color: var(--jira-surface-hover);
  color: var(--jira-text);
}

.filter-chip.active {
  background-color: #deebff;
  color: #0052cc;
  font-weight: 600;
}

.chip-count {
  font-size: 11px;
  background-color: rgba(9, 30, 66, 0.08);
  padding: 1px 5px;
  border-radius: 10px;
}

.select-filter-group {
  display: flex;
  align-items: center;
  gap: 5px;
}

.filter-mini-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--jira-text-subtle);
}

.jira-select-compact {
  padding: 4px 6px;
  font-size: 12px;
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  background-color: #ffffff;
  color: var(--jira-text);
  outline: none;
  cursor: pointer;
}

.toolbar-right-side {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.sort-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sort-caption {
  font-size: 12px;
  color: var(--jira-text-subtle);
}

.jira-sort-select {
  padding: 4px 8px;
  font-size: 12px;
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  background-color: #ffffff;
  color: var(--jira-text);
  outline: none;
  cursor: pointer;
}

.jira-sort-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  background-color: var(--jira-bg);
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  color: var(--jira-text);
  cursor: pointer;
  font-weight: 700;
}

.reset-demo-btn {
  background: transparent;
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  padding: 4px 8px;
  font-size: 11px;
  color: var(--jira-text-subtle);
  cursor: pointer;
  transition: all 0.15s;
}

.reset-demo-btn:hover {
  background-color: var(--jira-surface-hover);
  color: var(--jira-text);
}

/* Table Card */
.jira-table-card {
  background-color: var(--jira-surface);
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(9, 30, 66, 0.08);
}

.table-header-row {
  display: grid;
  grid-template-columns: 32px 90px 1fr 40px 120px 130px 48px;
  align-items: center;
  padding: 10px 16px;
  background-color: #fafbfc;
  border-bottom: 1px solid var(--jira-border);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--jira-text-subtle);
  letter-spacing: 0.03em;
}

.issue-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.issue-row {
  display: grid;
  grid-template-columns: 32px 90px 1fr 40px 120px 130px 48px;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid var(--jira-border-subtle);
  cursor: pointer;
  transition: background-color 0.12s ease;
}

.issue-row:last-child {
  border-bottom: none;
}

.issue-row:hover {
  background-color: #f4f5f7;
}

.type-icon-box {
  display: flex;
  align-items: center;
  width: 16px;
  height: 16px;
  color: #4bade8;
}

.jira-key {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--jira-primary);
}

.issue-row:hover .jira-key {
  text-decoration: underline;
}

.col-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding-right: 12px;
}

.summary-text {
  font-size: 14px;
  color: var(--jira-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.summary-text.strike {
  text-decoration: line-through;
  color: var(--jira-text-muted);
}

.summary-indicators {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.indicator-chip {
  font-size: 11px;
  color: var(--jira-text-subtle);
  background-color: var(--jira-bg);
  border: 1px solid var(--jira-border-subtle);
  padding: 1px 5px;
  border-radius: var(--jira-radius);
  font-weight: 500;
}

.indicator-chip.due-alert {
  background-color: #ffebe6;
  color: #de350b;
  border-color: #ffbdad;
  font-weight: 600;
}

.prio-glyph {
  display: flex;
  align-items: center;
  width: 16px;
  height: 16px;
}

.prio-glyph.high { color: var(--jira-prio-high); }
.prio-glyph.low { color: var(--jira-prio-low); }

.jira-lozenge {
  display: inline-block;
  font-family: var(--font-jira);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 3px 8px;
  border-radius: var(--jira-radius);
  border: none;
  cursor: pointer;
  text-align: center;
  text-transform: uppercase;
  transition: opacity 0.15s ease;
}

.jira-lozenge:hover {
  opacity: 0.85;
}

.lozenge-todo {
  background-color: var(--lozenge-todo-bg);
  color: var(--lozenge-todo-text);
}

.lozenge-progress {
  background-color: var(--lozenge-prog-bg);
  color: var(--lozenge-prog-text);
}

.lozenge-done {
  background-color: var(--lozenge-done-bg);
  color: var(--lozenge-done-text);
}

.assignee-pill {
  display: flex;
  align-items: center;
  gap: 6px;
}

.avatar-tiny {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.assignee-name {
  font-size: 12px;
  color: var(--jira-text);
}

.unassigned-text {
  font-size: 12px;
  color: var(--jira-text-muted);
  font-style: italic;
}

.row-delete-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: transparent;
  border: none;
  border-radius: var(--jira-radius);
  color: var(--jira-text-muted);
  cursor: pointer;
  transition: all 0.15s ease;
}

.row-delete-btn svg {
  width: 14px;
  height: 14px;
}

.row-delete-btn:hover {
  background-color: #ffebe6;
  color: #de350b;
}

/* Empty State */
.jira-empty-state {
  padding: 48px 16px;
  text-align: center;
}

.empty-icon-box {
  font-size: 32px;
  margin-bottom: 8px;
}

.empty-msg {
  font-size: 15px;
  font-weight: 600;
  color: var(--jira-text);
  margin-bottom: 4px;
}

.empty-sub {
  font-size: 13px;
  color: var(--jira-text-subtle);
}

@media (max-width: 900px) {
  .table-header-row,
  .issue-row {
    grid-template-columns: 28px 80px 1fr 100px;
  }
  .col-priority,
  .col-assignee,
  .col-actions {
    display: none;
  }
}
</style>
