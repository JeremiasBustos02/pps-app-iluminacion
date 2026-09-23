package com.service;

import com.entity.CuadrillaTecnico;
import com.entity.HojaDeRuta;
import com.repository.CuadrillaTecnicoRepository;
import com.repository.HojaDeRutaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class HojaDeRutaService {

    @Autowired
    private HojaDeRutaRepository hojaDeRutaRepository;
    @Autowired
    private CuadrillaTecnicoRepository cuadrillaTecnicoRepository;

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

    public List<HojaDeRuta> findMiHojaDelDia(Long usuarioId) {
        CuadrillaTecnico ct = cuadrillaTecnicoRepository.findByUsuarioId(usuarioId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("El técnico no pertenece a ninguna cuadrilla"));

        Long cuadrillaId = ct.getCuadrilla().getId();

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia = hoy.atTime(LocalTime.MAX);

        return hojaDeRutaRepository.findByCuadrillaIdAndFechaBetween(cuadrillaId, inicioDia, finDia);
    }
}