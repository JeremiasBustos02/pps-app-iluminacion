package com.controller;

import com.dto.UsuarioResponseDTO;
import com.entity.Usuario;
import com.enums.Rol;
import com.security.JwtService;
import com.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController (RF-03): /api/auth/login y /api/auth/registro")
class AuthControllerTest {

    @Mock private UsuarioService usuarioService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    private Usuario usuario(Rol rol) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setDni(30111222L);
        u.setNombre("Juan Perez");
        u.setRol(rol);
        return u;
    }

    @Test
    @DisplayName("login con credenciales válidas devuelve 200 y un token con el rol")
    void loginOk() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("30111222", "secreta123"));
        when(usuarioService.findByDni(30111222L)).thenReturn(java.util.Optional.of(usuario(Rol.ADMINISTRADOR)));
        when(jwtService.generarToken(any(Usuario.class))).thenReturn("TOKEN123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":30111222,\"password\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("TOKEN123"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.dni").value(30111222L));
    }

    @Test
    @DisplayName("login con credenciales inválidas devuelve 401")
    void loginCredencialesInvalidas() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("credenciales"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":30111222,\"password\":\"malaClave\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("registro de vecino devuelve 201 y fuerza el rol VECINO")
    void registroVecino() throws Exception {
        when(usuarioService.saveFromDTO(any())).thenReturn(usuario(Rol.VECINO));
        when(usuarioService.toResponseDTO(any())).thenReturn(new UsuarioResponseDTO(usuario(Rol.VECINO)));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan Perez\",\"email\":\"juan@vecino.com\"," +
                                "\"password\":\"secreta123\",\"dni\":30111222,\"rol\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("VECINO"));
    }
}
