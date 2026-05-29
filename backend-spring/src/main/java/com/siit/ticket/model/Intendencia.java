package com.siit.ticket.model;

import jakarta.persistence.*;

@Entity
@Table(name = "INTENDENCIAS")
public class Intendencia {

    @Id
    @Column(name = "COD_IRE", length = 50)
    private String codIre;

    @Column(name = "NOMBRE_IRE", nullable = false, length = 100)
    private String nombreIre;

    public Intendencia() {}

    public String getCodIre() { return codIre; }
    public void setCodIre(String codIre) { this.codIre = codIre; }
    public String getNombreIre() { return nombreIre; }
    public void setNombreIre(String nombreIre) { this.nombreIre = nombreIre; }
}
