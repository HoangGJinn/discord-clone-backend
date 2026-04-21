package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.NitroOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NitroOrderRepository extends JpaRepository<NitroOrder, Long>, JpaSpecificationExecutor<NitroOrder> {
    Optional<NitroOrder> findByVnpTxnRef(String vnpTxnRef);

    @Query("SELECT COALESCE(SUM(n.amount), 0) FROM NitroOrder n WHERE n.status = 'CONFIRMED'")
    Long sumConfirmedRevenue();

    Long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(n.amount), 0) FROM NitroOrder n WHERE n.status = 'CONFIRMED' AND n.createdAt >= :from AND n.createdAt <= :to")
    Long sumConfirmedRevenueByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    Page<NitroOrder> findByStatus(String status, Pageable pageable);

    List<NitroOrder> findByStatusOrderByCreatedAtDesc(String status);
}