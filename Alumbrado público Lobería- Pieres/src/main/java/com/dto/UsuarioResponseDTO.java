package com.dto;

import com.entity.Usuario;
import com.enums.Rol;
import lombok.Data;

@Data
public class UsuarioResponseDTO {
    private Long id;
    private String nombre;
    private Rol rol;
    private String email;
    private Long dni;
    private String celular;
    private String calle;
    private Integer numeroCalle;
    private String referenciaDomicilio;

    public UsuarioResponseDTO(Usuario usuario) {
        if (usuario != null) {
            this.id = usuario.getId();
            this.nombre = usuario.getNombre();
            this.rol = usuario.getRol();
            this.email = usuario.getEmail();
            this.dni = usuario.getDni();
            this.celular = usuario.getCelular();
            this.calle = usuario.getCalle();
            this.numeroCalle = usuario.getNumeroCalle();
            this.referenciaDomicilio = usuario.getReferenciaDomicilio();
        }
    }
}
