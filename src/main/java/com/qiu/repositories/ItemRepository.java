package com.qiu.repositories;

import com.qiu.entities.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(
            value      = "DELETE FROM items_user WHERE item_id = :itemId",
            countQuery = "SELECT COUNT(*) FROM items_user WHERE item_id = :itemId", // ← TO JEST FIX
            nativeQuery = true
    )
    void deleteItemUserRelation(@Param("itemId") Long itemId);
}