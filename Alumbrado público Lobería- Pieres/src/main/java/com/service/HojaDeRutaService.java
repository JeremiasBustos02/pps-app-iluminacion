package com.service;

import com.entity.HojaDeRuta;
import com.repository.HojaDeRutaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HojaDeRutaService {

    @Autowired
    private HojaDeRutaRepository hojaDeRutaRepository;

    public List<HojaDeRuta> findAll() {
        return hojaDeRutaRepository.findAll();
    }

    public Optional<HojaDeRuta> findById(Long id) {
        return hojaDeRutaRepository.findById(id);
    }

    public HojaDeRuta save(HojaDeRuta hojaDeRuta) {
        return hojaDeRutaRepository.save(hojaDeRuta);
    }

    public void delete(Long id) {
        hojaDeRutaRepository.deleteById(id);
    }
}