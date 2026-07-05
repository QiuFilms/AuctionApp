package com.qiu.services;

import com.qiu.entities.Item;
import com.qiu.repositories.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    public List<Item> findAll(){
        return itemRepository.findAll();
    }
    
    public Optional<Item> findById(Long id){
        return itemRepository.findById(id);
    }
}
