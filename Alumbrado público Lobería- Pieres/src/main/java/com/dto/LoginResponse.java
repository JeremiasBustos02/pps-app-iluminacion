package com.dto;

import com.entity.Usuario;
import com.enums.Rol;
import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private String tipo = "Bearer";
    private Long id;
    private String nombre;
    private Long dni;
    private Rol rol;

    public LoginResponse(String token, Usuario usuario) {
        this.token = token;
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
        this.dni = usuario.getDni();
        this.rol = usuario.getRol();
    }
}
