package com.siit.ticket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TICKET_LOGS")
public class TicketLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_LOG")
    private Long idLog;

    @Column(name = "ID_TICKET", length = 100)
    private String idTicket;

    @Column(name = "ESTADO_ANTERIOR", length = 100)
    private String estadoAnterior;

    @Column(name = "ESTADO_NUEVO", length = 100)
    private String estadoNuevo;

    @Column(name = "USUARIO", length = 100)
    private String usuario;

    @Column(name = "FECHA")
    private LocalDateTime fecha;

    @Lob
    @Column(name = "DETALLES", columnDefinition = "CLOB")
    private String detalles;

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
    }

    public TicketLog() {}

    public TicketLog(String idTicket, String estadoAnterior, String estadoNuevo, String usuario, String detalles) {
        this.idTicket = idTicket;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuario = usuario;
        this.detalles = detalles;
    }

    // Getters y Setters
    public Long getIdLog() { return idLog; }
    public void setIdLog(Long idLog) { this.idLog = idLog; }

    public String getIdTicket() { return idTicket; }
    public void setIdTicket(String idTicket) { this.idTicket = idTicket; }

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getDetalles() { return detalles; }
    public void setDetalles(String detalles) { this.detalles = detalles; }
}
