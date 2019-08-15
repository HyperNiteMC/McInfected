package com.ericlam.mc.mcinfected.tasks;

import com.ericlam.mc.mcinfected.implement.team.HumanTeam;
import com.ericlam.mc.mcinfected.main.McInfected;
import com.ericlam.mc.minigames.core.character.GamePlayer;
import com.ericlam.mc.minigames.core.character.TeamPlayer;
import com.ericlam.mc.minigames.core.main.MinigamesCore;
import com.ericlam.mc.minigames.core.manager.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class GameEndTask extends InfTask {

    private static int humanWins = 0;
    private static int zombieWins = 0;
    private int currentRound = 0;

    public static String getTeamScore() {
        String zombieWin = ChatColor.RED.toString() + ChatColor.BOLD.toString() + GameEndTask.zombieWins + ChatColor.RESET.toString();
        String humanWin = ChatColor.GREEN.toString() + ChatColor.BOLD.toString() + GameEndTask.humanWins + ChatColor.RESET.toString();
        return zombieWin.concat("§7§l : §r").concat(humanWin);
    }

    static void cancelGame(List<GamePlayer> gamePlayers) {
        McInfected inf = McInfected.getPlugin(McInfected.class);
        MinigamesCore.getApi().getGameManager().endGame(gamePlayers, humanWins == zombieWins ? null : humanWins > zombieWins ? inf.getHumanTeam() : inf.getZombieTeam(), true);
    }

    private List<GamePlayer> getHumans() {
        return playerManager.getGamePlayer().stream().filter(g -> g.castTo(TeamPlayer.class).getTeam() instanceof HumanTeam).collect(Collectors.toList());
    }

    @Override
    public void initRun(PlayerManager playerManager) {
        boolean zombieWin = playerManager.getGamePlayer().stream().noneMatch(g -> g.castTo(TeamPlayer.class).getTeam() instanceof HumanTeam);
        String title = McInfected.getApi().getConfigManager().getPureMessage("Game.Over.".concat(zombieWin ? "Infected" : "Humans").concat(" Win"));
        Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle(title, "", 20, 100, 20));
        if (zombieWin) {
            zombieWins++;
            VotingTask.bossBar.setColor(BarColor.RED);
            GameTask.alphasZombies.forEach(p -> MinigamesCore.getApi().getGameStatsManager().addWins(p, 1));
        } else {
            getHumans().forEach(p -> {
                Player player = p.getPlayer();
                player.setGlowing(true);
                MinigamesCore.getApi().getFireWorkManager().spawnFireWork(player);
                MinigamesCore.getApi().getGameStatsManager().addWins(p, 1);
            });
            humanWins++;
            VotingTask.bossBar.setColor(BarColor.GREEN);
            Bukkit.broadcastMessage(McInfected.getApi().getConfigManager().getMessage("Game.Over.Survivors").replace("<humans>", getHumans().stream().map(e -> e.getPlayer().getDisplayName()).collect(Collectors.joining(", "))));
        }
        currentRound++;
        VotingTask.updateBoard(0, playerManager.getGamePlayer(), zombieWin ? "&4全部感染" : "&a抵抗成功");
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onFinish() {
        int maxRound = McInfected.getApi().getConfigManager().getData("maxRound", Integer.class).orElse(5);
        getHumans().forEach(p -> p.getPlayer().setGlowing(false));
        int matchPoint = (int) Math.ceil(maxRound / 2);
        if (matchPoint % 2 == 0) matchPoint++;
        String mpTitle = McInfected.getApi().getConfigManager().getPureMessage("Picture.Bar.Mp");
        if (zombieWins == matchPoint - 1 || humanWins == matchPoint - 1) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendTitle("", mpTitle, 0, 40, 20));
        }
        VotingTask.bossBar.setTitle(McInfected.getApi().getConfigManager().getPureMessage("Picture.Bar.Title")
                .replace("<z>", zombieWins + "")
                .replace("<h>", humanWins + ""));
        VotingTask.bossBar.setProgress(1.0);
        VotingTask.bossBar.setColor(BarColor.PURPLE);
        if (currentRound == maxRound || humanWins == matchPoint || zombieWins == matchPoint) {
            cancelGame(playerManager.getGamePlayer());
        } else {
            MinigamesCore.getApi().getScheduleManager().jumpInto(mcinf.getPreStartState(), false);
        }
    }

    @Override
    public long run(long l) {
        return l;
    }

    @Override
    public long getTotalTime() {
        return 7;
    }

    @Override
    public boolean shouldCancel() {
        return false;
    }
}
