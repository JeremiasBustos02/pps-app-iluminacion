package com.service;

import com.dto.CuadrillaTecnicoRequest;
import com.entity.Cuadrilla;
import com.entity.CuadrillaTecnico;
import com.entity.Usuario;
import com.repository.CuadrillaRepository;
import com.repository.CuadrillaTecnicoRepository;
import com.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CuadrillaTecnicoService {

    @Autowired
    private CuadrillaTecnicoRepository cuadrillaTecnicoRepository;
    @Autowired
    private CuadrillaRepository cuadrillaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<CuadrillaTecnico> findByCuadrilla(Long cuadrillaId) {
        return cuadrillaTecnicoRepository.findByCuadrillaId(cuadrillaId);
    }

    public CuadrillaTecnico assignTecnico(CuadrillaTecnicoRequest cuadrillaTecnico) {
        Cuadrilla cuadrilla = cuadrillaRepository.findById(cuadrillaTecnico.getCuadrillaId())
                .orElseThrow(() -> new RuntimeException(
                        "Cuadrilla no encontrada: " + cuadrillaTecnico.getCuadrillaId()));
        Usuario tecnico = usuarioRepository.findById(cuadrillaTecnico.getTecnicoId())
                .orElseThrow(() -> new RuntimeException(
                        "Tecnico no encontrado: " + cuadrillaTecnico.getTecnicoId()));

        if (cuadrillaTecnicoRepository
                .findByCuadrillaIdAndUsuarioId(
                        cuadrillaTecnico.getCuadrillaId(),
                        cuadrillaTecnico.getTecnicoId()
                ).isPresent()) {

            throw new RuntimeException("El técnico ya pertenece a esta cuadrilla");
        }

        CuadrillaTecnico asignacion = new CuadrillaTecnico();
        asignacion.setCuadrilla(cuadrilla);
        asignacion.setUsuario(tecnico);

        return cuadrillaTecnicoRepository.save(asignacion);
    }

    public List<CuadrillaTecnico> findByUsuario(Long usuarioId) {
        return cuadrillaTecnicoRepository.findByUsuarioId(usuarioId);
    }

    public void removeTecnico(Long id) {
        cuadrillaTecnicoRepository.deleteById(id);
    }
}