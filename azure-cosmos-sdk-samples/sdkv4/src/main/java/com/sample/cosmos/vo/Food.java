package com.sample.cosmos.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

public class Food {
    private String id;
    private String description;
    private String manufacturerName;
    private List<Tag> tags;
    private String foodGroup;
    private List<Nutrient> nutrients;
    private List<Serving> servings;
    private String recipe;
    private BigDecimal nutritionValue;
    private BigDecimal amount;
    private BigDecimal weightInGrams;

    public BigDecimal getNutritionValue() {
        return nutritionValue;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getWeightInGrams() {
        return weightInGrams;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setNutritionValue(BigDecimal nutritionValue) {
        this.nutritionValue = nutritionValue;
    }

    public void setWeightInGrams(BigDecimal weightInGrams) {
        this.weightInGrams = weightInGrams;
    }

    public String getId() {
        return id;
    }

    public List<Serving> getServings() {
        return servings;
    }

    public void setServings(List<Serving> servings) {
        this.servings = servings;
    }

    public void addServing(Serving serving) {
        if (this.servings == null)
            this.servings = new ArrayList<Serving>();

        this.servings.add(serving);
    }

    public List<Nutrient> getNutrients() {
        return nutrients;
    }

    public void setNutrients(List<Nutrient> nutrients) {
        this.nutrients = nutrients;
    }

    public void addNutrient(Nutrient nutrient) {
        if (this.nutrients == null)
            this.nutrients = new ArrayList<Nutrient>();

        this.nutrients.add(nutrient);
    }

    public String getFoodGroup() {
        return foodGroup;
    }

    public void setFoodGroup(String foodGroup) {
        this.foodGroup = foodGroup;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void addTag(Tag tag) {
        if (this.tags == null)
            this.tags = new ArrayList<Tag>();

        this.tags.add(tag);
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getRecipe() {
        return recipe;
    }
}
