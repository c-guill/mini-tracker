package com.apitracker.apitracker.config;

import com.apitracker.apitracker.domain.AppUser;
import com.apitracker.apitracker.domain.IssueLink;
import com.apitracker.apitracker.domain.IssueLinkType;
import com.apitracker.apitracker.domain.Ticket;
import com.apitracker.apitracker.domain.TicketComment;
import com.apitracker.apitracker.domain.TicketHistoryItem;
import com.apitracker.apitracker.domain.TicketLabel;
import com.apitracker.apitracker.domain.TicketPriority;
import com.apitracker.apitracker.domain.TicketStatus;
import com.apitracker.apitracker.repository.AppUserRepository;
import com.apitracker.apitracker.repository.TicketRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final long HOUR_MS = 3_600_000L;

    private final AppUserRepository appUserRepository;
    private final TicketRepository ticketRepository;

    public DataInitializer(AppUserRepository appUserRepository, TicketRepository ticketRepository) {
        this.appUserRepository = appUserRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedTickets();
    }

    private void seedUsers() {
        if (appUserRepository.count() > 0) {
            return;
        }
        List<AppUser> users = List.of(
                new AppUser("user-jd", "John Doe", "JD", "#00875a"),
                new AppUser("user-sc", "Sarah Connor", "SC", "#6554c0"),
                new AppUser("user-ad", "Alex Dev", "AD", "#0052cc"),
                new AppUser("user-pm", "Chef de projet", "MT", "#ffab00")
        );
        appUserRepository.saveAll(users);
    }

    private void seedTickets() {
        if (ticketRepository.count() > 0) {
            return;
        }

        long now = System.currentTimeMillis();
        LocalDate today = LocalDate.now();
        String yesterday = today.minusDays(1).toString();
        String tomorrow = today.plusDays(1).toString();
        String nextWeek = today.plusDays(7).toString();

        // KAN-1
        Ticket ticket1 = new Ticket();
        ticket1.setTitle("Configurer le repo Git");
        ticket1.setDescription("Initialiser le dépôt GitHub, configurer la branche main, ajouter le .gitignore et les règles de commit.");
        ticket1.setStatus(TicketStatus.DONE);
        ticket1.setPriority(TicketPriority.HIGH);
        ticket1.setCreatedAt(now - HOUR_MS * 48);
        ticket1.setDueDate(yesterday);
        ticket1.setAssigneeId("user-jd");
        ticket1.setLabels(List.of(TicketLabel.DEVOPS));
        ticket1.addComment(new TicketComment(
                "John Doe",
                "Le repository a été configuré avec les hooks de pre-commit.",
                now - HOUR_MS * 36
        ));
        ticket1.addHistoryItem(new TicketHistoryItem("John Doe", "a créé la demande", null, now - HOUR_MS * 48));
        ticket1.addHistoryItem(new TicketHistoryItem("John Doe", "a changé l'état", "À FAIRE → EN COURS", now - HOUR_MS * 40));
        ticket1.addHistoryItem(new TicketHistoryItem("John Doe", "a changé l'état", "EN COURS → TERMINÉ", now - HOUR_MS * 30));

        // KAN-2
        Ticket ticket2 = new Ticket();
        ticket2.setTitle("Créer le formulaire Vue 3");
        ticket2.setDescription("Mettre en place le composant de saisie avec validation du titre et gestion de la priorité.");
        ticket2.setStatus(TicketStatus.IN_PROGRESS);
        ticket2.setPriority(TicketPriority.HIGH);
        ticket2.setCreatedAt(now - HOUR_MS * 12);
        ticket2.setDueDate(tomorrow);
        ticket2.setAssigneeId("user-jd");
        ticket2.setLabels(List.of(TicketLabel.FRONTEND, TicketLabel.UI_UX));
        ticket2.addComment(new TicketComment(
                "Chef de projet",
                "Pense bien à ajouter la validation sur le champ résumé.",
                now - HOUR_MS * 8
        ));
        ticket2.addComment(new TicketComment(
                "John Doe",
                "C'est en cours, les tests unitaires suivent dans le ticket KAN-3.",
                now - HOUR_MS * 4
        ));
        ticket2.addHistoryItem(new TicketHistoryItem("Chef de projet", "a créé la demande", null, now - HOUR_MS * 12));
        ticket2.addHistoryItem(new TicketHistoryItem("John Doe", "a changé l'état", "À FAIRE → EN COURS", now - HOUR_MS * 6));

        // KAN-3
        Ticket ticket3 = new Ticket();
        ticket3.setTitle("Écrire les tests Vitest");
        ticket3.setDescription("Couvrir les actions du store Pinia (addTicket, toggleStatus, deleteTicket, filtres et tris).");
        ticket3.setStatus(TicketStatus.TODO);
        ticket3.setPriority(TicketPriority.LOW);
        ticket3.setCreatedAt(now - HOUR_MS * 3);
        ticket3.setDueDate(nextWeek);
        ticket3.setAssigneeId("user-sc");
        ticket3.setLabels(List.of(TicketLabel.FRONTEND, TicketLabel.FEATURE));
        ticket3.addHistoryItem(new TicketHistoryItem("John Doe", "a créé la demande", null, now - HOUR_MS * 3));

        ticket1 = ticketRepository.saveAndFlush(ticket1);
        ticket2 = ticketRepository.saveAndFlush(ticket2);
        ticket3 = ticketRepository.saveAndFlush(ticket3);

        ticket1.addLink(new IssueLink(ticket2.getId(), IssueLinkType.BLOCKS));
        ticket2.addLink(new IssueLink(ticket1.getId(), IssueLinkType.IS_BLOCKED_BY));
        ticket2.addLink(new IssueLink(ticket3.getId(), IssueLinkType.RELATES_TO));
        ticket3.addLink(new IssueLink(ticket2.getId(), IssueLinkType.RELATES_TO));

        ticketRepository.saveAllAndFlush(List.of(ticket1, ticket2, ticket3));
    }
}
