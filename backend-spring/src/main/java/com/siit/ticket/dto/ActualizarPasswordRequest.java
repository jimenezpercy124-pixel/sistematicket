package com.siit.ticket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ActualizarPasswordRequest {
    
    @JsonProperty("nuevaPassword")
    private String nuevaPassword;

    public ActualizarPasswordRequest() {}

    public String getNuevaPassword() {
        return nuevaPassword;
    }

    public void setNuevaPassword(String nuevaPassword) {
        this.nuevaPassword = nuevaPassword;
    }
}
