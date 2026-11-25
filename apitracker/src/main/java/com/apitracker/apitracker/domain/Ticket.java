package com.apitracker.apitracker.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority = TicketPriority.LOW;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "due_date", length = 10)
    private String dueDate;

    @Column(name = "assignee_id", length = 50)
    private String assigneeId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ticket_labels", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "label", nullable = false, length = 50)
    @Convert(converter = TicketLabelConverter.class)
    private List<TicketLabel> labels = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC, id ASC")
    private List<TicketComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp DESC, id DESC")
    private List<TicketHistoryItem> history = new ArrayList<>();

    @OneToMany(mappedBy = "sourceTicket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<IssueLink> links = new ArrayList<>();

    public Ticket() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public List<TicketLabel> getLabels() {
        return labels;
    }

    public void setLabels(List<TicketLabel> labels) {
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
    }

    public List<TicketComment> getComments() {
        return comments;
    }

    public void setComments(List<TicketComment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
    }

    public List<TicketHistoryItem> getHistory() {
        return history;
    }

    public void setHistory(List<TicketHistoryItem> history) {
        this.history = history != null ? history : new ArrayList<>();
    }

    public List<IssueLink> getLinks() {
        return links;
    }

    public void setLinks(List<IssueLink> links) {
        this.links = links != null ? links : new ArrayList<>();
    }

    public void addComment(TicketComment comment) {
        comments.add(comment);
        comment.setTicket(this);
    }

    public void removeComment(TicketComment comment) {
        comments.remove(comment);
        comment.setTicket(null);
    }

    public void addHistoryItem(TicketHistoryItem item) {
        history.add(0, item);
        item.setTicket(this);
    }

    public void addLink(IssueLink link) {
        links.add(link);
        link.setSourceTicket(this);
    }

    public void removeLink(IssueLink link) {
        links.remove(link);
        link.setSourceTicket(null);
    }
}
