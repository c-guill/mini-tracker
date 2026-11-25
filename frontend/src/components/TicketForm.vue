<script setup lang="ts">
import { ref } from 'vue'
import {
  useTicketStore,
  type TicketPriority,
  type TicketLabel,
  TEAM_MEMBERS,
  ALL_LABELS
} from '../stores/ticketStore'

const emit = defineEmits<{
  (e: 'close'): void
}>()

const ticketStore = useTicketStore()

const title = ref('')
const description = ref('')
const priority = ref<TicketPriority>('LOW')
const assigneeId = ref('user-jd')
const dueDate = ref('')
const selectedLabels = ref<TicketLabel[]>([])
const titleInput = ref<HTMLInputElement | null>(null)
const error = ref('')

function toggleLabel(label: TicketLabel) {
  const idx = selectedLabels.value.indexOf(label)
  if (idx !== -1) {
    selectedLabels.value.splice(idx, 1)
  } else {
    selectedLabels.value.push(label)
  }
}

function handleSubmit() {
  const trimmed = title.value.trim()
  if (!trimmed) {
    error.value = 'Vous devez spécifier un résumé pour cette demande.'
    titleInput.value?.focus()
    return
  }

  ticketStore.addTicket(
    trimmed,
    priority.value,
    description.value,
    assigneeId.value || undefined,
    dueDate.value || undefined,
    selectedLabels.value
  )

  title.value = ''
  description.value = ''
  priority.value = 'LOW'
  assigneeId.value = 'user-jd'
  dueDate.value = ''
  selectedLabels.value = []
  error.value = ''
  emit('close')
}
</script>

<template>
  <div class="jira-modal-backdrop" @click.self="emit('close')">
    <div class="jira-dialog" role="dialog" aria-labelledby="modal-title">
      <!-- En-tête Jira Modal -->
      <header class="dialog-header">
        <h2 id="modal-title" class="dialog-title">Créer une demande</h2>
        <button
          type="button"
          class="dialog-close-btn"
          @click="emit('close')"
          aria-label="Fermer la boîte de dialogue"
        >
          ✕
        </button>
      </header>

      <!-- Corps du formulaire Jira -->
      <form @submit.prevent="handleSubmit" class="dialog-body" novalidate>
        <!-- Projet & Type d'issue -->
        <div class="fields-row">
          <div class="field-col">
            <label class="field-label">Projet</label>
            <div class="readonly-select">
              <span class="project-dot"></span>
              <span>Mini Tracker (KAN)</span>
            </div>
          </div>

          <div class="field-col">
            <label class="field-label">Type de demande</label>
            <div class="readonly-select">
              <span class="issue-type-icon">
                <svg viewBox="0 0 16 16" fill="currentColor">
                  <path d="M2 3a1 1 0 011-1h10a1 1 0 011 1v10a1 1 0 01-1 1H3a1 1 0 01-1-1V3zm10 2.5a.75.75 0 00-1.06-1.06L6.5 8.88 5.06 7.44a.75.75 0 00-1.06 1.06l2 2a.75.75 0 001.06 0l4.94-5z" />
                </svg>
              </span>
              <span>Tâche</span>
            </div>
          </div>
        </div>

        <hr class="jira-divider" />

        <!-- Champ Résumé (Titre) -->
        <div class="form-group">
          <label for="issue-summary" class="field-label required">
            Résumé
          </label>
          <input
            id="issue-summary"
            ref="titleInput"
            v-model="title"
            type="text"
            class="jira-input"
            :class="{ 'has-error': error }"
            placeholder="Ex. Configurer la pipeline CI/CD..."
            @input="error = ''"
          />
          <span v-if="error" class="field-error">{{ error }}</span>
        </div>

        <!-- Priorité, Assignee & Échéance -->
        <div class="fields-row">
          <div class="field-col">
            <label for="issue-priority" class="field-label">Priorité</label>
            <select
              id="issue-priority"
              v-model="priority"
              class="jira-select"
            >
              <option value="LOW">Basse (Low)</option>
              <option value="HIGH">Haute (High)</option>
            </select>
          </div>

          <div class="field-col">
            <label for="issue-assignee" class="field-label">Responsable</label>
            <select
              id="issue-assignee"
              v-model="assigneeId"
              class="jira-select"
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

          <div class="field-col">
            <label for="issue-due" class="field-label">Échéance</label>
            <input
              id="issue-due"
              v-model="dueDate"
              type="date"
              class="jira-input"
            />
          </div>
        </div>

        <!-- Étiquettes (Labels) -->
        <div class="form-group">
          <label class="field-label">Étiquettes</label>
          <div class="labels-selection-wrap">
            <button
              v-for="label in ALL_LABELS"
              :key="label"
              type="button"
              class="form-label-chip"
              :class="{ active: selectedLabels.includes(label) }"
              @click="toggleLabel(label)"
            >
              {{ label }}
            </button>
          </div>
        </div>

        <!-- Description -->
        <div class="form-group">
          <label for="issue-desc" class="field-label">Description</label>
          <textarea
            id="issue-desc"
            v-model="description"
            class="jira-textarea"
            rows="3"
            placeholder="Ajoutez une description détaillée de la demande..."
          ></textarea>
        </div>

        <!-- Footer Jira Modal -->
        <footer class="dialog-footer">
          <button
            type="button"
            class="jira-btn-subtle"
            @click="emit('close')"
          >
            Annuler
          </button>
          <button type="submit" class="jira-btn-primary">
            Créer
          </button>
        </footer>
      </form>
    </div>
  </div>
