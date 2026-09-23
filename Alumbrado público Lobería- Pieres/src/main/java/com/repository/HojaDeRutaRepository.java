package com.repository;

import com.entity.HojaDeRuta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HojaDeRutaRepository extends JpaRepository<HojaDeRuta, Long> {
    List<HojaDeRuta> findByCuadrillaId(Long cuadrillaId);

    List<HojaDeRuta> findByCuadrillaIdAndFechaBetween(Long cuadrillaId, LocalDateTime desde, LocalDateTime hasta);
}
