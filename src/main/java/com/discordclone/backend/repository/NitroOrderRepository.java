package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.NitroOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface NitroOrderRepository extends JpaRepository<NitroOrder, Long> {
    Optional<NitroOrder> findByVnpTxnRef(String vnpTxnRef);
    @Query("SELECT COALESCE(SUM(n.amount), 0) FROM NitroOrder n WHERE n.status = 'CONFIRMED'")
    Long sumConfirmedRevenue();
}