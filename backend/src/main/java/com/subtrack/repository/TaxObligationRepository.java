package com.subtrack.repository;

import com.subtrack.model.TaxObligation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxObligationRepository extends JpaRepository<TaxObligation, Long> {
    List<TaxObligation> findByUserId(Long userId);

}