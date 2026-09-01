package com.security;

import com.entity.Usuario;
import com.enums.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService (RF-03): generación y validación de tokens")
class JwtServiceTest {

    private static final String SECRET = "ClaveSecretaDePruebaParaJwtDeAlMenos32Bytes!!";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L); // 1 hora
    }

    private Usuario usuario(long dni, Rol rol) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setDni(dni);
        u.setNombre("Juan Perez");
        u.setRol(rol);
        return u;
    }

    @Test
    @DisplayName("el token generado lleva el DNI como subject y puede volver a extraerse")
    void generaYExtraeDni() {
        String token = jwtService.generarToken(usuario(30111222L, Rol.VECINO));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extraerDni(token)).isEqualTo("30111222");
    }

    @Test
    @DisplayName("un token recién generado es válido")
    void tokenRecienGeneradoEsValido() {
        String token = jwtService.generarToken(usuario(30111222L, Rol.ADMINISTRADOR));

        assertThat(jwtService.esValido(token)).isTrue();
    }

    @Test
    @DisplayName("un texto que no es un JWT no es válido")
    void textoBasuraNoEsValido() {
        assertThat(jwtService.esValido("esto-no-es-un-token")).isFalse();
    }

    @Test
    @DisplayName("un token ya expirado no es válido")
    void tokenExpiradoNoEsValido() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1_000L); // ya vencido
        String token = jwtService.generarToken(usuario(30111222L, Rol.TECNICO));

        assertThat(jwtService.esValido(token)).isFalse();
    }

    @Test
    @DisplayName("un token firmado con otra clave no es válido")
    void tokenConOtraFirmaNoEsValido() {
        String token = jwtService.generarToken(usuario(30111222L, Rol.VECINO));

        JwtService otroServicio = new JwtService();
        ReflectionTestUtils.setField(otroServicio, "secret", "OtraClaveTotalmenteDistintaDeAlMenos32!!!");
        ReflectionTestUtils.setField(otroServicio, "expiration", 3_600_000L);

        assertThat(otroServicio.esValido(token)).isFalse();
    }
}
