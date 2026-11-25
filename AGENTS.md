# Architecture Multi-Agents & Conventions du Projet Mini-Tracker

Ce document définit la structure générale, les commandes clés, les conventions strictes et l'orchestration multi-agents pour le projet **Mini-Tracker**.

---

## 1. Structure du Projet

Le projet est un monorepo comprenant deux modules distincts :

```text
mini-tracker/
├── AGENTS.md                   # Règles globales et orchestration des agents
├── .agents/                    # Configuration Antigravity / Agents
│   ├── rules/
│   │   └── springboot.md       # Règles strictes d'architecture Spring Boot
│   └── agents/
│       ├── architect.md        # Définition de l'agent Architecte
│       ├── backend-dev.md      # Définition de l'agent Backend Dev
│       └── qa-reviewer.md      # Définition de l'agent QA Reviewer
│
├── frontend/                   # Frontend Vue 3 + TypeScript + Vite + Pinia
│   ├── src/
│   │   ├── stores/             # Stores Pinia (contrat de données du domaine)
│   │   │   ├── ticketStore.ts  # Types TypeScript & logique des tickets
│   │   │   └── ticketStore.spec.ts
│   │   ├── components/         # Composants UI (Dashboard, Kanban, Détail, etc.)
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   └── vite.config.ts
│
└── apitracker/                 # Backend Java 17+ / Spring Boot 3.x / 4.x
    ├── src/
    │   ├── main/
    │   │   ├── java/com/apitracker/apitracker/
    │   │   └── resources/
    │   │       └── application.properties
    │   └── test/
    │       └── java/com/apitracker/apitracker/
    ├── pom.xml
    └── mvnw / mvnw.cmd
```

---

## 2. Commandes de Build & de Test

### Backend Spring Boot (`apitracker/`)
Toutes les commandes Maven doivent être exécutées depuis le répertoire `apitracker/` :

- **Lancer les tests unitaires et d'intégration** :
  ```bash
  ./mvnw test
  # ou sous Windows :
  .\mvnw.cmd test
  # ou si Maven global est installé :
  mvn test
  ```
- **Compiler le projet** :
  ```bash
  ./mvnw clean compile
  ```
- **Démarrer l'application locale** :
  ```bash
  ./mvnw spring-boot:run
  ```

### Frontend Vue 3 (`frontend/`)
Toutes les commandes npm doivent être exécutées depuis le répertoire `frontend/` :

- **Lancer les tests unitaires** :
  ```bash
  npm run test:unit
  # ou :
  npm run test
  ```
- **Vérification des types TypeScript** :
  ```bash
  npm run type-check
  ```
- **Build de production** :
  ```bash
  npm run build
  ```
- **Serveur de développement** (URL par défaut : `http://localhost:5173`) :
  ```bash
  npm run dev
  ```

---

## 3. Règle Stricte : Contrat d'API REST & Synchronisation TypeScript

> [!IMPORTANT]
> **Le contrat de l'API REST Spring Boot doit obligatoirement et strictement correspondre aux interfaces TypeScript définies dans le frontend.**

Référence absolue côté frontend : `frontend/src/stores/ticketStore.ts`.

### Entités et Types Cibles
- **Ticket** :
  - `id` (`Long` / `number`)
  - `title` (`String` - obligatoire)
  - `description` (`String` - facultatif)
  - `status` (`TicketStatus` : `TODO`, `IN_PROGRESS`, `DONE`)
  - `priority` (`TicketPriority` : `LOW`, `HIGH`)
  - `createdAt` (`Long` / timestamp epoch ms)
  - `dueDate` (`String` au format `YYYY-MM-DD` ou `LocalDate`)
  - `assigneeId` (`String` correspondant à `User.id`)
  - `labels` (`List<TicketLabel>` : `Frontend`, `Backend`, `Bug`, `Feature`, `UI/UX`, `DevOps`)
  - `comments` (`List<TicketCommentRecord>`)
  - `history` (`List<TicketHistoryItemRecord>`)
  - `links` (`List<IssueLinkRecord>`)
- **TicketComment** :
  - `id` (`Long`), `author` (`String`), `content` (`String`), `createdAt` (`Long`)
- **TicketHistoryItem** :
  - `id` (`Long`), `author` (`String`), `action` (`String`), `details` (`String`), `timestamp` (`Long`)
- **IssueLink** :
  - `id` (`Long`), `targetTicketId` (`Long`), `type` (`IssueLinkType` : `blocks`, `is_blocked_by`, `relates_to`)
- **User** :
  - `id` (`String`), `name` (`String`), `initials` (`String`), `color` (`String`)

### Règles de Convention JSON / DTO
- Utiliser la nomenclature **camelCase** pour l'ensemble des clés JSON afin d'éviter tout mapping complexe côté client TypeScript.
- Les DTOs de réponse et de requête Spring Boot sont obligatoirement des `record` Java.
- Les codes de réponse HTTP doivent respecter les standards REST :
  - `200 OK` : Récupération ou mise à jour réussie.
  - `201 Created` : Création de ressource réussie (avec header `Location` si applicable).
  - `204 No Content` : Suppression réussie.
  - `400 Bad Request` : Erreur de validation de payload (avec détails des champs invalides).
  - `404 Not Found` : Ressource inexistante.

---

## 4. Organisation Multi-Agents

Le cycle de développement s'appuie sur trois sous-agents spécialisés :

1. **`architect`** *(Mode lecture seule / conception)* :
   - Inspecte le store Pinia et les composants Vue 3.
   - Conçoit les endpoints REST, les DTOs `record`, la gestion des erreurs et le schéma de base relationnel H2.
   - Émet un plan de conception validé avant tout codage.

2. **`backend-dev`** *(Implémentation Spring Boot)* :
   - Implémente le code Java 17+ dans `apitracker/`.
   - Respecte scrupuleusement le plan de l'architecte et les règles de `.agents/rules/springboot.md`.
   - Met en place Entity JPA, Repository, Service, Controller, CORS et `@RestControllerAdvice`.

3. **`qa-reviewer`** *(Qualité, Tests & Revue)* :
   - Rédige et maintient la couverture de tests JUnit 5 et Mockito.
   - Exécute `mvn test` pour valider la non-régression.
   - Effectue la revue critique de code (sécurité, conformité aux règles `springboot.md`, robustesse des cas limites).
