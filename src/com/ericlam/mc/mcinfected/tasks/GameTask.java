package com.ericlam.mc.mcinfected.tasks;

import com.ericlam.mc.mcinfected.implement.McInfPlayer;
import com.ericlam.mc.mcinfected.implement.team.HumanTeam;
import com.ericlam.mc.mcinfected.implement.team.ZombieTeam;
import com.ericlam.mc.mcinfected.main.McInfected;
import com.ericlam.mc.mcinfected.main.SoundUtils;
import com.ericlam.mc.minigames.core.character.GamePlayer;
import com.ericlam.mc.minigames.core.character.TeamPlayer;
import com.ericlam.mc.minigames.core.game.GameState;
import com.ericlam.mc.minigames.core.main.MinigamesCore;
import com.ericlam.mc.minigames.core.manager.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GameTask extends InfTask {

    static List<GamePlayer> alphasZombies = new ArrayList<>();
    private boolean notifiedHunter = false;

    @Override
    public void initRun(PlayerManager playerManager) {
        MinigamesCore.getApi().getGameManager().setState(GameState.IN_GAME);
        float percent = McInfected.getApi().getConfigManager().getData("alphaPercent", Float.class).orElse(0.25F);
        int alphas = Math.round(playerManager.getGamePlayer().size() * percent);
        Random random = new Random();
        alphasZombies.clear();
        while (alphasZombies.size() < alphas) {
            LinkedList<GamePlayer> list = new LinkedList<>(playerManager.getGamePlayer());
            int i = random.nextInt(list.size());
            TeamPlayer player = list.remove(i).castTo(TeamPlayer.class);
            player.setTeam(mcinf.getZombieTeam());
            alphasZombies.add(player);
            Bukkit.broadcastMessage(McInfected.getApi().getConfigManager().getMessage("Game.Infected").replace("<player>", player.getPlayer().getDisplayName()));
        }
        playerManager.getGamePlayer().forEach(p -> McInfected.getApi().gainKit(p.castTo(McInfPlayer.class)));

    }

    public static boolean shouldHunterActivate(final List<GamePlayer> gamePlayers) {
        if (gamePlayers.size() < 1) return false;
        float hunterPercent = McInfected.getApi().getConfigManager().getData("hunterPercent", Float.class).orElse(0.2f);
        List<GamePlayer> humans = gamePlayers.stream().filter(g -> g.castTo(TeamPlayer.class).getTeam() instanceof HumanTeam).collect(Collectors.toList());
        int hunterSize = (int) Math.floor(gamePlayers.size() * hunterPercent);
        return hunterSize >= humans.size();
    }

    @Override
    public void onCancel() {
        this.onFinish();
        boolean noone = playerManager.getGamePlayer().size() + getDeathGamer() < McInfected.getApi().getConfigManager().getData("autoStart", Integer.class).orElse(2);
        if (noone) {
            GameEndTask.cancelGame(playerManager.getGamePlayer());
            Bukkit.broadcastMessage(McInfected.getApi().getConfigManager().getMessage("Error.Game.Not Enough Players"));
        } else {
            MinigamesCore.getApi().getScheduleManager().jumpInto(mcinf.getGameEndState(), false);
        }
    }

    @Override
    public void onFinish() {
        this.notifiedHunter = false;
    }

    @Override
    public long run(long l) {
        if (l % 30 == 0 || l == 10 || l <= 5) {
            String time = MinigamesCore.getApi().getGameUtils().getTimeWithUnit(l);
            Bukkit.broadcastMessage(McInfected.getApi().getConfigManager().getMessage("Game.Time.Game").replace("<time>", time));
            SoundUtils.playGameSound(false);
        }
        if (l == McInfected.getApi().getConfigManager().getData("compassTime", Long.class).orElse(65L)) {
            playerManager.getGamePlayer()
                    .stream().filter(g -> g.castTo(TeamPlayer.class).getTeam() instanceof ZombieTeam)
                    .forEach(p -> {
                        p.getPlayer().getInventory().addItem(new ItemStack(Material.COMPASS));
                        McInfected.getApi().getConfigManager().getData("compassGain", String[].class).ifPresent(sound -> MinigamesCore.getApi().getGameUtils().playSound(p.getPlayer(), sound));
                    });
        }
        int level = (int) l;
        Bukkit.getOnlinePlayers().forEach(p -> p.setLevel(level));
        VotingTask.bossBar.setProgress((double) l / getTotalTime());
        VotingTask.updateBoard(l, playerManager.getGamePlayer(), "&c母體已出現");
        if (shouldHunterActivate(playerManager.getGamePlayer()) && !notifiedHunter) {
            VotingTask.hunterBossBar.setVisible(true);
            VotingTask.bossBar.setVisible(false);
            VotingTask.updateHunterBossBar(playerManager.getGamePlayer());
            playerManager.getGamePlayer().stream().filter(g -> g.castTo(TeamPlayer.class).getTeam() instanceof HumanTeam).forEach(g -> {
                Player player = g.getPlayer();
                McInfected.getApi().getConfigManager().getData("hunterActive", String[].class).ifPresent(s -> MinigamesCore.getApi().getGameUtils().playSound(player, s));
                player.sendTitle("", "§a按 F 可以化身成幽靈獵手。", 0, 100, 0);
            });
            this.notifiedHunter = true;
        }
        return l;
    }

    @Override
    public long getTotalTime() {
        return McInfected.getApi().getConfigManager().getData("gameTime", Long.class).orElse(150L);
    }

    private long getDeathGamer() {
        return playerManager.getSpectators().stream().filter(e -> e.castTo(TeamPlayer.class).getTeam() != null).count();
    }

    @Override
    public boolean shouldCancel() {
        boolean noone = playerManager.getGamePlayer().size() + getDeathGamer() < McInfected.getApi().getConfigManager().getData("autoStart", Integer.class).orElse(2);
        boolean normalEnd = playerManager.getGamePlayer().stream().noneMatch(g -> g.castTo(TeamPlayer.class).getTeam() instanceof HumanTeam) || playerManager.getGamePlayer().stream().noneMatch(g -> g.castTo(TeamPlayer.class).getTeam() instanceof ZombieTeam);
        return noone || normalEnd;
    }
}