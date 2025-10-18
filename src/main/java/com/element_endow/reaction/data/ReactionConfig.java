package com.element_endow.reaction.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class ReactionConfig {
    private final String key;
    private final ReactionType type;
    private final List<ReactionEntry> reactions;

    public ReactionConfig(JsonObject json) {
        this.key = json.get("key").getAsString();
        this.type = ReactionType.valueOf(json.get("type").getAsString().toUpperCase());
        this.reactions = new ArrayList<>();

        JsonArray reactionsArray = json.getAsJsonArray("reactions");
        for (JsonElement reactionElement : reactionsArray) {
            reactions.add(new ReactionEntry(reactionElement.getAsJsonObject(), this.type));
        }
    }

    public String getKey() { return key; }
    public ReactionType getType() { return type; }
    public List<ReactionEntry> getReactions() { return reactions; }
}