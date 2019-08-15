package com.ericlam.mc.mcinfected.implement;

import com.ericlam.mc.mcinfected.implement.team.HumanTeam;
import com.ericlam.mc.mcinfected.implement.team.ZombieTeam;
import com.ericlam.mc.mcinfected.main.McInfected;
import com.ericlam.mc.mcinfected.tasks.VotingTask;
import com.ericlam.mc.minigames.core.character.TeamPlayer;
import com.ericlam.mc.minigames.core.game.GameTeam;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Optional;

public class McInfPlayer implements TeamPlayer {

    private Player player;
    private GameTeam gameTeam;
    private Status status;
    private String humanKit, zombieKit;
    private boolean isKillByMelee;

    public McInfPlayer(Player player, GameTeam gameTeam, Status status) {
        this.player = player;
        player.setFoodLevel(20);
        player.setWalkSpeed(0.2f);
        player.setGlowing(false);
        Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).ifPresent(a -> {
            a.setBaseValue(20);
            player.setHealth(a.getBaseValue());
            player.setHealthScale(a.getBaseValue());
        });
        this.gameTeam = gameTeam;
        this.status = status;
        this.humanKit = McInfected.getApi().getConfigManager().getData("humanDefault", String.class).orElse("");
        this.zombieKit = McInfected.getApi().getConfigManager().getData("zombieDefault", String.class).orElse("");
        this.isKillByMelee = false;
    }

    public boolean isKillByMelee() {
        return isKillByMelee;
    }

    public void setKillByMelee(boolean killByMelee) {
        isKillByMelee = killByMelee;
    }

    public McInfPlayer(Player player){
        this(player, null, null);
    }

    public String getHumanKit() {
        return humanKit;
    }

    public void setHumanKit(String humanKit) {
        this.humanKit = humanKit;
    }

    public String getZombieKit() {
        return zombieKit;
    }

    public void setZombieKit(String zombieKit) {
        this.zombieKit = zombieKit;
    }

    @Override
    public GameTeam getTeam() {
        return gameTeam;
    }

    @Override
    public void setTeam(GameTeam gameTeam) {
        this.gameTeam = gameTeam;
        if (gameTeam instanceof ZombieTeam){
            Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).ifPresent(a->{
                a.setBaseValue(200);
                player.setHealth(a.getBaseValue());
                player.setHealthScale(a.getBaseValue());
            });
            player.setWalkSpeed(0.265f);
        }else if (gameTeam instanceof HumanTeam){
            Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).ifPresent(a->{
                a.setBaseValue(20);
                player.setHealth(a.getBaseValue());
                player.setHealthScale(a.getBaseValue());
            });
            player.setWalkSpeed(0.2f);
        }
        VotingTask.switchTeam(this);
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }
}
