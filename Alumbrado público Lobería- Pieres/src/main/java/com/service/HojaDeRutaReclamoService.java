package com.service;

import com.entity.HojaDeRuta;
import com.entity.HojaDeRutaReclamo;
import com.entity.Reclamo;
import com.repository.HojaDeRutaReclamoRepository;
import com.repository.HojaDeRutaRepository;
import com.repository.ReclamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HojaDeRutaReclamoService {

    @Autowired
    private HojaDeRutaReclamoRepository hojaDeRutaReclamoRepository;

    @Autowired
    private HojaDeRutaRepository hojaDeRutaRepository;

    @Autowired
    private ReclamoRepository reclamoRepository;

    public List<HojaDeRutaReclamo> findByHojaDeRuta(Long hojaDeRutaId) {
        return hojaDeRutaReclamoRepository.findByHojaDeRutaId(hojaDeRutaId);
    }

    public HojaDeRutaReclamo addReclamoToHoja(HojaDeRutaReclamo hojaDeRutaReclamo) {
        return hojaDeRutaReclamoRepository.save(hojaDeRutaReclamo);
    }

    public void removeReclamoFromHoja(Long id) {
        hojaDeRutaReclamoRepository.deleteById(id);
    }

    public List<HojaDeRutaReclamo> addReclamosBatch(Long hojaDeRutaId, List<Long> reclamoIds) {
        HojaDeRuta hoja = hojaDeRutaRepository.findById(hojaDeRutaId)
                .orElseThrow(() -> new RuntimeException("Hoja de ruta no encontrada: " + hojaDeRutaId));

        List<HojaDeRutaReclamo> creados = new ArrayList<>();

        for (Long reclamoId : reclamoIds) {
            Reclamo reclamo = reclamoRepository.findById(reclamoId)
                    .orElseThrow(() -> new RuntimeException("Reclamo no encontrado: " + reclamoId));

            if (hojaDeRutaReclamoRepository.findByHojaDeRutaIdAndReclamoId(hojaDeRutaId, reclamoId).isPresent()) {
                continue;
            }

            HojaDeRutaReclamo hdr = new HojaDeRutaReclamo();
            hdr.setHojaDeRuta(hoja);
            hdr.setReclamo(reclamo);
            creados.add(hojaDeRutaReclamoRepository.save(hdr));
        }

        return creados;
    }
}