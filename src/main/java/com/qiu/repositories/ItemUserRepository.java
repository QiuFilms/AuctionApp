package com.qiu.repositories;

import com.qiu.entities.ItemUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ItemUserRepository extends JpaRepository<ItemUser, Long> {

}