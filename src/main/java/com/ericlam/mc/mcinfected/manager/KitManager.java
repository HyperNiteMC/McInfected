package com.ericlam.mc.mcinfected.manager;

import com.ericlam.mc.mcinfected.Kit;
import com.ericlam.mc.mcinfected.config.KitConfig;
import com.ericlam.mc.mcinfected.config.LangConfig;
import com.ericlam.mc.mcinfected.implement.McInfPlayer;
import com.ericlam.mc.mcinfected.implement.team.HumanTeam;
import com.ericlam.mc.mcinfected.main.McInfected;
import com.shampaggon.crackshot.CSUtility;
import me.DeeCaaD.CrackShotPlus.API;
import me.DeeCaaD.CrackShotPlus.CSPapi;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class KitManager {
    private final Map<String, Kit> kitMap = new LinkedHashMap<>();
    private final Map<OfflinePlayer, String> currentKit = new ConcurrentHashMap<>();
    private CSUtility csUtility = API.cs();


    public KitManager(KitConfig kitConfig) {
        this.loadKit("Human", kitConfig.Human);
        this.loadKit("Infected", kitConfig.Infected);
    }

    private void loadKit(String section, Map<String, KitConfig.Kit> kitMap) {
        if (kitMap.isEmpty()) return;
        for (var en : kitMap.entrySet()) {
            String kitName = en.getKey();
            KitConfig.Kit kit = en.getValue();
            List<Material> armor = kit.Armor;
            String display = kit.Display;
            List<String> csItems = kit.Inventory.Crackshot;
            List<String> items = kit.Inventory.Normal;
            List<String> description = kit.Description.stream().map(e -> ChatColor.translateAlternateColorCodes('&', e)).collect(Collectors.toList());
            String icon = kit.Icon;
            EntityType disguise = kit.Disguise;
            List<String> potions = kit.Potions;
            List<ItemStack> stacks = csItems.stream().map(s -> s.split(":")).map(arr -> {
                ItemStack stack = csUtility.generateWeapon(arr[0]);
                stack = CSPapi.updateItemStackFeaturesNonPlayer(arr[0], stack);
                if (stack != null && arr.length > 1) {
                    stack.setAmount(Integer.parseInt(arr[1]));
                }
                return stack;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            stacks.addAll(items.stream().map(s -> s.split(":")).map(arr -> {
                Material material = Material.getMaterial(arr[0]);
                if (material == null) return null;
                ItemStack stack = new ItemStack(material);
                if (arr.length > 1) {
                    stack.setAmount(Integer.parseInt(arr[1]));
                }
                return stack;
            }).filter(Objects::nonNull).collect(Collectors.toList()));
            ItemStack[] armors = armor.size() == 0 ? null : armor.stream().map(s -> s == null ? Material.AIR : s).map(ItemStack::new).toArray(ItemStack[]::new);
            ItemStack iconItem = Optional.ofNullable(CSPapi.updateItemStackFeaturesNonPlayer(icon, csUtility.generateWeapon(icon))).orElseGet(() -> new ItemStack(Material.valueOf(icon)));
            List<PotionEffect> potionsEffect = Optional.ofNullable(potions).orElse(List.of()).stream().map(s -> s.split(":")).filter(s -> s.length >= 3).map(s -> {
                PotionEffectType type = PotionEffectType.getByName(s[0]);
                if (type == null) return null;
                int dur = Integer.parseInt(s[1]);
                int amp = Integer.parseInt(s[2]);
                return new PotionEffect(type, dur * 20, amp, false, false);
            }).filter(Objects::nonNull).collect(Collectors.toList());
            Kit kits = new Kit(display == null ? kitName : ChatColor.translateAlternateColorCodes('&', display), armors, stacks.toArray(ItemStack[]::new), description, iconItem, potionsEffect);
            if (disguise != null) {
                try {
                    kits.setDisguise(disguise);
                } catch (IllegalArgumentException ignored) {
                }
            }
            this.kitMap.put(kitName, kits);
        }
    }

    public Map<String, Kit> getKitMap() {
        return kitMap;
    }

    public void removeLastKit(Player target, boolean invClear) {
        if (invClear) target.getInventory().clear();
        target.getActivePotionEffects().forEach(e -> target.removePotionEffect(e.getType()));
        target.setFireTicks(0);
        target.setGlowing(false);
        target.setFoodLevel(20);
        DisguiseAPI.undisguiseToAll(target);
        this.currentKit.remove(target);
    }

    public void gainKit(Player target, String kit) {
        Kit infKit = kitMap.get(kit);
        if (infKit == null) {
            McInfected.getProvidingPlugin(McInfected.class).getLogger().warning("There are no ".concat(kit).concat(" in kitMap"));
            return;
        }
        PlayerInventory playerInventory = target.getInventory();
        //clear previous
        removeLastKit(target, true);
        //insert current
        playerInventory.setArmorContents(infKit.getArmors());
        playerInventory.addItem(infKit.getInventory());
        target.addPotionEffects(infKit.getPotionEffects());
        this.currentKit.put(target, kit);
        if (infKit.getDisguise() == EntityType.PLAYER) return;
        Disguise disguise = new MobDisguise(DisguiseType.getType(infKit.getDisguise()));
        DisguiseAPI.setViewDisguiseToggled(target, false);
        DisguiseAPI.disguiseToAll(target, disguise);
    }

    public void gainKit(McInfPlayer player) {
        String kit = player.getTeam() instanceof HumanTeam ? player.getHumanKit() : player.getZombieKit();
        this.gainKit(player.getPlayer(), kit);
        var msg = McInfected.getApi().getConfigManager().getConfigAs(LangConfig.class);
        if (player.getTeam() instanceof HumanTeam) {
            player.getPlayer().sendMessage(msg.get("Picture.Human.To Win"));
            player.getPlayer().sendTitle("", msg.getPure("Picture.Human.You"), 0, 60, 20);
        } else {
            player.getPlayer().sendMessage(msg.get("Picture.Infected.To Win"));
            player.getPlayer().sendTitle("", msg.getPure("Picture.Infected.You"), 0, 60, 20);
        }
    }

    @Nullable
    public String getCurrentUsing(Player player) {
        return currentKit.get(player);
    }
}
