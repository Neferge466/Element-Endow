package com.element_endow.reaction.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class ReactionEntry {
    private final List<String> matchElements;
    private final double rate;
    private final double[] rateArray;
    private final ReactionEffect effect;
    private final ReactionType type;

    public ReactionEntry(JsonObject json, ReactionType configType) {
        this.type = configType;
        this.matchElements = new ArrayList<>();

        if (json.has("match") && json.get("match").isJsonArray()) {
            JsonArray matchArray = json.get("match").getAsJsonArray();
            for (JsonElement element : matchArray) {
                matchElements.add(element.getAsString());
            }
        } else if (json.has("two_match") && json.get("two_match").isJsonArray()) {
            JsonArray twoMatchArray = json.get("two_match").getAsJsonArray();
            for (JsonElement element : twoMatchArray) {
                matchElements.add(element.getAsString());
            }
        }

        if (type == ReactionType.INTERNAL) {
            this.rate = json.get("rate").getAsDouble();
            this.rateArray = null;
        } else {
            JsonArray rateArrayJson = json.getAsJsonArray("rate");
            this.rateArray = new double[]{rateArrayJson.get(0).getAsDouble(), rateArrayJson.get(1).getAsDouble()};
            this.rate = 0.0;
        }

        JsonObject effectObj = json.getAsJsonObject("effect");
        this.effect = new ReactionEffect(effectObj.get("afflict"), effectObj.get("empower"));
    }

    public List<String> getMatchElements() { return matchElements; }
    public double getRate() { return rate; }
    public double[] getRateArray() { return rateArray; }
    public ReactionEffect getEffect() { return effect; }
    public ReactionType getType() { return type; }
}