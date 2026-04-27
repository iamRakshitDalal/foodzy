package com.foodzy.server.controllers;

import com.foodzy.server.models.Food;
import com.foodzy.server.services.AIService;
import com.foodzy.server.services.FoodService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/food")
@CrossOrigin(origins = "http://localhost:3000", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS })
public class FoodController {

    @Autowired
    private FoodService foodService;

    @Autowired
    private AIService aiService;

    // GET /api/food  → all foods
    @GetMapping
    public List<Food> getAllFood() {
        return foodService.getAllFood();
    }

    // GET /api/food/{id}  → single food
    @GetMapping("/{id}")
    public Food getFoodById(@PathVariable Long id) throws IOException {
        return foodService.getFoodById(id);
    }

    // POST /api/food  → create (JSON body)
    @PostMapping
    public ResponseEntity<Food> createFood(@RequestBody Food food) throws IOException {
        if (food.getImageData() != null && food.getImageData().length > 0) {
            if (isDescriptionMissing(food.getDescription())) {
                try {
                    food.setDescription(aiService.generateDescription(food.getImageData()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ResponseEntity.ok(foodService.createFood(food));
    }

    // PUT /api/food/{id}  → update (JSON body)
    @PutMapping("/{id}")
    public ResponseEntity<Food> updateFood(@PathVariable Long id, @RequestBody Food food) throws IOException {
        food.setId(id);
        if (food.getImageData() != null && food.getImageData().length > 0) {
            if (isDescriptionMissing(food.getDescription())) {
                try {
                    food.setDescription(aiService.generateDescription(food.getImageData()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ResponseEntity.ok(foodService.updateFood(food));
    }

    // DELETE /api/food/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Food> deleteFood(@PathVariable Long id) throws IOException {
        Food food = foodService.getFoodById(id);
        return ResponseEntity.ok(foodService.deleteFood(food));
    }

    // POST /api/food/ai-description  → generate description from uploaded file
    @PostMapping("/ai-description")
    public ResponseEntity<String> getAIDescription(@RequestParam("image") MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body("Image is required");
        }
        String description = aiService.generateDescription(image.getBytes());
        return ResponseEntity.ok(description);
    }

    private boolean isDescriptionMissing(String desc) {
        return desc == null || desc.trim().isEmpty() || desc.equals("Freshly prepared...");
    }
}
