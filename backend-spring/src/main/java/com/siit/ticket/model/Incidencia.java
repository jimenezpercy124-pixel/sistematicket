package com.siit.ticket.model;

import jakarta.persistence.*;

@Entity
@Table(name = "INCIDENCIAS")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_INCIDENCIA")
    private Long idIncidencia;

    @Column(name = "NOMBRE_INCIDENCIA", nullable = false, length = 255)
    private String nombreIncidencia;

    @Column(name = "ESPECIALISTA_NUM")
    private Integer especialistaNum;

    public Incidencia() {}

    public Long getIdIncidencia() { return idIncidencia; }
    public void setIdIncidencia(Long idIncidencia) { this.idIncidencia = idIncidencia; }
    public String getNombreIncidencia() { return nombreIncidencia; }
    public void setNombreIncidencia(String nombreIncidencia) { this.nombreIncidencia = nombreIncidencia; }
    public Integer getEspecialistaNum() { return especialistaNum; }
    public void setEspecialistaNum(Integer especialistaNum) { this.especialistaNum = especialistaNum; }
}
