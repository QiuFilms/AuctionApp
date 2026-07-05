package com.qiu.entities;

import lombok.*;


import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;


    @Column(name = "avatar", columnDefinition = "bytea")
    private byte[] avatar;

    @Column(name = "avatar_content_type", length = 50)
    private String avatarContentType;

    @Id
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "wallet",  nullable = false)
    private float wallet;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ItemUser> items = new ArrayList<>();

}
