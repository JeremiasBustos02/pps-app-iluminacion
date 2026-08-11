package com.service;

import com.entity.Material;
import com.repository.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MaterialService {

    @Autowired
    private MaterialRepository materialRepository;

    public List<Material> findAll() {
        return materialRepository.findByDeletedAtIsNull();
    }

    public Optional<Material> findById(Long id) {
        return materialRepository.findById(id).filter(m -> m.getDeletedAt() == null);
    }

    public Material save(Material material) {
        return materialRepository.save(material);
    }

    public Material updateStock(Long id, Integer nuevaCantidad) {
        Material material = findById(id).orElseThrow(() -> new RuntimeException("Material no encontrado: " + id));
        material.setCantidad(nuevaCantidad);
        return materialRepository.save(material);
    }

    public void delete(Long id) {
        Material material = findById(id).orElseThrow(() -> new RuntimeException("Material no encontrado: " + id));
        material.setDeletedAt(LocalDateTime.now());
        materialRepository.save(material);
    }
}