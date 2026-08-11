package com.service;

import com.entity.TipoReclamo;
import com.repository.TipoReclamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TipoReclamoService {

    @Autowired
    private TipoReclamoRepository tipoReclamoRepository;

    public List<TipoReclamo> findAll() {
        return tipoReclamoRepository.findAll();
    }

    public Optional<TipoReclamo> findById(Long id) {
        return tipoReclamoRepository.findById(id);
    }

    public TipoReclamo save(TipoReclamo tipoReclamo) {
        return tipoReclamoRepository.save(tipoReclamo);
    }

    public void delete(Long id) {
        tipoReclamoRepository.deleteById(id);
    }
}