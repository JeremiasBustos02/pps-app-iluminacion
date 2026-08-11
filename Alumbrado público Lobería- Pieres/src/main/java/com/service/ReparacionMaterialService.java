package com.service;

import com.entity.ReparacionMaterial;
import com.repository.ReparacionMaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReparacionMaterialService {

    @Autowired
    private ReparacionMaterialRepository reparacionMaterialRepository;

    public List<ReparacionMaterial> findByReparacion(Long reparacionId) {
        return reparacionMaterialRepository.findByReparacionId(reparacionId);
    }

    public ReparacionMaterial addMaterial(ReparacionMaterial reparacionMaterial) {
        return reparacionMaterialRepository.save(reparacionMaterial);
    }
}