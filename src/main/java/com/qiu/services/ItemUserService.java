package com.qiu.services;

import com.qiu.entities.ItemUser;
import com.qiu.repositories.ItemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ItemUserService {

    @Autowired
    private ItemUserRepository itemUserRepository;

    public void delete(ItemUser itemUser){
        itemUserRepository.delete(itemUser);
    }
}
