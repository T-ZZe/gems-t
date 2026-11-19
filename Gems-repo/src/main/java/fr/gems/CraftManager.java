package fr.gems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gère les crafts des gems : éditeur en interface (/gem craft <gem>), activation,
 * limite de crafts (globale au serveur), enregistrement des recettes et animation de craft.
 */
public final class CraftManager implements Listener {

    /** Slots de la grille 3x3 dans l'inventaire de 54 cases. */
    private static final int[] GRID = {11, 12, 13, 20, 21, 22, 29, 30, 31};
    private static final int SLOT_TOGGLE = 16;
    private static final int SLOT_ARROW = 23;
    private static final int SLOT_RESULT = 24;
    private static final int SLOT_LIMIT = 25;
    private static final int SLOT_RESET = 34;
    private static final int SLOT_CANCEL = 45;
    private static final int SLOT_SAVE = 49;
    private static final int SLOT_CLEAR = 53;

    private final GemsPlugin plugin;
    private final File file;
    private final String namespace;
    private YamlConfiguration data;

    private static final class CraftHolder implements InventoryHolder {
        final Gem gem;
        boolean enabled;
        int limit;
        boolean saved;
        Inventory inventory;

        CraftHolder(Gem gem) {
            this.gem = gem;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public CraftManager(GemsPlugin plugin) {
        this.plugin = plugin;
        this.namespace = new NamespacedKey(plugin, "x").getNamespace();
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "crafts.yml");
        reload();
    }

    // ------------------------------------------------------------------ données

    public void reload() {
        data = YamlConfiguration.loadConfiguration(file);
        for (Gem g : Gem.values()) registerRecipe(g);
    }

    public void unregisterAll() {
        for (Gem g : Gem.values()) Bukkit.removeRecipe(recipeKey(g));
    }

    private String path(Gem g) {
        return "crafts." + g.name();
    }

    private NamespacedKey recipeKey(Gem g) {
        return new NamespacedKey(plugin, "craft_" + g.name().toLowerCase(Locale.ROOT));
    }

    private boolean isEnabled(Gem g) {
        return data.getBoolean(path(g) + ".enabled", false);
    }

    private int getLimit(Gem g) {
        return data.getInt(path(g) + ".limit", 0);
    }

    private int getCrafted(Gem g) {
        return data.getInt(path(g) + ".crafted", 0);
    }

    private boolean limitReached(Gem g) {
        int limit = getLimit(g);
        return limit > 0 && getCrafted(g) >= limit;
    }

    private ItemStack[] getIngredients(Gem g) {
        ItemStack[] arr = new ItemStack[9];
        for (int i = 0; i < 9; i++) arr[i] = data.getItemStack(path(g) + ".ingredients." + i);
        return arr;
    }

    private void persist() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder crafts.yml : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ recettes

    private RecipeChoice choiceFor(ItemStack item) {
        if (item.hasItemMeta()) return new RecipeChoice.ExactChoice(item);
        return new RecipeChoice.MaterialChoice(item.getType());
    }

    private void registerRecipe(Gem g) {
        NamespacedKey key = recipeKey(g);
        Bukkit.removeRecipe(key);
        if (!isEnabled(g)) return;

        ItemStack[] ing = getIngredients(g);
        int minR = 3, maxR = -1, minC = 3, maxC = -1;
        for (int i = 0; i < 9; i++) {
            if (ing[i] == null || ing[i].getType().isAir()) continue;
            int r = i / 3, c = i % 3;
            minR = Math.min(minR, r);
            maxR = Math.max(maxR, r);
            minC = Math.min(minC, c);
            maxC = Math.max(maxC, c);
        }
        if (maxR < 0) return;

        try {
            ShapedRecipe recipe = new ShapedRecipe(key, plugin.createGem(g));
            String[] shape = new String[maxR - minR + 1];
            for (int r = minR; r <= maxR; r++) {
                StringBuilder sb = new StringBuilder();
                for (int c = minC; c <= maxC; c++) {
                    int idx = r * 3 + c;
                    boolean has = ing[idx] != null && !ing[idx].getType().isAir();
                    sb.append(has ? (char) ('A' + idx) : ' ');
                }
                shape[r - minR] = sb.toString();
            }
            recipe.shape(shape);
            for (int i = 0; i < 9; i++) {
                if (ing[i] != null && !ing[i].getType().isAir()) {
                    recipe.setIngredient((char) ('A' + i), choiceFor(ing[i]));
                }
            }
            Bukkit.addRecipe(recipe);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            plugin.getLogger().warning("Recette invalide pour " + g.name() + " : " + ex.getMessage());
        }
    }

