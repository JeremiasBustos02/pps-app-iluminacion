package com.repository;

import com.entity.HojaDeRutaReclamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface HojaDeRutaReclamoRepository extends JpaRepository<HojaDeRutaReclamo, Long> {
    List<HojaDeRutaReclamo> findByHojaDeRutaId(Long hojaDeRutaId);
    List<HojaDeRutaReclamo> findByReclamoId(Long reclamoId);
    Optional<HojaDeRutaReclamo> findByHojaDeRutaIdAndReclamoId(Long hojaDeRutaId, Long reclamoId);

}