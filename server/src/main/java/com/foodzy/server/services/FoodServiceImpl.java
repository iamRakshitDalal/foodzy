package com.foodzy.server.services;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.foodzy.server.entitys.FoodEntity;
import com.foodzy.server.models.Food;
import com.foodzy.server.repositories.FoodRepository;

@Service
public class FoodServiceImpl implements FoodService {
    @Autowired
    private FoodRepository foodRepository;

    @Override
    public List<Food> getAllFood() {
        return foodRepository.findAll()
                .stream()
                .map(entity -> {
                    Food food = new Food();
                    BeanUtils.copyProperties(entity, food);
                    return food;
                })
                .toList();
    }

    @Override
    public Food getFoodById(Long id) throws IOException {
        FoodEntity foodEntity = foodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food not found with id: " + id));

        Food food = new Food();
        BeanUtils.copyProperties(foodEntity, food);
        return food;
    }

    @Override
    public Food createFood(Food food) throws IOException {
        FoodEntity foodEntity = new FoodEntity();
        BeanUtils.copyProperties(food, foodEntity);
        foodRepository.save(foodEntity);
        return food;
    }

    @Override
    public Food updateFood(Food food) throws IOException {
        FoodEntity foodEntity = foodRepository.findById(food.getId())
                .orElseThrow(() -> new RuntimeException("Food not found"));
        foodEntity.setName(food.getName());
        foodEntity.setDescription(food.getDescription());
        foodEntity.setPrice(food.getPrice());
        foodEntity.setRating(food.getRating());
        foodEntity.setImageData(food.getImageData());
        foodEntity.setCategory(food.getCategory());
        foodRepository.save(foodEntity);
        return food;

    }

    @Override
    public Food deleteFood(Food food) throws IOException {
        FoodEntity foodEntity = foodRepository.findById(food.getId())
                .orElseThrow(() -> new RuntimeException("Food not found"));
        foodRepository.delete(foodEntity);
        return food;
    }

}
