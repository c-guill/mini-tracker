# Mini-Tracker 🎯

Mini-Tracker est un outil de gestion agile de tickets (style Jira Software), développé en monorepo avec un frontend Vue 3 réactif et un backend Spring Boot robuste.

---

## 🛠 Stack Technique

- **Frontend (`frontend/`)** :
  - **Framework** : Vue 3 (Composition API, `<script setup>`)
  - **State Management** : Pinia (avec support offline-first et synchronisation API)
  - **Langage & Build** : TypeScript, Vite
  - **Tests** : Vitest, vue-tsc

- **Backend (`apitracker/`)** :
  - **Framework** : Spring Boot 4.x / Java 17+
  - **Persistance** : Spring Data JPA, Base relationnelle H2 en mémoire
  - **Architecture** : Controller $\rightarrow$ Service $\rightarrow$ Repository, DTOs `record`, gestion centralisée `@RestControllerAdvice`, CORS activé
  - **Tests** : JUnit 5, Mockito, Spring WebMvcTest / SpringBootTest

- **Architecture Multi-Agents (`.agents/` & `AGENTS.md`)** :
  - Orchestration basée sur 3 rôles spécialisés :
    - **`architect`** (`model: pro`) : Conception du contrat REST, parité TypeScript et schéma H2.
    - **`backend-dev`** (`model: flash`) : Implémentation rapide du code Java Spring Boot.
    - **`qa-reviewer`** (`model: pro`) : Rédaction des tests JUnit, validation `mvn test` et revue de code critique.

---

## 📁 Structure du Projet

```text
mini-tracker/
├── AGENTS.md                  # Conventions globales et orchestration des agents
├── README.md                  # Documentation principale
├── .gitignore                 # Exclusion des fichiers IDE, builds et environnements
├── .agents/                   # Configuration Antigravity
│   ├── rules/springboot.md    # Règles strictes d'ingénierie Spring Boot
│   └── agents/                # Définitions des 3 sous-agents (architect, backend-dev, qa-reviewer)
├── frontend/                  # Application Vue 3 + TypeScript
│   ├── src/
│   │   ├── components/        # Kanban, Dashboard, Détail, Formulaire
│   │   ├── services/          # Client API HTTP typé (ticketApi.ts)
│   │   └── stores/            # Store Pinia (ticketStore.ts)
│   ├── package.json
│   └── vite.config.ts
└── apitracker/                # API REST Spring Boot + Java 17
    ├── src/
    │   ├── main/java/...      # Domain, DTOs (records), Repositories, Services, Controllers
    │   └── test/java/...      # 51 tests unitaires et d'intégration
    ├── pom.xml
    └── mvnw / mvnw.cmd
```

---

## 🚀 Démarrage Rapide

### 1. Démarrer le Backend Spring Boot
Depuis le répertoire `apitracker/` :
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
- **API REST** : `http://localhost:8080/api/v1/tickets`
- **Console H2** : `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:minitracker`, User: `sa`, sans mot de passe)

### 2. Démarrer le Frontend Vue 3
Depuis le répertoire `frontend/` (dans un second terminal) :
```bash
npm install
npm run dev
```
- **Application Web** : `http://localhost:5173`
- Une pastille verte **« API Connectée »** dans la barre de navigation confirme la communication avec le backend.

### 3. Déploiement Coolify / Docker Compose
Pour un déploiement conteneurisé ou sur [Coolify](https://coolify.io/) :
```bash
docker compose up --build -d
```
- Les conteneurs communiquent sur un réseau interne (`minitracker-net`).
- Nginx sert le frontend sur le port 80 et redirige les requêtes `/api/` vers `backend:8080`.
- Aucun port fixe n'est mappé sur l'hôte pour laisser le proxy Traefik de Coolify gérer le routage des domaines et le SSL/HTTPS.

---

## 🧪 Exécution des Tests

### Backend (51 tests JUnit 5 / Mockito)
```bash
cd apitracker
.\mvnw.cmd test    # Windows
./mvnw test        # Linux / macOS
```

### Frontend (Vitest & vérification des types)
```bash
cd frontend
npm run test       # ou npm run test:unit
npm run type-check # vérification stricte vue-tsc
npm run build      # compilation du bundle de production
```

---

## 📜 Conventions Clés
- **Contrat strict** : Le format JSON échangé entre le backend et le frontend respecte impérativement la nomenclature **camelCase** et les interfaces TypeScript de [`ticketStore.ts`](frontend/src/stores/ticketStore.ts).
- **DTOs Immuables** : Tous les DTOs backend sont obligatoirement des `record` Java 17+.
- **Injection Propre** : Dépendances déclarées `private final` et injectées par constructeur (aucun `@Autowired` sur champ).
