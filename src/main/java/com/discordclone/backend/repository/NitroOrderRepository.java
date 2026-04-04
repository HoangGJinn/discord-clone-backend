package com.discordclone.backend.repository;

import com.discordclone.backend.entity.jpa.NitroOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NitroOrderRepository extends JpaRepository<NitroOrder, Long> {
    Optional<NitroOrder> findByVnpTxnRef(String vnpTxnRef);
}