package com.apitracker.apitracker.service;

import com.apitracker.apitracker.domain.AppUser;
import com.apitracker.apitracker.domain.IssueLink;
import com.apitracker.apitracker.domain.IssueLinkType;
import com.apitracker.apitracker.domain.Ticket;
import com.apitracker.apitracker.domain.TicketComment;
import com.apitracker.apitracker.domain.TicketHistoryItem;
import com.apitracker.apitracker.domain.TicketLabel;
import com.apitracker.apitracker.domain.TicketPriority;
import com.apitracker.apitracker.domain.TicketStatus;
import com.apitracker.apitracker.dto.CreateCommentRequest;
import com.apitracker.apitracker.dto.CreateIssueLinkRequest;
import com.apitracker.apitracker.dto.CreateTicketRequest;
import com.apitracker.apitracker.dto.IssueLinkResponse;
import com.apitracker.apitracker.dto.PatchTicketRequest;
import com.apitracker.apitracker.dto.TicketCommentResponse;
import com.apitracker.apitracker.dto.TicketHistoryItemResponse;
import com.apitracker.apitracker.dto.TicketResponse;
import com.apitracker.apitracker.dto.TicketStatsResponse;
import com.apitracker.apitracker.dto.ToggleTicketLabelRequest;
import com.apitracker.apitracker.dto.UpdateTicketAssigneeRequest;
import com.apitracker.apitracker.dto.UpdateTicketDueDateRequest;
import com.apitracker.apitracker.dto.UpdateTicketPriorityRequest;
import com.apitracker.apitracker.dto.UpdateTicketRequest;
import com.apitracker.apitracker.dto.UpdateTicketStatusRequest;
import com.apitracker.apitracker.exception.DuplicateResourceException;
import com.apitracker.apitracker.exception.ResourceNotFoundException;
import com.apitracker.apitracker.repository.AppUserRepository;
import com.apitracker.apitracker.repository.IssueLinkRepository;
import com.apitracker.apitracker.repository.TicketCommentRepository;
import com.apitracker.apitracker.repository.TicketHistoryItemRepository;
import com.apitracker.apitracker.repository.TicketRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private static final String DEFAULT_AUTHOR = "John Doe";

    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketHistoryItemRepository ticketHistoryItemRepository;
    private final IssueLinkRepository issueLinkRepository;
    private final TicketMapper ticketMapper;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            AppUserRepository appUserRepository,
            TicketCommentRepository ticketCommentRepository,
            TicketHistoryItemRepository ticketHistoryItemRepository,
            IssueLinkRepository issueLinkRepository,
            TicketMapper ticketMapper
    ) {
        this.ticketRepository = ticketRepository;
        this.appUserRepository = appUserRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.ticketHistoryItemRepository = ticketHistoryItemRepository;
        this.issueLinkRepository = issueLinkRepository;
        this.ticketMapper = ticketMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findAll(
            TicketStatus status,
            String assigneeId,
            TicketLabel label,
            String search,
            String sortBy,
            String sortOrder
    ) {
        Specification<Ticket> spec = buildSpecification(status, assigneeId, label, search);
        List<Ticket> tickets = ticketRepository.findAll(spec);

        Comparator<Ticket> comparator = buildComparator(sortBy, sortOrder);
        return tickets.stream()
                .sorted(comparator)
                .map(ticketMapper::toTicketResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        Ticket ticket = getTicketOrThrow(id);
        return ticketMapper.toTicketResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketStatsResponse getStats() {
        long total = ticketRepository.count();
        long todo = ticketRepository.countByStatus(TicketStatus.TODO);
        long inProgress = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long done = ticketRepository.countByStatus(TicketStatus.DONE);
        return new TicketStatsResponse(total, todo, inProgress, done);
    }

    @Override
    public TicketResponse create(CreateTicketRequest request) {
        String normalizedAssignee = normalizeBlank(request.assigneeId());
        validateAssigneeIfPresent(normalizedAssignee);

        long now = System.currentTimeMillis();
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title().trim());
        ticket.setDescription(normalizeBlank(request.description()));
        ticket.setStatus(request.status() != null ? request.status() : TicketStatus.TODO);
        ticket.setPriority(request.priority());
        ticket.setCreatedAt(now);
        ticket.setDueDate(normalizeBlank(request.dueDate()));
        ticket.setAssigneeId(normalizedAssignee);
        ticket.setLabels(distinctLabels(request.labels()));

        logHistory(ticket, DEFAULT_AUTHOR, "a créé la demande", null, now);
        Ticket saved = ticketRepository.saveAndFlush(ticket);
        return ticketMapper.toTicketResponse(saved);
    }

    @Override
    public TicketResponse update(Long id, UpdateTicketRequest request) {
        Ticket ticket = getTicketOrThrow(id);
        String normalizedAssignee = normalizeBlank(request.assigneeId());
        validateAssigneeIfPresent(normalizedAssignee);

        String newTitle = request.title().trim();
        String newDescription = normalizeBlank(request.description());
        String newDueDate = normalizeBlank(request.dueDate());
        long now = System.currentTimeMillis();

        if (!Objects.equals(ticket.getTitle(), newTitle)) {
            ticket.setTitle(newTitle);
            logHistory(ticket, DEFAULT_AUTHOR, "a modifié le résumé", "« " + newTitle + " »", now);
        }
        if (!Objects.equals(ticket.getDescription(), newDescription)) {
            ticket.setDescription(newDescription);
            logHistory(ticket, DEFAULT_AUTHOR, "a mis à jour la description", null, now);
        }
        if (ticket.getStatus() != request.status()) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(request.status());
            logHistory(
                    ticket,
                    DEFAULT_AUTHOR,
                    "a changé l'état",
                    oldStatus.getFrenchLabel() + " → " + request.status().getFrenchLabel(),
                    now
            );
        }
        if (ticket.getPriority() != request.priority()) {
            TicketPriority oldPriority = ticket.getPriority();
            ticket.setPriority(request.priority());
            logHistory(
                    ticket,
                    DEFAULT_AUTHOR,
                    "a changé la priorité",
                    oldPriority.getFrenchLabel() + " → " + request.priority().getFrenchLabel(),
                    now
            );
        }
        if (!Objects.equals(ticket.getDueDate(), newDueDate)) {
            ticket.setDueDate(newDueDate);
            logHistory(
                    ticket,
                    DEFAULT_AUTHOR,
                    "a mis à jour la date d'échéance",
                    newDueDate != null ? "Échéance : " + newDueDate : "Retirée",
                    now
            );
        }
        if (!Objects.equals(ticket.getAssigneeId(), normalizedAssignee)) {
            ticket.setAssigneeId(normalizedAssignee);
            logHistory(ticket, DEFAULT_AUTHOR, "a assigné la demande à", resolveAssigneeName(normalizedAssignee), now);
        }

        ticket.setLabels(distinctLabels(request.labels()));
        Ticket saved = ticketRepository.saveAndFlush(ticket);
        return ticketMapper.toTicketResponse(saved);
    }

    @Override
    public TicketResponse patch(Long id, PatchTicketRequest request) {
        Ticket ticket = getTicketOrThrow(id);
        long now = System.currentTimeMillis();

        if (request.title() != null) {
            String trimmedTitle = request.title().trim();
            if (trimmedTitle.isEmpty()) {
                throw new IllegalArgumentException("Le titre ne peut pas être vide");
            }
            if (!Objects.equals(ticket.getTitle(), trimmedTitle)) {
                ticket.setTitle(trimmedTitle);
                logHistory(ticket, DEFAULT_AUTHOR, "a modifié le résumé", "« " + trimmedTitle + " »", now);
            }
        }
        if (request.description() != null) {
            String newDescription = normalizeBlank(request.description());
            if (!Objects.equals(ticket.getDescription(), newDescription)) {
                ticket.setDescription(newDescription);
                logHistory(ticket, DEFAULT_AUTHOR, "a mis à jour la description", null, now);
            }
        }
        if (request.status() != null && ticket.getStatus() != request.status()) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(request.status());
            logHistory(
                    ticket,
                    DEFAULT_AUTHOR,
                    "a changé l'état",
                    oldStatus.getFrenchLabel() + " → " + request.status().getFrenchLabel(),
                    now
            );
        }
        if (request.priority() != null && ticket.getPriority() != request.priority()) {
            TicketPriority oldPriority = ticket.getPriority();
            ticket.setPriority(request.priority());
            logHistory(
                    ticket,
                    DEFAULT_AUTHOR,
                    "a changé la priorité",
                    oldPriority.getFrenchLabel() + " → " + request.priority().getFrenchLabel(),
                    now
            );
        }
        if (request.dueDate() != null) {
            String newDueDate = normalizeBlank(request.dueDate());
            if (!Objects.equals(ticket.getDueDate(), newDueDate)) {
                ticket.setDueDate(newDueDate);
                logHistory(
                        ticket,
                        DEFAULT_AUTHOR,
                        "a mis à jour la date d'échéance",
                        newDueDate != null ? "Échéance : " + newDueDate : "Retirée",
                        now
                );
            }
        }
        if (request.assigneeId() != null) {
            String normalizedAssignee = normalizeBlank(request.assigneeId());
            validateAssigneeIfPresent(normalizedAssignee);
            if (!Objects.equals(ticket.getAssigneeId(), normalizedAssignee)) {
                ticket.setAssigneeId(normalizedAssignee);
                logHistory(
                        ticket,
                        DEFAULT_AUTHOR,
                        "a assigné la demande à",
                        resolveAssigneeName(normalizedAssignee),
                        now
                );
            }
        }
        if (request.labels() != null) {
            ticket.setLabels(distinctLabels(request.labels()));
        }

        Ticket saved = ticketRepository.saveAndFlush(ticket);
        return ticketMapper.toTicketResponse(saved);
    }

    @Override
    public TicketResponse updateStatus(Long id, UpdateTicketStatusRequest request) {
        Ticket ticket = getTicketOrThrow(id);
        if (ticket.getStatus() != request.status()) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(request.status());
            logHistory(
                    ticket,
                    DEFAULT_AUTHOR,
                    "a changé l'état",
                    oldStatus.getFrenchLabel() + " → " + request.status().getFrenchLabel(),
                    System.currentTimeMillis()
            );
        }
        return ticketMapper.toTicketResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public TicketResponse toggleStatus(Long id) {
        Ticket ticket = getTicketOrThrow(id);
        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus nextStatus = oldStatus.next();
        ticket.setStatus(nextStatus);
        logHistory(
                ticket,
                DEFAULT_AUTHOR,
                "a changé l'état",
                oldStatus.getFrenchLabel() + " → " + nextStatus.getFrenchLabel(),
                System.currentTimeMillis()
        );
        return ticketMapper.toTicketResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public TicketResponse updatePriority(Long id, UpdateTicketPriorityRequest request) {
        Ticket ticket = getTicketOrThrow(id);
        if (ticket.getPriority() != request.priority()) {
            TicketPriority oldPriority = ticket.getPriority();
            ticket.setPriority(request.priority());
            logHistory(
                    ticket,
                    DEFAULT_AUTHOR,
                    "a changé la priorité",
                    oldPriority.getFrenchLabel() + " → " + request.priority().getFrenchLabel(),
                    System.currentTimeMillis()
            );
        }
        return ticketMapper.toTicketResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public TicketResponse updateAssignee(Long id, UpdateTicketAssigneeRequest request) {
        Ticket ticket = getTicketOrThrow(id);
        String normalizedAssignee = normalizeBlank(request.assigneeId());
        validateAssigneeIfPresent(normalizedAssignee);

        ticket.setAssigneeId(normalizedAssignee);
        logHistory(
                ticket,
                DEFAULT_AUTHOR,
                "a assigné la demande à",
                resolveAssigneeName(normalizedAssignee),
                System.currentTimeMillis()
        );
        return ticketMapper.toTicketResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public TicketResponse updateDueDate(Long id, UpdateTicketDueDateRequest request) {
        Ticket ticket = getTicketOrThrow(id);
        String normalizedDueDate = normalizeBlank(request.dueDate());
        ticket.setDueDate(normalizedDueDate);
        logHistory(
                ticket,
                DEFAULT_AUTHOR,
                "a mis à jour la date d'échéance",
                normalizedDueDate != null ? "Échéance : " + normalizedDueDate : "Retirée",
                System.currentTimeMillis()
        );
        return ticketMapper.toTicketResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public TicketResponse toggleLabel(Long id, ToggleTicketLabelRequest request) {
        Ticket ticket = getTicketOrThrow(id);
        TicketLabel label = request.label();
        long now = System.currentTimeMillis();

        if (ticket.getLabels().contains(label)) {
            ticket.getLabels().remove(label);
            logHistory(ticket, DEFAULT_AUTHOR, "a retiré l'étiquette « " + label.getValue() + " »", null, now);
        } else {
            ticket.getLabels().add(label);
            logHistory(ticket, DEFAULT_AUTHOR, "a ajouté l'étiquette « " + label.getValue() + " »", null, now);
        }
        return ticketMapper.toTicketResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public void delete(Long id) {
        Ticket ticket = getTicketOrThrow(id);
        issueLinkRepository.deleteByTargetTicketId(id);
        ticketRepository.delete(ticket);
    }

    @Override
    public TicketCommentResponse addComment(Long ticketId, CreateCommentRequest request) {
        Ticket ticket = getTicketOrThrow(ticketId);
        String author = normalizeBlank(request.author());
        if (author == null) {
            author = DEFAULT_AUTHOR;
        }
        long now = System.currentTimeMillis();

        TicketComment comment = new TicketComment(author, request.content().trim(), now);
        ticket.addComment(comment);
        TicketComment savedComment = ticketCommentRepository.saveAndFlush(comment);

        logHistory(ticket, author, "a ajouté un commentaire", null, now);
        ticketRepository.saveAndFlush(ticket);

        return ticketMapper.toCommentResponse(savedComment);
    }

    @Override
    public void deleteComment(Long ticketId, Long commentId) {
        Ticket ticket = getTicketOrThrow(ticketId);
        TicketComment comment = ticketCommentRepository.findByIdAndTicketId(commentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commentaire introuvable avec l'id " + commentId + " pour le ticket " + ticketId
                ));

        ticket.removeComment(comment);
        ticketCommentRepository.delete(comment);
        logHistory(ticket, DEFAULT_AUTHOR, "a supprimé un commentaire", null, System.currentTimeMillis());
        ticketRepository.saveAndFlush(ticket);
    }

    @Override
    public IssueLinkResponse addIssueLink(Long ticketId, CreateIssueLinkRequest request) {
        Long targetId = request.targetTicketId();
        if (Objects.equals(ticketId, targetId)) {
            throw new IllegalArgumentException("Un ticket ne peut pas être lié à lui-même");
        }

        Ticket sourceTicket = getTicketOrThrow(ticketId);
        Ticket targetTicket = getTicketOrThrow(targetId);

        if (issueLinkRepository.existsBySourceTicketIdAndTargetTicketId(ticketId, targetId)) {
            throw new DuplicateResourceException(
                    "Un lien existe déjà entre le ticket KAN-" + ticketId + " et le ticket KAN-" + targetId
            );
        }

        IssueLinkType type = request.type();
        IssueLinkType mirrorType = type.mirror();
        long now = System.currentTimeMillis();

        IssueLink sourceLink = new IssueLink(targetId, type);
        sourceTicket.addLink(sourceLink);
        IssueLink savedSourceLink = issueLinkRepository.saveAndFlush(sourceLink);

        if (!issueLinkRepository.existsBySourceTicketIdAndTargetTicketId(targetId, ticketId)) {
            IssueLink targetLink = new IssueLink(ticketId, mirrorType);
            targetTicket.addLink(targetLink);
            issueLinkRepository.saveAndFlush(targetLink);
        }

        logHistory(
                sourceTicket,
                DEFAULT_AUTHOR,
                "a lié cette demande (" + type.getFrenchLabel() + " KAN-" + targetId + ")",
                null,
                now
        );
        logHistory(
                targetTicket,
                DEFAULT_AUTHOR,
                "a été liée (" + mirrorType.getFrenchLabel() + " KAN-" + ticketId + ")",
                null,
                now
        );

        ticketRepository.saveAndFlush(sourceTicket);
        ticketRepository.saveAndFlush(targetTicket);

        return ticketMapper.toLinkResponse(savedSourceLink);
    }

    @Override
    public void removeIssueLink(Long ticketId, Long targetTicketId) {
        Ticket sourceTicket = getTicketOrThrow(ticketId);
        IssueLink sourceLink = issueLinkRepository.findBySourceTicketIdAndTargetTicketId(ticketId, targetTicketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lien introuvable entre KAN-" + ticketId + " et KAN-" + targetTicketId
                ));

        long now = System.currentTimeMillis();
        sourceTicket.removeLink(sourceLink);
        issueLinkRepository.delete(sourceLink);
        logHistory(sourceTicket, DEFAULT_AUTHOR, "a supprimé le lien avec KAN-" + targetTicketId, null, now);
        ticketRepository.saveAndFlush(sourceTicket);

        ticketRepository.findById(targetTicketId).ifPresent(targetTicket ->
                issueLinkRepository.findBySourceTicketIdAndTargetTicketId(targetTicketId, ticketId)
                        .ifPresent(mirrorLink -> {
                            targetTicket.removeLink(mirrorLink);
                            issueLinkRepository.delete(mirrorLink);
                            logHistory(
                                    targetTicket,
                                    DEFAULT_AUTHOR,
                                    "a supprimé le lien avec KAN-" + ticketId,
                                    null,
                                    now
                            );
                            ticketRepository.saveAndFlush(targetTicket);
                        })
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketHistoryItemResponse> getHistory(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket introuvable avec l'id : " + ticketId);
        }
        return ticketHistoryItemRepository.findByTicketIdOrderByTimestampDescIdDesc(ticketId).stream()
                .map(ticketMapper::toHistoryResponse)
                .toList();
    }

    private Ticket getTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable avec l'id : " + id));
    }

    private void validateAssigneeIfPresent(String assigneeId) {
        if (assigneeId != null && !appUserRepository.existsById(assigneeId)) {
            throw new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + assigneeId);
        }
    }

    private String resolveAssigneeName(String assigneeId) {
        if (assigneeId == null) {
            return "Non assigné";
        }
        return appUserRepository.findById(assigneeId)
                .map(AppUser::getName)
                .orElse("Non assigné");
    }

    private void logHistory(Ticket ticket, String author, String action, String details, long timestamp) {
        TicketHistoryItem item = new TicketHistoryItem(author, action, details, timestamp);
        ticket.addHistoryItem(item);
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<TicketLabel> distinctLabels(List<TicketLabel> labels) {
        if (labels == null) {
            return new ArrayList<>();
        }
        return labels.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private Specification<Ticket> buildSpecification(
            TicketStatus status,
            String assigneeId,
            TicketLabel label,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (assigneeId != null && !assigneeId.isBlank()) {
                if ("UNASSIGNED".equalsIgnoreCase(assigneeId.trim())) {
                    predicates.add(cb.isNull(root.get("assigneeId")));
                } else {
                    predicates.add(cb.equal(root.get("assigneeId"), assigneeId.trim()));
                }
            }

            if (label != null) {
                if (query != null) {
                    query.distinct(true);
                }
                Join<Ticket, TicketLabel> labelJoin = root.join("labels");
                predicates.add(cb.equal(labelJoin, label));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descriptionMatch = cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern);
                Predicate keyMatch = cb.like(
                        cb.concat("kan-", root.get("id").as(String.class)),
                        pattern
                );
                predicates.add(cb.or(titleMatch, descriptionMatch, keyMatch));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Comparator<Ticket> buildComparator(String sortBy, String sortOrder) {
        String field = sortBy != null ? sortBy : "id";
        boolean ascending = "asc".equalsIgnoreCase(sortOrder);

        Comparator<Ticket> comparator = switch (field) {
            case "title" -> Comparator.comparing(
                    t -> t.getTitle() != null ? t.getTitle() : "",
                    String.CASE_INSENSITIVE_ORDER
            );
            case "priority" -> Comparator.comparingInt(t -> t.getPriority() == TicketPriority.HIGH ? 2 : 1);
            case "status" -> Comparator.comparingInt(t -> switch (t.getStatus()) {
                case TODO -> 1;
                case IN_PROGRESS -> 2;
                case DONE -> 3;
            });
            case "dueDate" -> Comparator.comparing(
                    t -> t.getDueDate() != null ? t.getDueDate() : "9999-99-99"
            );
            default -> Comparator.comparing(Ticket::getId);
        };

        return ascending ? comparator : comparator.reversed();
    }

    @Override
    @Transactional
    public List<TicketResponse> resetToDemoData() {
        issueLinkRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        ticketHistoryItemRepository.deleteAll();
        ticketRepository.deleteAll();
        ticketRepository.flush();

        long now = System.currentTimeMillis();
        long hourMs = 3_600_000L;
        LocalDate today = LocalDate.now();
        String yesterday = today.minusDays(1).toString();
        String tomorrow = today.plusDays(1).toString();
        String nextWeek = today.plusDays(7).toString();

        Ticket ticket1 = new Ticket();
        ticket1.setTitle("Configurer le repo Git");
        ticket1.setDescription("Initialiser le dépôt GitHub, configurer la branche main, ajouter le .gitignore et les règles de commit.");
        ticket1.setStatus(TicketStatus.DONE);
        ticket1.setPriority(TicketPriority.HIGH);
        ticket1.setCreatedAt(now - hourMs * 48);
        ticket1.setDueDate(yesterday);
        ticket1.setAssigneeId("user-jd");
        ticket1.setLabels(List.of(TicketLabel.DEVOPS));
        ticket1.addComment(new TicketComment("John Doe", "Le repository a été configuré avec les hooks de pre-commit.", now - hourMs * 36));
        ticket1.addHistoryItem(new TicketHistoryItem("John Doe", "a créé la demande", null, now - hourMs * 48));
        ticket1.addHistoryItem(new TicketHistoryItem("John Doe", "a changé l'état", "À FAIRE → EN COURS", now - hourMs * 40));
        ticket1.addHistoryItem(new TicketHistoryItem("John Doe", "a changé l'état", "EN COURS → TERMINÉ", now - hourMs * 30));

        Ticket ticket2 = new Ticket();
        ticket2.setTitle("Créer le formulaire Vue 3");
        ticket2.setDescription("Mettre en place le composant de saisie avec validation du titre et gestion de la priorité.");
        ticket2.setStatus(TicketStatus.IN_PROGRESS);
        ticket2.setPriority(TicketPriority.HIGH);
        ticket2.setCreatedAt(now - hourMs * 12);
        ticket2.setDueDate(tomorrow);
        ticket2.setAssigneeId("user-jd");
        ticket2.setLabels(List.of(TicketLabel.FRONTEND, TicketLabel.UI_UX));
        ticket2.addComment(new TicketComment("Chef de projet", "Pense bien à ajouter la validation sur le champ résumé.", now - hourMs * 8));
        ticket2.addComment(new TicketComment("John Doe", "C'est en cours, les tests unitaires suivent dans le ticket KAN-3.", now - hourMs * 4));
        ticket2.addHistoryItem(new TicketHistoryItem("Chef de projet", "a créé la demande", null, now - hourMs * 12));
        ticket2.addHistoryItem(new TicketHistoryItem("John Doe", "a changé l'état", "À FAIRE → EN COURS", now - hourMs * 6));

        Ticket ticket3 = new Ticket();
        ticket3.setTitle("Écrire les tests Vitest");
        ticket3.setDescription("Couvrir les actions du store Pinia (addTicket, toggleStatus, deleteTicket, filtres et tris).");
        ticket3.setStatus(TicketStatus.TODO);
        ticket3.setPriority(TicketPriority.LOW);
        ticket3.setCreatedAt(now - hourMs * 3);
        ticket3.setDueDate(nextWeek);
        ticket3.setAssigneeId("user-sc");
        ticket3.setLabels(List.of(TicketLabel.FRONTEND, TicketLabel.FEATURE));
        ticket3.addHistoryItem(new TicketHistoryItem("John Doe", "a créé la demande", null, now - hourMs * 3));

        ticket1 = ticketRepository.saveAndFlush(ticket1);
        ticket2 = ticketRepository.saveAndFlush(ticket2);
        ticket3 = ticketRepository.saveAndFlush(ticket3);

        ticket1.addLink(new IssueLink(ticket2.getId(), IssueLinkType.BLOCKS));
        ticket2.addLink(new IssueLink(ticket1.getId(), IssueLinkType.IS_BLOCKED_BY));
        ticket2.addLink(new IssueLink(ticket3.getId(), IssueLinkType.RELATES_TO));
        ticket3.addLink(new IssueLink(ticket2.getId(), IssueLinkType.RELATES_TO));

        ticketRepository.saveAllAndFlush(List.of(ticket1, ticket2, ticket3));
        return findAll(null, null, null, null, "id", "asc");
    }
}
