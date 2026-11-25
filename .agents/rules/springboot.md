# Règles d'Architecture et Conventions Spring Boot

Ce document regroupe les standards d'ingénierie et règles obligatoires pour le développement du backend Spring Boot dans `apitracker/`.

---

## 1. Java 17+ & Modélisation des Données (Records & DTOs)

- **DTOs sous forme de `record` exclusivement** :
  - Tout transfert de données en entrée (`*Request`) ou en sortie (`*Response`) de l'API REST doit être implémenté via un `record` Java.
  - Exemples : `TicketResponse`, `CreateTicketRequest`, `UpdateTicketStatusRequest`, `CommentResponse`.
- **Interdiction formelle d'exposer les entités JPA** :
  - Aucune classe annotée `@Entity` ne doit être retournée directement par les contrôleurs REST ou acceptée comme paramètre `@RequestBody`.
  - La conversion entre les entités JPA et les DTOs `record` doit être effectuée au niveau de la couche Service (via des constructeurs de conversion ou des méthodes de mapping statiques / dédiées).
- **Validation déclarative (`jakarta.validation`)** :
  - Utiliser les annotations de validation Bean Validation sur les records de requête (`@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, etc.).
  - Placer l'annotation `@Valid` sur les paramètres `@RequestBody` des contrôleurs.

---

## 2. Injection de Dépendances Strictement par Constructeur

- **Interdiction formelle du `@Autowired` sur les champs (`Field Injection`)** :
  - Le marquage de champs avec `@Autowired` est strictement proscrit.
- **Constructeurs explicites pour l'injection** :
  - Toutes les dépendances d'un composant (`@Service`, `@RestController`, etc.) doivent être déclarées `private final`.
  - L'injection doit être réalisée par le constructeur de classe Java (Spring injecte automatiquement les dépendances si un seul constructeur est présent).
  
```java
// ✅ CONFORME : Dépendances déclarées final et injectées par constructeur
@Service
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;

    public TicketServiceImpl(TicketRepository ticketRepository, CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
    }
}

// ❌ NON CONFORME : Injection de champ
@Service
public class TicketServiceImpl implements TicketService {
    @Autowired
    private TicketRepository ticketRepository;
}
```

---

## 3. Architecture en Couches Stricte

Le flux de données et d'appels doit strictement respecter le sens unidirectionnel suivant :

$$\text{Client HTTP} \longrightarrow \text{Controller} \longrightarrow \text{Service} \longrightarrow \text{Repository} \longrightarrow \text{Base de Données}$$

1. **Couche Contrôleur (`@RestController`)** :
   - Rôle : Point d'entrée HTTP, routage, extraction des paramètres, validation syntaxique des entrées (`@Valid`), codes HTTP de retour et restitution des `record` DTOs.
   - **Interdiction** : Ne contient aucune logique métier ni accès direct aux `Repository`.
2. **Couche Service (`@Service`)** :
   - Rôle : Logique métier, transactions (`@Transactional`), orchestration, règles de gestion, transformation Entity $\longleftrightarrow$ DTO.
3. **Couche Persistance (`@Repository` / `JpaRepository`)** :
   - Rôle : Opérations CRUD et requêtes vers la base de données relationnelle H2.
   - **Interdiction** : Ne manipule pas les DTOs de l'API.

---

## 4. Gestion Centralisée des Erreurs (`@RestControllerAdvice`)

- Toute exception levée par l'application doit être capturée et transformée en réponse HTTP structurée par une classe annotée `@RestControllerAdvice`.
- Modèle d'erreur standardisé (record) :
  ```java
  public record ErrorResponse(
      Instant timestamp,
      int status,
      String error,
      String message,
      String path,
      List<ValidationErrorDetail> validationErrors
  ) {}
  ```
- Exceptions obligatoirement prises en charge :
  - `MethodArgumentNotValidException` $\rightarrow$ `400 Bad Request` avec liste détaillée des champs en erreur et contraintes violées.
  - Exceptions métier personnalisées (ex: `ResourceNotFoundException`) $\rightarrow$ `404 Not Found`.
  - `IllegalArgumentException` / `IllegalStateException` $\rightarrow$ `400 Bad Request` ou `409 Conflict`.
  - `Exception` générique $\rightarrow$ `500 Internal Server Error` (avec journalisation sans divulgation de détails internes sensibles au client).

---

## 5. Configuration CORS (Cross-Origin Resource Sharing)

- Autoriser de manière explicite l'environnement de développement Vue 3 :
  - Origine autorisée : `http://localhost:5173`.
  - Méthodes HTTP autorisées : `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
  - En-têtes autorisés : `Content-Type`, `Authorization`, etc.
  - Autoriser les credentials si nécessaire (`allowCredentials(true)`).
- Implémentation recommandée : Bean `WebMvcConfigurer` centralisé ou configuration globale dans la classe de sécurité / configuration MVC, plutôt que de multiplier les annotations `@CrossOrigin` dispersées sur chaque contrôleur.

---

## 6. Stratégie de Tests Unitaires & Intégration

- **Technologies obligatoires** :
  - **JUnit 5** (`org.junit.jupiter.api.*`)
  - **Mockito** (`org.mockito.*` & `@ExtendWith(MockitoExtension.class)`)
  - **Spring Test / MockMvc** (`@WebMvcTest` ou `MockMvcBuilders`)
- **Tests des Services** :
  - Tester la logique métier de manière unitaire et isolée en mockant les repositories avec Mockito.
  - Vérifier systématiquement les cas nominaux et les cas d'erreur (ex: exception levée si ID introuvable).
- **Tests des Contrôleurs** :
  - Tester les routes REST via `MockMvc` pour valider les codes de statut HTTP, la sérialisation JSON, et le rejet des requêtes invalides (`@Valid`).
- **Validation continue** :
  - Chaque fonctionnalité développée doit valider l'exécution sans échec de `./mvnw test`.
