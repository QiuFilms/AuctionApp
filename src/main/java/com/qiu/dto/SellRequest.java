package com.qiu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellRequest {
    @JsonProperty("itemId")
    public Long itemId;

    @JsonProperty("userId")
    public Long userId;

    public double price;
    public int hours;
}
