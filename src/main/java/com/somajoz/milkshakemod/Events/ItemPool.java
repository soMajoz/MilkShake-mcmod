package com.somajoz.milkshakemod.Events;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ItemPool {
    private static final Random random = new Random();

    public static Item getRandomItem(Set<String> dimensions) {
        List<Item> candidates = new ArrayList<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;

            String path = BuiltInRegistries.ITEM.getKey(item).getPath();

            // 1. Отсеиваем технические предметы
            if (isCreativeOrTechnical(path, item)) {
                continue;
            }

            // 2. Проверяем принадлежность к измерениям
            boolean isNether = isNetherItem(path);
            boolean isEnd = isEndItem(path);

            // Если предмет требует Незер, а он закрыт
            if (isNether && !dimensions.contains("minecraft:the_nether")) continue;

            // Если предмет требует Энд, а он закрыт
            if (isEnd && !dimensions.contains("minecraft:the_end")) continue;

            candidates.add(item);
        }

        if (candidates.isEmpty()) return Items.DIRT;
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static boolean isCreativeOrTechnical(String path, Item item) {
        // Яйца призыва
        if (path.contains("spawn_egg")) return true;

        // Технические блоки и недоступные вещи
        if (path.contains("command_block") || path.contains("jigsaw") || path.contains("structure_") ||
                path.contains("barrier") || path.contains("light") && !path.contains("lightning") ||
                path.contains("debug_stick") || path.contains("knowledge_book")) return true;

        // Зараженные блоки (Infested / Monster Egg)
        if (path.startsWith("infested_")) return true;

        // Предметы с обязательным NBT (без него они бесполезны)
        if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION ||
                item == Items.TIPPED_ARROW || item == Items.ENCHANTED_BOOK || item == Items.GOAT_HORN ||
                item == Items.SUSPICIOUS_STEW || item == Items.OMINOUS_BOTTLE || item == Items.WRITTEN_BOOK) return true;

        // Блоки, не выпадающие в выживании или неразрушимые
        if (item == Items.BEDROCK || item == Items.END_PORTAL_FRAME || item == Items.REINFORCED_DEEPSLATE ||
                item == Items.SPAWNER || item == Items.TRIAL_SPAWNER || item == Items.VAULT ||
                item == Items.BUDDING_AMETHYST || item == Items.FARMLAND || item == Items.DIRT_PATH ||
                item == Items.FROGSPAWN || item == Items.PETRIFIED_OAK_SLAB || item == Items.PLAYER_HEAD) return true;

        return false;
    }

    private static boolean isNetherItem(String path) {
        // --- Блоки и руды ---
        if (path.contains("nether")) return true; // netherrack, nether_brick, netherite, etc.
        if (path.contains("crimson")) return true; // nylium, stems, planks, door...
        if (path.contains("warped")) return true;  // nylium, stems, planks, fungus...
        if (path.contains("soul_")) return true;   // sand, soil, torch, lantern, campfire
        if (path.contains("basalt")) return true;  // smooth, polished
        if (path.contains("blackstone")) return true; // gilded, polished, bricks
        if (path.contains("glowstone")) return true; // dust, block
        if (path.contains("shroomlight")) return true;
        if (path.contains("magma")) return true;   // block, cream
        if (path.contains("quartz")) return true;  // block, ore, pillar
        if (path.contains("ancient_debris") || path.contains("scrap")) return true;
        if (path.contains("crying_obsidian")) return true;
        if (path.contains("respawn_anchor")) return true;

        // --- Мобы и лут ---
        if (path.contains("blaze")) return true;   // rod, powder
        if (path.contains("ghast")) return true;   // tear
        if (path.contains("wither_skeleton")) return true; // skull
        if (path.contains("piglin")) return true;  // banner pattern, head
        if (path.contains("hoglin")) return true;
        if (path.contains("zoglin")) return true;
        if (path.contains("strider")) return true;

        // --- 1.20+ Шаблоны и прочее ---
        if (path.contains("snout")) return true; // armor trim, banner pattern
        if (path.contains("rib")) return true;   // armor trim
        if (path.equals("pigstep")) return true; // music disc (only bastions)

        // --- Предметы, зависящие от Незера (Composite Items) ---
        // Око эндера требует Blaze Powder -> значит нужен Незер
        if (path.equals("ender_eye")) return true;
        if (path.equals("ender_chest")) return true; // Требует Eye of Ender
        if (path.equals("end_crystal")) return true; // Требует Ghast Tear
        if (path.equals("spectral_arrow")) return true; // Требует Glowstone
        if (path.equals("lodestone")) return true; // Требует Netherite

        // Золото есть в обычном мире, его НЕ фильтруем, кроме руды Незера (она уже поймана словом nether)

        return false;
    }

    private static boolean isEndItem(String path) {
        // --- Блоки ---
        if (path.contains("end_")) return true; // stone, rod, bricks
        if (path.endsWith("_end")) return true; // rod (если вдруг)
        if (path.contains("purpur")) return true; // block, pillar, slab
        if (path.contains("chorus")) return true; // plant, flower, fruit

        // --- Мобы и лут ---
        if (path.contains("shulker")) return true; // shell, box (все цвета)
        if (path.contains("elytra")) return true;
        if (path.contains("dragon")) return true; // breath, head, egg

        // --- 1.20+ Шаблоны ---
        if (path.contains("spire")) return true; // armor trim (End City)

        // Примечание: Ender Pearl можно добыть в обычном мире (клирики, эндермены),
        // поэтому мы не блокируем само слово "ender" глобально, только специфику.

        return false;
    }
}
