package com.service;

import com.entity.HojaDeRutaReclamo;
import com.repository.HojaDeRutaReclamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HojaDeRutaReclamoService {

    @Autowired
    private HojaDeRutaReclamoRepository hojaDeRutaReclamoRepository;

    public List<HojaDeRutaReclamo> findByHojaDeRuta(Long hojaDeRutaId) {
        return hojaDeRutaReclamoRepository.findByHojaDeRutaId(hojaDeRutaId);
    }

    public HojaDeRutaReclamo addReclamoToHoja(HojaDeRutaReclamo hojaDeRutaReclamo) {
        return hojaDeRutaReclamoRepository.save(hojaDeRutaReclamo);
    }

    public void removeReclamoFromHoja(Long id) {
        hojaDeRutaReclamoRepository.deleteById(id);
    }
}