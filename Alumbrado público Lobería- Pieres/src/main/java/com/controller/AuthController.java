package com.controller;

import com.dto.UsuarioDTO;
import com.dto.UsuarioResponseDTO;
import com.entity.Usuario;
import com.enums.Rol;
import com.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    // Endpoint PÚBLICO para que cualquier vecino se registre
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registrarVecino(@RequestBody UsuarioDTO dto) {
        // Forzamos que el rol siempre sea VECINO en el autoregistro
        dto.setRol(Rol.VECINO);
        Usuario nuevoUsuario = usuarioService.saveFromDTO(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UsuarioResponseDTO(nuevoUsuario));
    }
}