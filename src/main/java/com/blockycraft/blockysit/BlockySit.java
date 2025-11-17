package com.blockycraft.blockysit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import java.util.HashMap;
import java.util.Map;

public class BlockySit extends JavaPlugin implements Listener {

    private Map<Player, Minecart> seatedPlayers = new HashMap<Player, Minecart>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // Agendador para verificar se os jogadores ainda estão nos carrinhos
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                checkSeatedPlayers();
            }
        }, 0L, 10L); // Verifica a cada 10 ticks (0.5 segundos)

        System.out.println("[BlockySit] Ativado. Clique com o botao direito em uma cadeira para sentar.");
    }

    @Override
    public void onDisable() {
        // Remove apenas os carrinhos criados pelo plugin
        for (Minecart seat : seatedPlayers.values()) {
            if (seat != null && !seat.isDead()) {
                seat.remove();
            }
        }
        seatedPlayers.clear();
        System.out.println("[BlockySit] Desativado.");
    }

    // Verifica periodicamente o estado dos jogadores sentados
    private void checkSeatedPlayers() {
        for (Player player : seatedPlayers.keySet().toArray(new Player[0])) {
            if (!player.isOnline()) {
                continue;
            }

            Minecart seat = seatedPlayers.get(player);

            // Verifica se o jogador está sneakando (pressionando Shift)
            if (player.isSneaking()) {
                removeSeat(player, seat);
                continue;
            }

            // Verifica se o carrinho ainda existe
            if (seat == null || seat.isDead()) {
                seatedPlayers.remove(player);
                continue;
            }

            // Verifica se o jogador ainda está no carrinho
            if (seat.getPassenger() == null || !seat.getPassenger().equals(player)) {
                removeSeat(player, seat);
                continue;
            }

            // Impede que o carrinho se mova
            seat.setVelocity(new Vector(0, 0, 0));
        }
    }

    private void removeSeat(Player player, Minecart seat) {
        if (seat != null && !seat.isDead()) {
            seat.remove();
        }
        seatedPlayers.remove(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Material clickedBlockType = event.getClickedBlock().getType();
            if (clickedBlockType == Material.WOOD_STAIRS || clickedBlockType == Material.COBBLESTONE_STAIRS) {

                // Verifica se o jogador já está sentado
                if (seatedPlayers.containsKey(player)) {
                    event.setCancelled(true);
                    return;
                }

                Location sitLocation = event.getClickedBlock().getLocation().add(0.5, 0.55, 0.5);
                Minecart seat = player.getWorld().spawn(sitLocation, Minecart.class);
                seat.setVelocity(new Vector(0, 0, 0));
                seat.setMaxSpeed(0);
                seat.setDamage(-999999999);
                seat.setPassenger(player);

                seatedPlayers.put(player, seat);

                event.setCancelled(true);
            }
        }

    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Quando um jogador sai do servidor, remove apenas o carrinho criado por nós
        Player player = event.getPlayer();

        if (seatedPlayers.containsKey(player)) {
            Minecart seat = seatedPlayers.get(player);
            removeSeat(player, seat);
        }
    }
}