</template>

<style scoped>
.jira-modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(9, 30, 66, 0.54);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 20px;
}

.jira-dialog {
  background-color: var(--jira-surface);
  border-radius: var(--jira-radius);
  box-shadow: 0 8px 30px rgba(9, 30, 66, 0.35);
  width: 100%;
  max-width: 640px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: jiraDialogIn 0.15s ease-out;
}

@keyframes jiraDialogIn {
  from {
    opacity: 0;
    transform: scale(0.97);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.dialog-header {
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--jira-border-subtle);
}

.dialog-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--jira-text);
  margin: 0;
}

.dialog-close-btn {
  background: transparent;
  border: none;
  font-size: 16px;
  color: var(--jira-text-subtle);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--jira-radius);
}

.dialog-close-btn:hover {
  background-color: var(--jira-surface-hover);
  color: var(--jira-text);
}

.dialog-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.fields-row {
  display: flex;
  gap: 12px;
}

.field-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--jira-text-subtle);
}

.field-label.required::after {
  content: " *";
  color: #de350b;
}

.readonly-select {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background-color: var(--jira-bg);
  border: 1px solid var(--jira-border);
  border-radius: var(--jira-radius);
  font-size: 13px;
  color: var(--jira-text);
}

.project-dot {
  width: 14px;
  height: 14px;
  background-color: #ffab00;
  border-radius: 2px;
}

.issue-type-icon {
  width: 14px;
  height: 14px;
  color: #4bade8;
}

.jira-divider {
  border: 0;
  border-top: 1px solid var(--jira-border-subtle);
  margin: 4px 0;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.jira-input,
.jira-select,
.jira-textarea {
  width: 100%;
  padding: 6px 8px;
  font-size: 13px;
  color: var(--jira-text);
  background-color: var(--jira-surface);
  border: 2px solid var(--jira-border);
  border-radius: var(--jira-radius);
  outline: none;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.jira-input:hover,
.jira-select:hover,
.jira-textarea:hover {
  background-color: #fafbfc;
}

.jira-input:focus,
.jira-select:focus,
.jira-textarea:focus {
  border-color: var(--jira-border-focus);
  background-color: #ffffff;
}

.jira-input.has-error {
  border-color: #de350b;
}

.field-error {
  font-size: 12px;
  color: #de350b;
  margin-top: 2px;
}

.labels-selection-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.form-label-chip {
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 500;
  border: 1px solid var(--jira-border);
  background-color: var(--jira-bg);
  color: var(--jira-text-subtle);
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.12s;
}

.form-label-chip:hover {
  background-color: var(--jira-surface-hover);
  color: var(--jira-text);
}

.form-label-chip.active {
  background-color: #deebff;
  color: #0052cc;
  border-color: #4c9aff;
  font-weight: 600;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--jira-border-subtle);
}

.jira-btn-primary {
  background-color: var(--jira-primary);
  color: #ffffff;
  border: none;
  border-radius: var(--jira-radius);
  font-weight: 500;
  padding: 6px 14px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.jira-btn-primary:hover {
  background-color: var(--jira-primary-hover);
}

.jira-btn-subtle {
  background: transparent;
  color: var(--jira-text-subtle);
  border: none;
  border-radius: var(--jira-radius);
  font-weight: 500;
  padding: 6px 12px;
  cursor: pointer;
}

.jira-btn-subtle:hover {
  background-color: var(--jira-surface-hover);
  color: var(--jira-text);
}
</style>
