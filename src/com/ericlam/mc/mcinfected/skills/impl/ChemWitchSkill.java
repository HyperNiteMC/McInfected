package com.ericlam.mc.mcinfected.skills.impl;

import com.ericlam.mc.mcinfected.implement.team.ZombieTeam;
import com.ericlam.mc.mcinfected.skills.InfectedSkill;
import com.ericlam.mc.minigames.core.character.GamePlayer;
import com.ericlam.mc.minigames.core.character.TeamPlayer;
import com.ericlam.mc.minigames.core.main.MinigamesCore;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ChemWitchSkill implements InfectedSkill {

    @Override
    public void execute(Player self) {
        List<GamePlayer> zombies = MinigamesCore.getApi().getPlayerManager().getGamePlayer().stream().filter(g -> g.castTo(TeamPlayer.class).getTeam() instanceof ZombieTeam).collect(Collectors.toList());
        zombies.forEach(g -> {
            Player player = g.getPlayer();
            player.sendTitle("", "§e全體殭屍速度 +5%, 持續 10 秒", 10, 40, 10);
            player.setWalkSpeed(0.3f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2, 0.9f);
        });
    }

    @Override
    public void revert(Player self) {
        List<GamePlayer> zombies = MinigamesCore.getApi().getPlayerManager().getGamePlayer().stream().filter(g -> g.castTo(TeamPlayer.class).getTeam() instanceof ZombieTeam).collect(Collectors.toList());
        zombies.forEach(g -> g.getPlayer().setWalkSpeed(0.25f));
    }

    @Override
    public long getKeepingTime() {
        return 10;
    }

    @Override
    public long getCoolDown() {
        return 45;
    }
}
