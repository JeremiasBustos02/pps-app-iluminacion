package com.service;

import com.entity.CuadrillaTecnico;
import com.repository.CuadrillaTecnicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CuadrillaTecnicoService {

    @Autowired
    private CuadrillaTecnicoRepository cuadrillaTecnicoRepository;

    public List<CuadrillaTecnico> findByCuadrilla(Long cuadrillaId) {
        return cuadrillaTecnicoRepository.findByCuadrillaId(cuadrillaId);
    }

    public CuadrillaTecnico assignTecnico(CuadrillaTecnico cuadrillaTecnico) {
        return cuadrillaTecnicoRepository.save(cuadrillaTecnico);
    }

    public void removeTecnico(Long id) {
        cuadrillaTecnicoRepository.deleteById(id);
    }
}