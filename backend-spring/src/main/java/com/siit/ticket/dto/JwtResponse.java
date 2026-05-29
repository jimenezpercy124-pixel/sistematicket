package com.siit.ticket.dto;

public class JwtResponse {
    private String token;
    private String username;
    private Long id;
    private String rol;
    private String codIre;
    private Integer especialistaNum;
    private Boolean debeCambiarPassword;

    public JwtResponse(String token, Long id, String username, String rol, String codIre, Integer especialistaNum, Boolean debeCambiarPassword) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.rol = rol;
        this.codIre = codIre;
        this.especialistaNum = especialistaNum;
        this.debeCambiarPassword = debeCambiarPassword;
    }

    // Getters
    public String getToken() { return token; }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getRol() { return rol; }
    public String getCodIre() { return codIre; }
    public Integer getEspecialistaNum() { return especialistaNum; }
    public Boolean getDebeCambiarPassword() { return debeCambiarPassword; }
}
