package com.foodzy.server.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Food {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private Double rating;
    private byte[] imageData;
    private String category; // Veg, Non-Veg, Desserts
}
