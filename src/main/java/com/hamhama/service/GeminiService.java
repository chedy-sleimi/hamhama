package com.hamhama.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamhama.dto.SubstituteDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeminiService {
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final String PROMPT = """
You are an FDA-compliant nutrition label generator. Output MUST be pure SVG with perfect layout.

CRITICAL FIXES:
1. Expanded viewBox: "0 0 350 650" (wider and taller)
2. Right-aligned numbers at x="330" (not 280)
3. Left text starts at x="20" (not 15)
4. Line spacing: 22px between nutrient rows
5. Dynamic height calculation based on content

STRICT TEMPLATE:
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 350 650">
  <!-- Main border -->
  <rect x="0" y="0" width="350" height="650" fill="white" stroke="black" stroke-width="2"/>
  
  <!-- Header Section -->
  <text x="50%" y="40" font-family="Arial Black" font-size="28" font-weight="bold" text-anchor="middle">Nutrition Facts</text>
  <line x1="20" y1="50" x2="330" y2="50" stroke="black" stroke-width="3"/>
  
  <!-- Serving Info -->
  <text x="20" y="80" font-family="Arial" font-size="16">8 servings per container</text>
  <text x="20" y="110" font-family="Arial" font-size="18" font-weight="bold">Serving size</text>
  <text x="330" y="110" font-family="Arial" font-size="18" text-anchor="end">1 cup (228g)</text>
  
  <!-- Calories Banner -->
  <rect x="0" y="125" width="350" height="30" fill="black"/>
  <text x="20" y="147" font-family="Arial" font-size="16" fill="white">Amount per serving</text>
  
  <!-- Calories Value -->
  <text x="20" y="190" font-family="Arial" font-size="36" font-weight="bold">Calories</text>
  <text x="330" y="190" font-family="Arial" font-size="36" text-anchor="end">250</text>
  <line x1="20" y1="200" x2="330" y2="200" stroke="black" stroke-width="3"/>
  
  <!-- Daily Value Header -->
  <text x="330" y="230" font-family="Arial" font-size="16" font-weight="bold" text-anchor="end">% Daily Value*</text>
  <line x1="20" y1="240" x2="330" y2="240" stroke="black" stroke-width="2"/>
  
  <!-- Nutrient List (22px vertical spacing) -->
  <text x="20" y="265" font-family="Arial" font-size="18" font-weight="bold">Total Fat</text>
  <text x="250" y="265" font-family="Arial" font-size="18" text-anchor="end">14g</text>
  <text x="330" y="265" font-family="Arial" font-size="18" text-anchor="end">18%</text>
  <line x1="20" y1="275" x2="330" y2="275" stroke="black" stroke-width="0.5"/>
  
  <!-- Sub-item (indented 10px) -->
  <text x="30" y="297" font-family="Arial" font-size="17">Saturated Fat</text>
  <text x="250" y="297" font-family="Arial" font-size="17" text-anchor="end">1g</text>
  <text x="330" y="297" font-family="Arial" font-size="17" text-anchor="end">5%</text>
  <line x1="20" y1="307" x2="330" y2="307" stroke="black" stroke-width="0.5"/>
  
  <!-- Continue nutrients with y+=22 each row -->
  
  <!-- Bottom spacing buffer -->
  <rect x="0" y="620" width="350" height="30" fill="white"/> 
</svg>

MANDATORY LAYOUT RULES:
1. Three-column layout:
   - Nutrient names: x=20 (x=30 for sub-items)
   - Quantities: x=250 (right-aligned)
   - %DV: x=330 (right-aligned)
2. Minimum 22px vertical spacing between lines
3. Section spacing:
   - 40px after header
   - 35px after calories
   - 25px after DV header
4. Font sizes:
   - Header: 28pt
   - Calories: 36pt
   - Nutrients: 18pt
   - Sub-items: 17pt
5. 30px bottom buffer to prevent overflow
6. All horizontal lines span 310px (20-330)
7. Text must not exceed 200px width:
   - Truncate long names with ellipsis
   - Stack text if absolutely necessary

PROHIBITED:
- Any text overlapping
- Elements exceeding 650px height
- Lines exceeding column boundaries
- Font sizes deviating from specification
- Spacing less than 22px between rows
""";
    @Value("${gemini.api.key}")
    private String apiKey;

    // Cache to store results for each recipe
    private final ConcurrentHashMap<String, String> nutritionCache = new ConcurrentHashMap<>();

    public String generateNutritionLabel(String nutritionData) throws Exception {
        // Create a normalized version of the input to use as a cache key
        // Parse the JSON to normalize it (removes whitespace differences, etc.)
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(nutritionData);
        String normalizedInput = jsonNode.toString();

        // Check if we already have results for this exact recipe
        if (nutritionCache.containsKey(normalizedInput)) {
            System.out.println("Cache hit: Using cached nutrition facts");
            return nutritionCache.get(normalizedInput);
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Use a consistent seed/temperature to reduce variation in responses
        Map<String, Object> requestMap = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.0); // Use deterministic output

        parts.add(Map.of("text", PROMPT + "\nInput JSON:\n" + nutritionData));
        content.put("parts", parts);
        requestMap.put("contents", List.of(content));
        requestMap.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestMap, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GEMINI_URL + "?key=" + apiKey,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode root = new ObjectMapper().readTree(response.getBody());
            if (!root.path("candidates").isArray() || root.path("candidates").isEmpty()) {
                throw new RuntimeException("No candidates in response: " + response.getBody());
            }

            String svgContent = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            String cleanedSvg = cleanSvgContent(svgContent);

            // Cache the result for future use
            nutritionCache.put(normalizedInput, cleanedSvg);

            return cleanedSvg;
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Gemini API error: " + e.getResponseBodyAsString(), e);
        }
    }

    private String cleanSvgContent(String svgContent) {
        // Remove ```svg and ``` markdown if present
        svgContent = svgContent
                .replaceAll("```xml", "")
                .replaceAll("```svg", "")
                .replaceAll("```", "")
                .trim();

        return svgContent.trim();
    }

    // Add new method for substitutions
    public SubstituteDTO getSubstitutes(String ingredient) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Declare jsonResponse outside try block for error handling
        String jsonResponse = null;

        try {
            String formattedPrompt = String.format("""
            Generate ingredient substitutes in STRICT JSON format:
            {
                "original": "%s",
                "substitutes": [
                    {"name": "Sub1", "reason": "Reason1"},
                    {"name": "Sub2", "reason": "Reason2"}
                ]
            }
            No other text or formatting! Only valid JSON!
            """, ingredient);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(Map.of("text", formattedPrompt)));
            requestBody.put("contents", List.of(content));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    GEMINI_URL + "?key=" + apiKey,
                    HttpMethod.POST,
                    entity,
                    String.class
            );


            System.out.println("=== RAW RESPONSE ===");
            System.out.println(response.getBody());

            JsonNode root = new ObjectMapper().readTree(response.getBody());
            JsonNode candidates = root.path("candidates");

            if (candidates.isEmpty() || !candidates.isArray() || candidates.get(0) == null) {
                throw new RuntimeException("No valid candidates in response");
            }

            jsonResponse = candidates.get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // Enhanced cleaning
            jsonResponse = jsonResponse
                    .replaceAll("(?i)```json", "") // Case-insensitive replacement
                    .replaceAll("```", "")
                    .replaceAll("(?s)^.*?\\{", "{") // Remove everything before first {
                    .replaceAll("(?s)\\}[^}]*$", "}") // Remove everything after last }
                    .replaceAll("^\\s+|\\s+$", "") // Trim whitespace
                    .trim();
            System.out.println("=== CLEANED JSON ===\n" + jsonResponse);

            // Validate JSON structure
            try {
                new ObjectMapper().readTree(jsonResponse);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Invalid JSON: " + jsonResponse);
            }

            System.out.println("Cleaned JSON: " + jsonResponse);

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            return mapper.readValue(jsonResponse, SubstituteDTO.class);

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Gemini API request failed: " + e.getStatusCode()
                    + " - " + e.getResponseBodyAsString());
        } catch (JsonProcessingException e) {
            // Include jsonResponse in error message
            String errorJson = jsonResponse != null ? jsonResponse : "No JSON content available";
            throw new RuntimeException("Failed to parse JSON response: " + e.getOriginalMessage()
                    + "\nJSON: " + errorJson);
        }
    }

}