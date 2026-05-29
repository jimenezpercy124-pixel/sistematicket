package com.siit.ticket.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "TICKETS")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Ticket {

    @Id
    @Column(name = "ID_TICKET", length = 100)
    private String idTicket;

    @Column(name = "COD_IRE", length = 20)
    private String codIre;

    @Lob
    @Column(name = "ASUNTO", columnDefinition = "CLOB")
    private String asunto;

    @Column(name = "FECHA_CREACION")
    private LocalDateTime fechaCreacion;

    @Column(name = "ESTADO", length = 100)
    private String estado = "PENDIENTE";

    @Column(name = "DRIVEFOLDERID", length = 255)
    private String driveFolderId;

    @Column(name = "LINK_DRIVE", length = 500)
    private String linkDrive;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_INCIDENCIA")
    private Incidencia incidencia;

    @Column(name = "ESPECIALISTA_NUM")
    private Integer especialistaNum;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ESPECIALISTA_NUM", referencedColumnName = "ID_USUARIO", insertable = false, updatable = false)
    private User especialistaAsignado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_USUARIO_CREADOR", referencedColumnName = "ID_USUARIO")
    private User creador;

    @Column(name = "FECHA_RESOLUCION")
    private LocalDateTime fechaResolucion;

    @Column(name = "TOKEN_CONFIRMACION", length = 120)
    private String tokenConfirmacion;

    @Column(name = "FECHA_EXPIRACION_TOKEN")
    private LocalDateTime fechaExpiracionToken;

    @Column(name = "MOTIVO_RECHAZO", length = 500)
    private String motivoRechazo;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // Constructores
    public Ticket() {}

    // Getters y Setters
    public String getIdTicket() { return idTicket; }
    public void setIdTicket(String idTicket) { this.idTicket = idTicket; }
    public String getCodIre() { return codIre; }
    public void setCodIre(String codIre) { this.codIre = codIre; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDriveFolderId() { return driveFolderId; }
    public void setDriveFolderId(String driveFolderId) { this.driveFolderId = driveFolderId; }
    public String getLinkDrive() { return linkDrive; }
    public void setLinkDrive(String linkDrive) { this.linkDrive = linkDrive; }
    public Incidencia getIncidencia() { return incidencia; }
    public void setIncidencia(Incidencia incidencia) { this.incidencia = incidencia; }
    public Integer getEspecialistaNum() { return especialistaNum; }
    public void setEspecialistaNum(Integer especialistaNum) { this.especialistaNum = especialistaNum; }
    public User getEspecialistaAsignado() { return especialistaAsignado; }
    public void setEspecialistaAsignado(User especialistaAsignado) { this.especialistaAsignado = especialistaAsignado; }
    public User getCreador() { return creador; }
    public void setCreador(User creador) { this.creador = creador; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }
    public String getTokenConfirmacion() { return tokenConfirmacion; }
    public void setTokenConfirmacion(String tokenConfirmacion) { this.tokenConfirmacion = tokenConfirmacion; }
    public LocalDateTime getFechaExpiracionToken() { return fechaExpiracionToken; }
    public void setFechaExpiracionToken(LocalDateTime fechaExpiracionToken) { this.fechaExpiracionToken = fechaExpiracionToken; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
}
