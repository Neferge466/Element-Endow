Element Endow mod introduces a comprehensive elemental reaction system that can be fully customized via data packs. The mod supports two types of reactions: Internal Reactions (passive buffs when a player has multiple elements) and Induced Reactions (combat effects when a player attacks an entity with specific elements).





Reaction Types
Internal Reactions

Trigger Condition: Check every 20 ticks (1 second) if the player has multiple elemental attributes.

Matching Rule: The player must have all elements listed in the match array.

Effect Calculation: Final multiplier = average of elemental attribute values × rate.

Effect Application: Only the empower effects are applied to the player.

Induced Reactions

Trigger Condition: When a player attacks an entity.

Matching Rule: The attacker must have the element specified as the first element in two_match, and the target must have the second element with a value greater than 0.

Damage Calculation: Final damage = original damage × rate[0] ÷ rate[1].

Effect Application: afflict effects are applied to the target, and empower effects are applied to the attacker.

Configuration Fields
Top-Level Fields


Field	Type	Description
key	String	Unique identifier for the configuration file.
type	String	Reaction type: "INTERNAL" or "INDUCED".
reactions	Array	Array of reaction entries.
Internal Reaction Fields


Field	Type	Description
match	Array of Strings	List of element IDs that must be present.
rate	Number	Multiplier coefficient, multiplied by the average of the element attributes.
effect	Object	Effect configuration.
Induced Reaction Fields


Field	Type	Description
two_match	Array of Strings	Two elements: [attacker_element, target_element].
rate	Array of Numbers	Damage and defense multipliers: [damage_multiplier, defense_multiplier].
effect	Object	Effect configuration.
Effect Configuration

The effect object has two fields:

afflict: Array of effects applied to the target (in induced reactions) or left empty (in internal reactions).

empower: Array of effects applied to the attacker (in induced reactions) or the player (in internal reactions).

Each effect is an array with four elements:

json
["effect_id", duration, amplifier, show_particles]

effect_id: The Minecraft effect resource location (e.g., "minecraft:speed").

duration: Duration in ticks (20 ticks = 1 second).

amplifier: Effect level (0 = I, 1 = II, etc.).

show_particles: Boolean, whether to show particles.
