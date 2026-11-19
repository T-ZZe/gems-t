package fr.gems;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;

public enum Gem {
    WHITE("White Gem", NamedTextColor.WHITE, Material.WHITE_DYE, Color.WHITE, Particle.CLOUD,
            "Passif : invisibilité permanente",
            "Clic gauche : semi-intangible 15s (cooldown 60s)"),

    RED("Red Gem", NamedTextColor.RED, Material.RED_DYE, Color.RED, Particle.FLAME,
            "Passif : critiques automatiques dans le Nether",
            "Clic droit : tracking",
            "Sneak + clic droit : force x1.25"),

    GREEN("Green Gem", NamedTextColor.GREEN, Material.GREEN_DYE, Color.LIME, Particle.HAPPY_VILLAGER,
            "Passif : Héros du village V permanent",
            "Clic gauche sur un joueur : Hunger III"),

    PINK("Pink Gem", NamedTextColor.LIGHT_PURPLE, Material.PINK_DYE, Color.fromRGB(255, 105, 180), Particle.HEART,
            "Passif : +2 cœurs permanents",
            "Clic droit : répare ton armure"),

    YELLOW("Yellow Gem", NamedTextColor.YELLOW, Material.YELLOW_DYE, Color.YELLOW, Particle.ELECTRIC_SPARK,
            "Passif : saturation complète avec n'importe quelle nourriture (cooldown)",
            "Clic droit : épée -> mace pendant 30s (non enchantée)"),

    BLUE("Blue Gem", NamedTextColor.BLUE, Material.BLUE_DYE, Color.fromRGB(30, 100, 255), Particle.END_ROD,
            "Passif : Speed I permanent",
            "Clic droit : dash ~10 blocs (cooldown 30s)",
            "Interdit en sprintant"),

    BLACK("Black Gem", NamedTextColor.DARK_GRAY, Material.BLACK_DYE, Color.fromRGB(45, 45, 45), Particle.SMOKE,
            "Passif : 15 cœurs dans l'End",
            "Passif : immunité à l'effet Wither",
            "Clic droit : lance 3 têtes de Wither (cooldown 2min)"),

    PURPLE("Purple Gem", NamedTextColor.DARK_PURPLE, Material.PURPLE_DYE, Color.PURPLE, Particle.PORTAL,
            "Sneak + clic droit : téléportation là où tu regardes",
            "Clic droit : freecam 15s (cooldown 5min)"),

    ORANGE("Orange Gem", NamedTextColor.GOLD, Material.ORANGE_DYE, Color.ORANGE, Particle.LAVA,
            "Passif : résistance au feu"),

    BLOCK("Gem Block", NamedTextColor.AQUA, Material.NETHER_STAR, Color.AQUA, Particle.ELECTRIC_SPARK,
            "Passif : tous les effets positifs (sauf saturation)",
            "Passif : un éclair à chaque coup",
            "Passif : 20 cœurs",
            "Clic droit : dash",
            "Sneak + clic droit : auto-crit 15s (cooldown 1min)");

    public final String displayName;
    public final TextColor color;
    public final Material material;
    public final Color rgb;
    public final Particle particle;
    public final String[] lore;

    Gem(String displayName, TextColor color, Material material, Color rgb, Particle particle, String... lore) {
        this.displayName = displayName;
        this.color = color;
        this.material = material;
        this.rgb = rgb;
        this.particle = particle;
        this.lore = lore;
    }
}
