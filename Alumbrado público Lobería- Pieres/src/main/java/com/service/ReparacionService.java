package com.service;

import com.entity.Reparacion;
import com.repository.ReparacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReparacionService {

    @Autowired
    private ReparacionRepository reparacionRepository;

    public List<Reparacion> findAll() {
        return reparacionRepository.findAll();
    }

    public Optional<Reparacion> findById(Long id) {
        return reparacionRepository.findById(id);
    }

    public Reparacion save(Reparacion reparacion) {
        return reparacionRepository.save(reparacion);
    }

    public void delete(Long id) {
        reparacionRepository.deleteById(id);
    }
}