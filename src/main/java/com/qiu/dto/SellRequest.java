package com.qiu.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellRequest {
    private Long itemId;
    private double price;
    private int hours;
}
