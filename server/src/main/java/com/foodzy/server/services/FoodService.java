package com.foodzy.server.services;

import java.io.IOException;
import java.util.List;
import com.foodzy.server.models.Food;

public interface FoodService {
    public List<Food> getAllFood();

    public Food getFoodById(Long id) throws IOException;

    public Food createFood(Food food) throws IOException;

    public Food updateFood(Food food) throws IOException;

    public Food deleteFood(Food food) throws IOException;
}
