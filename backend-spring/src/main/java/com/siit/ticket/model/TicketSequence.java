package com.siit.ticket.model;

import jakarta.persistence.*;

@Entity
@Table(name = "TICKET_SEQUENCES")
public class TicketSequence {

    @Id
    @Column(name = "COD_IRE", length = 50)
    private String codIre;

    @Column(name = "CURRENT_VALUE")
    private Long currentValue;

    public TicketSequence() {}

    public TicketSequence(String codIre, Long currentValue) {
        this.codIre = codIre;
        this.currentValue = currentValue;
    }

    public String getCodIre() { return codIre; }
    public void setCodIre(String codIre) { this.codIre = codIre; }
    public Long getCurrentValue() { return currentValue; }
    public void setCurrentValue(Long currentValue) { this.currentValue = currentValue; }
}
