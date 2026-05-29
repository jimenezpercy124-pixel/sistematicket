package com.siit.ticket.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "USUARIOS")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_USUARIO")
    private Long idUsuario;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    @Column(name = "EMAIL", length = 255)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_ROL", referencedColumnName = "ID_ROL")
    private Role role;

    @Column(name = "COD_IRE", length = 20)
    private String codIre;

    @Column(name = "ESPECIALISTA_NUM")
    private Integer especialistaNum;

    @Column(name = "DEBE_CAMBIAR_PASSWORD")
    private Boolean debeCambiarPassword = true;

    @Column(name = "ACTIVO")
    private Boolean activo = true;

    // Constructores
    public User() {}

    // Getters y Setters
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getCodIre() { return codIre; }
    public void setCodIre(String codIre) { this.codIre = codIre; }
    public Integer getEspecialistaNum() { return especialistaNum; }
    public void setEspecialistaNum(Integer especialistaNum) { this.especialistaNum = especialistaNum; }
    public Boolean getDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(Boolean debeCambiarPassword) { this.debeCambiarPassword = debeCambiarPassword; }
    public Boolean getActivo() { return activo == null ? true : activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
