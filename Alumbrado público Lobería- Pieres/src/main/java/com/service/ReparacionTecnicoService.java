package com.service;

import com.entity.ReparacionTecnico;
import com.repository.ReparacionTecnicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReparacionTecnicoService {

    @Autowired
    private ReparacionTecnicoRepository reparacionTecnicoRepository;

    public List<ReparacionTecnico> findByReparacion(Long reparacionId) {
        return reparacionTecnicoRepository.findByReparacionId(reparacionId);
    }

    public ReparacionTecnico assignTecnico(ReparacionTecnico reparacionTecnico) {
        return reparacionTecnicoRepository.save(reparacionTecnico);
    }
}
