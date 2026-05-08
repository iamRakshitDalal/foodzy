package com.foodzy.server.services;

import java.util.Map;

public interface AIService {
    public String generateDescription(byte[] imageBytes);

    public String detectMimeType(byte[] bytes);

}