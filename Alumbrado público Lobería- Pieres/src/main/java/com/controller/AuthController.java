package com.controller;

import com.dto.LoginRequest;
import com.dto.LoginResponse;
import com.dto.UsuarioDTO;
import com.dto.UsuarioResponseDTO;
import com.entity.Usuario;
import com.enums.Rol;
import com.security.JwtService;
import com.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    // Endpoint PUBLICO para que cualquier vecino se registre (RF-01)
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registrarVecino(@RequestBody UsuarioDTO dto) {
        // Forzamos que el rol siempre sea VECINO en el autoregistro
        dto.setRol(Rol.VECINO);
        dto.setCuadrillaId(null); // un vecino nunca pertenece a una cuadrilla
        Usuario nuevoUsuario = usuarioService.saveFromDTO(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.toResponseDTO(nuevoUsuario));
    }

    // Endpoint PUBLICO de inicio de sesion con DNI y contrasena (RF-03)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            String.valueOf(request.getDni()), request.getPassword()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("DNI o contrasena incorrectos");
        }

        Usuario usuario = usuarioService.findByDni(request.getDni())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + request.getDni()));

        String token = jwtService.generarToken(usuario);
        return ResponseEntity.ok(new LoginResponse(token, usuario));
    }
}
