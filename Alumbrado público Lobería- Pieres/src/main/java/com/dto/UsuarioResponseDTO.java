package com.dto;

import com.entity.Cuadrilla;
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

    // Cuadrilla asignada (solo aplica a los tecnicos, RF-04)
    private Long cuadrillaId;
    private String cuadrillaNombre;

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

    public UsuarioResponseDTO(Usuario usuario, Cuadrilla cuadrilla) {
        this(usuario);
        if (cuadrilla != null) {
            this.cuadrillaId = cuadrilla.getId();
            this.cuadrillaNombre = cuadrilla.getNombre();
        }
    }
}
