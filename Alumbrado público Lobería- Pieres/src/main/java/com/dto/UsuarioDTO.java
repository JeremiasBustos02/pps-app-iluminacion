package com.dto;

import com.enums.Rol;
import lombok.Data;

@Data
public class UsuarioDTO {
    private String nombre;
    private Rol rol;
    private String email;
    private String password;
    private Long dni;
    private String celular;
    private String calle;
    private Integer numeroCalle;
    private String referenciaDomicilio;
}
