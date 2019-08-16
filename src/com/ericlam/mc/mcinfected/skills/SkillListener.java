package com.ericlam.mc.mcinfected.skills;

import com.ericlam.mc.mcinfected.api.McInfectedAPI;
import com.ericlam.mc.mcinfected.implement.team.HumanTeam;
import com.ericlam.mc.mcinfected.implement.team.ZombieTeam;
import com.ericlam.mc.mcinfected.main.McInfected;
import com.ericlam.mc.mcinfected.tasks.GameTask;
import com.ericlam.mc.minigames.core.character.GamePlayer;
import com.ericlam.mc.minigames.core.character.TeamPlayer;
import com.ericlam.mc.minigames.core.event.player.CrackShotDeathEvent;
import com.ericlam.mc.minigames.core.event.player.GamePlayerDeathEvent;
import com.ericlam.mc.minigames.core.event.state.InGameStateSwitchEvent;
import com.ericlam.mc.minigames.core.main.MinigamesCore;
import com.ericlam.mc.minigames.core.manager.GameUtils;
import me.DeeCaaD.CrackShotPlus.API;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class SkillListener implements Listener {
    private SkillManager skillManager;

    public SkillListener(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent e) {
        MinigamesCore.getApi().getPlayerManager().findPlayer(e.getPlayer()).ifPresent(g -> {
            if (MinigamesCore.getApi().getGameManager().getInGameState() == McInfected.getPlugin(McInfected.class).getGameEndState())
                return;
            e.setCancelled(true);
            if (g.getStatus() != GamePlayer.Status.GAMING) return;
            if (g.castTo(TeamPlayer.class).getTeam() instanceof ZombieTeam) {
                skillManager.launchSkill(e.getPlayer());
            } else if (g.castTo(TeamPlayer.class).getTeam() instanceof HumanTeam) {
                if (GameTask.shouldHunterActivate(MinigamesCore.getApi().getPlayerManager().getGamePlayer())) {
                    String hunterKit = McInfected.getApi().getConfigManager().getData("hunterKit", String.class).orElse("");
                    String using = McInfected.getApi().currentKit(g.getPlayer());
                    if (using != null && using.equals(hunterKit)) return;
                    McInfected.getApi().gainKit(g.getPlayer(), hunterKit);
                    McInfected.getApi().getConfigManager().getData("hunterBurn", String[].class).ifPresent(s -> MinigamesCore.getApi().getGameUtils().playSound(g.getPlayer(), s));
                }
            }
        });
    }

    @EventHandler
    public void onGameStateSwitch(InGameStateSwitchEvent e) {
        skillManager.clearSkill();
    }

    @EventHandler
    public void onGamePlayerDeath(GamePlayerDeathEvent e) {
        McInfectedAPI api = McInfected.getApi();
        GameUtils utils = MinigamesCore.getApi().getGameUtils();
        if (e.getKiller() == null) return;
        GamePlayer killer = e.getKiller();
        if (!(killer.castTo(TeamPlayer.class).getTeam() instanceof HumanTeam)) return;
        String using = api.currentKit(killer.getPlayer());
        if (using == null) return;
        String hunterKit = api.getConfigManager().getData("hunterKit", String.class).orElse("");
        if (!using.equals(hunterKit)) return;
        boolean melee = false;
        if (e instanceof CrackShotDeathEvent) {
            CrackShotDeathEvent cs = (CrackShotDeathEvent) e;
            melee = API.getCSDirector().getBoolean(cs.getWeaponTitle() + ".Item_Information.Melee_Mode");
        }
        if (melee) {
            api.getConfigManager().getData("hunterKill", String[].class).ifPresent(s -> Bukkit.getOnlinePlayers().forEach(p -> utils.playSound(p, s)));
        }


    }
}