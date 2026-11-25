<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useTicketStore } from './stores/ticketStore'
import TicketForm from './components/TicketForm.vue'
import TicketDashboard from './components/TicketDashboard.vue'
import TicketDetail from './components/TicketDetail.vue'

const ticketStore = useTicketStore()
const isCreateModalOpen = ref(false)

onMounted(() => {
  ticketStore.fetchTickets()
})

function openCreateModal() {
  isCreateModalOpen.value = true
}

function closeCreateModal() {
  isCreateModalOpen.value = false
}
</script>

<template>
  <div class="jira-layout">
    <!-- Barre de navigation supérieure Atlassian Jira -->
    <header class="jira-navbar">
      <div class="navbar-left">
        <!-- Logo Jira Iconique -->
        <div
          class="jira-logo"
          @click="ticketStore.selectTicket(null)"
          title="Tableau de bord Jira"
        >
          <svg class="jira-icon" viewBox="0 0 24 24" fill="none">
            <path
              d="M11.53 2c0 5.26-4.27 9.53-9.53 9.53V2h9.53z"
              fill="#2684FF"
            />
            <path
              d="M11.53 11.53c0 5.26-4.27 9.53-9.53 9.53v-9.53h9.53z"
              fill="#FFFFFF"
            />
            <path
              d="M21.06 11.53c0 5.26-4.27 9.53-9.53 9.53v-9.53h9.53z"
              fill="#2684FF"
            />
          </svg>
          <span class="jira-brand-name">Jira Software</span>
        </div>

        <nav class="nav-links">
          <button
            type="button"
            class="nav-item active"
            @click="ticketStore.selectTicket(null)"
          >
            Tableau
          </button>
          <span class="nav-item disabled">Projets</span>
          <span class="nav-item disabled">Filtres</span>
        </nav>

        <button
          type="button"
          class="jira-create-btn"
          @click="openCreateModal"
          title="Créer une nouvelle demande (ticket)"
        >
          Créer
        </button>
      </div>

      <div class="navbar-right">
        <!-- Badge de statut de connexion à l'API Spring Boot -->
        <div
          class="api-status-pill"
          :class="ticketStore.isBackendConnected ? 'status-connected' : 'status-disconnected'"
          :title="ticketStore.isBackendConnected ? 'Backend Spring Boot connecté (:8080)' : 'Mode local / Backend non détecté sur :8080 (cliquer pour réessayer)'"
          @click="ticketStore.fetchTickets()"
        >
          <span class="status-dot"></span>
          <span class="status-label">{{ ticketStore.isBackendConnected ? 'API Connectée' : 'Mode Local' }}</span>
        </div>

        <div class="project-pill">
          <span class="project-avatar">MT</span>
          <span class="project-title">Mini Tracker</span>
        </div>
        <div class="user-avatar" title="Utilisateur connecté">
          JD
        </div>
      </div>
    </header>

    <!-- Sous-en-tête / Fil d'Ariane Jira -->
    <div class="jira-subheader">
      <div class="subheader-content">
        <nav class="breadcrumb">
          <span class="crumb-link" @click="ticketStore.selectTicket(null)">Projets</span>
          <span class="crumb-separator">/</span>
          <span class="crumb-link" @click="ticketStore.selectTicket(null)">Mini Tracker</span>
          <span class="crumb-separator">/</span>
          <span v-if="ticketStore.selectedTicket" class="crumb-current">
            KAN-{{ ticketStore.selectedTicket.id }}
          </span>
          <span v-else class="crumb-current">
            Tableau Kanban
          </span>
        </nav>
      </div>
    </div>

    <!-- Contenu Principal -->
    <main class="jira-main-content">
      <transition name="jira-fade" mode="out-in">
        <TicketDetail v-if="ticketStore.selectedTicket" key="detail" />
        <div v-else key="board" class="board-view">
          <!-- Formulaire inline ou modal -->
          <TicketForm v-if="isCreateModalOpen" @close="closeCreateModal" />
          <TicketDashboard @open-create="openCreateModal" />
        </div>
      </transition>
    </main>
  </div>
</template>

<style scoped>
.jira-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--jira-bg);
}

/* Navbar Jira */
.jira-navbar {
  height: 56px;
  background-color: var(--jira-nav-bg);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  position: sticky;
  top: 0;
  z-index: 50;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.jira-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--jira-radius);
  transition: background-color 0.15s ease;
}

.jira-logo:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.jira-icon {
  width: 24px;
  height: 24px;
}

.jira-brand-name {
  font-weight: 700;
  font-size: 15px;
  letter-spacing: -0.01em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-item {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: var(--jira-radius);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
}

.nav-item.active {
  background-color: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.nav-item:hover:not(.disabled) {
  background-color: rgba(255, 255, 255, 0.15);
  color: #ffffff;
}

.nav-item.disabled {
  opacity: 0.6;
  cursor: default;
}

.jira-create-btn {
  background-color: #0052cc;
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: var(--jira-radius);
  font-weight: 600;
  font-size: 14px;
  padding: 6px 14px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.jira-create-btn:hover {
  background-color: #0065ff;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.project-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: rgba(255, 255, 255, 0.12);
  padding: 4px 10px;
  border-radius: var(--jira-radius);
}

.project-avatar {
  background-color: #ffab00;
  color: #172b4d;
  font-weight: 700;
  font-size: 11px;
  width: 20px;
  height: 20px;
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.project-title {
  font-size: 13px;
  font-weight: 500;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #00875a;
  color: #ffffff;
  font-weight: 600;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(255, 255, 255, 0.4);
}

/* Subheader / Fil d'Ariane */
.jira-subheader {
  background-color: var(--jira-surface);
  border-bottom: 1px solid var(--jira-border);
  padding: 10px 24px;
}

.subheader-content {
  max-width: 1180px;
  margin: 0 auto;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--jira-text-subtle);
}

.crumb-link {
  cursor: pointer;
}

.crumb-link:hover {
  color: var(--jira-primary);
  text-decoration: underline;
}

.crumb-separator {
  color: var(--jira-text-muted);
}

.crumb-current {
  color: var(--jira-text);
  font-weight: 600;
}

/* Main */
.jira-main-content {
  max-width: 1180px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
  flex: 1;
}

.board-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Transitions */
.jira-fade-enter-active,
.jira-fade-leave-active {
  transition: opacity 0.12s ease;
}

.jira-fade-enter-from,
.jira-fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .nav-links {
    display: none;
  }
  .jira-main-content {
    padding: 16px;
  }
}

/* Statut API Spring Boot */
.api-status-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
}

.api-status-pill:hover {
  filter: brightness(0.95);
}

.api-status-pill.status-connected {
  background-color: #e3fcef;
  color: #006644;
  border: 1px solid #abf5d1;
}

.api-status-pill.status-connected .status-dot {
  background-color: #36b37e;
  box-shadow: 0 0 6px rgba(54, 179, 126, 0.6);
}

.api-status-pill.status-disconnected {
  background-color: #f4f5f7;
  color: #5e6c84;
  border: 1px solid #dfe1e6;
}

.api-status-pill.status-disconnected .status-dot {
  background-color: #ffab00;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}
</style>
