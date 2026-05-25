package com.qiu.entities;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Table(name = "items_user")
@Getter
@Setter
public class ItemUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unique_item_id") // Dopasowane do nazwy w schema.sql
    private Long id;

    // Relacje pozostają, ale nie potrzebujemy już @MapsId
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "on_auction")
    private Boolean onAuction = false;
}