package com.qiu.entities;

import lombok.*;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "auction_history")
public class AuctionHistory implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "auction_id")
    private Auction auction;


    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "event_type")
    private String eventType;


    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;

}
