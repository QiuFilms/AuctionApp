package com.qiu.repositories;

import com.qiu.entities.ItemUser;
import com.qiu.entities.ItemUserKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ItemUserRepository extends JpaRepository<ItemUser, Long> {

    // Metoda pozwalająca znaleźć konkretną relację użytkownika z przedmiotem
    Optional<ItemUser> findByUserIdAndItemId(Long userId, Long itemId);
}