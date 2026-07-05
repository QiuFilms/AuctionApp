package com.qiu.repositories;

import com.qiu.entities.ItemUser;
import com.qiu.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ItemUserRepository extends JpaRepository<ItemUser, Long> {
    List<ItemUser> findByUser(User user);
    List<ItemUser> getByUserIdAndItemId(long username, long itemId);
}