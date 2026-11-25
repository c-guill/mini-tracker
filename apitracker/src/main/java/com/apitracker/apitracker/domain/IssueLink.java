package com.apitracker.apitracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "issue_links",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_ticket_id", "target_ticket_id"})
)
public class IssueLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_ticket_id", nullable = false)
    private Ticket sourceTicket;

    @Column(name = "target_ticket_id", nullable = false)
    private Long targetTicketId;

    @Convert(converter = IssueLinkTypeConverter.class)
    @Column(name = "link_type", nullable = false, length = 30)
    private IssueLinkType type;

    public IssueLink() {
    }

    public IssueLink(Long targetTicketId, IssueLinkType type) {
        this.targetTicketId = targetTicketId;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ticket getSourceTicket() {
        return sourceTicket;
    }

    public void setSourceTicket(Ticket sourceTicket) {
        this.sourceTicket = sourceTicket;
    }

    public Long getTargetTicketId() {
        return targetTicketId;
    }

    public void setTargetTicketId(Long targetTicketId) {
        this.targetTicketId = targetTicketId;
    }

    public IssueLinkType getType() {
        return type;
    }

    public void setType(IssueLinkType type) {
        this.type = type;
    }
}