    private Gem gemOfRecipe(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed)) return null;
        NamespacedKey key = keyed.getKey();
        if (!key.getNamespace().equals(namespace) || !key.getKey().startsWith("craft_")) return null;
        try {
            return Gem.valueOf(key.getKey().substring("craft_".length()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ événements de craft

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent e) {
        Gem crafted = gemOfRecipe(e.getRecipe());
        if (crafted != null) {
            if (!isEnabled(crafted) || limitReached(crafted)) e.getInventory().setResult(null);
            return;
        }
        // Les gems ne servent pas dans les crafts vanilla ou d'autres plugins
        for (ItemStack it : e.getInventory().getMatrix()) {
            if (plugin.gemOf(it) != null) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        Gem g = gemOfRecipe(e.getRecipe());
        if (g == null) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (!isEnabled(g) || limitReached(g)) {
            e.setCancelled(true);
            p.sendMessage(Component.text("Cette gem ne peut plus être craftée (limite atteinte ou craft désactivé).",
                    NamedTextColor.RED));
            return;
        }
        if (e.isShiftClick()) {
            e.setCancelled(true);
            p.sendMessage(Component.text("Craft la gem un exemplaire à la fois (sans shift).", NamedTextColor.RED));
            return;
        }

        int crafted = getCrafted(g) + 1;
        data.set(path(g) + ".crafted", crafted);
        persist();

        int limit = getLimit(g);
        if (limit > 0) {
            Bukkit.broadcast(Component.text(g.displayName + " : " + crafted + "/" + limit + " exemplaires craftés.",
                    NamedTextColor.GRAY));
        }
        Bukkit.getScheduler().runTask(plugin, () -> plugin.playCraftAnimation(p, g));
    }

    // ------------------------------------------------------------------ éditeur (GUI)

    public void openEditor(Player p, Gem g) {
        CraftHolder h = new CraftHolder(g);
        h.enabled = isEnabled(g);
        h.limit = getLimit(g);
        Inventory inv = Bukkit.createInventory(h, 54, Component.text("Craft : " + g.displayName, g.color));
        h.inventory = inv;

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setHideTooltip(true);
        filler.setItemMeta(fm);
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        ItemStack[] ing = getIngredients(g);
        for (int i = 0; i < 9; i++) inv.setItem(GRID[i], ing[i]);

        inv.setItem(SLOT_ARROW, named(Material.ARROW, "Résultat", NamedTextColor.GRAY));
        inv.setItem(SLOT_RESULT, plugin.createGem(g));
        inv.setItem(SLOT_CANCEL, named(Material.BARRIER, "Fermer sans sauvegarder", NamedTextColor.RED));
        inv.setItem(SLOT_SAVE, named(Material.EMERALD, "Enregistrer le craft", NamedTextColor.GREEN,
                "Applique la grille, l'état et la limite."));
        inv.setItem(SLOT_CLEAR, named(Material.LAVA_BUCKET, "Vider la grille", NamedTextColor.GOLD));
        refresh(h);

        p.openInventory(inv);
        p.sendMessage(Component.text("Prends un item dans ton inventaire (curseur) puis clique une case de la grille "
                + "pour le définir. Clic avec le curseur vide = retire la case.", NamedTextColor.GRAY));
    }

    private void refresh(CraftHolder h) {
        Inventory inv = h.inventory;
        Gem g = h.gem;

        if (h.enabled) {
            inv.setItem(SLOT_TOGGLE, named(Material.LIME_CONCRETE, "Craft activé", NamedTextColor.GREEN,
                    "Clique pour désactiver."));
        } else {
            inv.setItem(SLOT_TOGGLE, named(Material.RED_CONCRETE, "Craft désactivé", NamedTextColor.RED,
                    "Clique pour activer."));
        }

        String limitText = h.limit == 0 ? "illimité" : String.valueOf(h.limit);
        inv.setItem(SLOT_LIMIT, named(Material.BOOK, "Limite de crafts : " + limitText, NamedTextColor.YELLOW,
                "Déjà craftés : " + getCrafted(g),
                "Clic gauche : +1 / Clic droit : -1",
                "Shift + clic : +/-10",
                "0 = illimité (limite globale au serveur)"));

        inv.setItem(SLOT_RESET, named(Material.TNT, "Réinitialiser le compteur", NamedTextColor.RED,
                "Compteur actuel : " + getCrafted(g),
                "Appliqué immédiatement."));
    }

    private ItemStack named(Material m, String name, NamedTextColor color, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        if (lore.length > 0) {
            List<Component> l = new ArrayList<>();
            for (String s : lore) l.add(Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(l);
        }
        it.setItemMeta(meta);
        return it;
    }

    private int gridIndex(int rawSlot) {
        for (int i = 0; i < GRID.length; i++) if (GRID[i] == rawSlot) return i;
        return -1;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof CraftHolder h)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        int raw = e.getRawSlot();
        ClickType ct = e.getClick();

        if (raw >= 0 && raw < top.getSize()) {
            e.setCancelled(true);
            int gi = gridIndex(raw);
            if (gi >= 0) {
                // La grille contient des "fantômes" : aucun item réel n'est déplacé.
                ItemStack src = ct == ClickType.NUMBER_KEY
                        ? p.getInventory().getItem(e.getHotbarButton())
                        : e.getCursor();
                if (src != null && !src.getType().isAir()) {
                    ItemStack copy = src.clone();
                    copy.setAmount(1);
                    top.setItem(raw, copy);
                } else {
                    top.setItem(raw, null);
                }
                return;
            }
            handleButton(p, h, top, raw, ct);
            return;
        }
        // Clics dans l'inventaire du joueur : on bloque ce qui pourrait toucher l'éditeur
        if (ct.isShiftClick() || ct == ClickType.DOUBLE_CLICK) e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof CraftHolder)) return;
        for (int s : e.getRawSlots()) {
            if (s < top.getSize()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof CraftHolder h && !h.saved
                && e.getPlayer() instanceof Player p) {
            p.sendMessage(Component.text("Craft non sauvegardé.", NamedTextColor.GRAY));
        }
    }

    private void handleButton(Player p, CraftHolder h, Inventory top, int raw, ClickType ct) {
        if (raw == SLOT_TOGGLE) {
            h.enabled = !h.enabled;
        } else if (raw == SLOT_LIMIT) {
            int delta = switch (ct) {
                case LEFT -> 1;
                case RIGHT -> -1;
                case SHIFT_LEFT -> 10;
                case SHIFT_RIGHT -> -10;
                default -> 0;
            };
            h.limit = Math.max(0, Math.min(100000, h.limit + delta));
        } else if (raw == SLOT_RESET) {
            data.set(path(h.gem) + ".crafted", 0);
            persist();
            p.sendMessage(Component.text("Compteur de " + h.gem.displayName + " remis à 0.", NamedTextColor.GREEN));
        } else if (raw == SLOT_CLEAR) {
            for (int s : GRID) top.setItem(s, null);
        } else if (raw == SLOT_CANCEL) {
            p.closeInventory();
            return;
        } else if (raw == SLOT_SAVE) {
            save(p, h, top);
            return;
        } else {
            return;
        }
        refresh(h);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void save(Player p, CraftHolder h, Inventory top) {
        boolean any = false;
        for (int s : GRID) {
            ItemStack it = top.getItem(s);
            if (it != null && !it.getType().isAir()) {
                any = true;
                break;
            }
        }
        if (!any) {
            p.sendMessage(Component.text("Mets au moins un ingrédient dans la grille.", NamedTextColor.RED));
            return;
        }

        Gem g = h.gem;
        String base = path(g);
        data.set(base + ".ingredients", null);
        for (int i = 0; i < 9; i++) {
            ItemStack it = top.getItem(GRID[i]);
            if (it != null && !it.getType().isAir()) {
                ItemStack one = it.clone();
                one.setAmount(1);
                data.set(base + ".ingredients." + i, one);
            }
        }
        data.set(base + ".enabled", h.enabled);
        data.set(base + ".limit", h.limit);
        persist();
        registerRecipe(g);

        h.saved = true;
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        p.sendMessage(Component.text("Craft de " + g.displayName + " enregistré ("
                + (h.enabled ? "activé" : "désactivé") + ", limite : "
                + (h.limit == 0 ? "illimitée" : h.limit) + ").", NamedTextColor.GREEN));
    }

    public void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof CraftHolder) {
                p.closeInventory();
            }
        }
    }
}
