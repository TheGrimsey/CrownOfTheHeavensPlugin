package net.thegrimsey.crownoftheheavens;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class CrownEventListener implements Listener {
    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event)
    {
        CrownOfTheHeavens crownOfTheHeavens = CrownOfTheHeavens.INSTANCE;
        if(crownOfTheHeavens.IsNotCrownHolder(event.getPlayer()))
            return;

        //Crown Holder has quit.
        crownOfTheHeavens.RemoveCrownFromPlayer(event.getPlayer());
        crownOfTheHeavens.OnCrownHolderRemoved();
        crownOfTheHeavens.BroadcastEventMessage(crownOfTheHeavens.ARTIFACT_LOGGED_OUT, crownOfTheHeavens.CROWN_NAME, event.getPlayer().getDisplayName());
    }

    @EventHandler
    public void onEntityPickupItemEvent(EntityPickupItemEvent event)
    {
        CrownOfTheHeavens crownOfTheHeavens = CrownOfTheHeavens.INSTANCE;
        if(!crownOfTheHeavens.IsCrownOfTheHeavens(event.getItem().getItemStack()))
            return;

        if(event.getEntity().getType() != EntityType.PLAYER)
        {
            event.setCancelled(true);
            return;
        }

        crownOfTheHeavens.AssignNewCrownHolder((Player)event.getEntity());
    }

    @EventHandler
    public void onPlayerDropItemEvent(PlayerDropItemEvent event)
    {
        CrownOfTheHeavens crownOfTheHeavens = CrownOfTheHeavens.INSTANCE;
        if(!crownOfTheHeavens.IsCrownOfTheHeavens(event.getItemDrop().getItemStack()))
            return;
        event.getItemDrop().remove();

        if(crownOfTheHeavens.crownItem != null)
            return;

        crownOfTheHeavens.OnCrownHolderRemoved();
        crownOfTheHeavens.BroadcastEventMessage(crownOfTheHeavens.ARTIFACT_DROPPED, crownOfTheHeavens.CROWN_NAME, event.getPlayer().getDisplayName());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        CrownOfTheHeavens crownOfTheHeavens = CrownOfTheHeavens.INSTANCE;
        if(crownOfTheHeavens.IsNotCrownHolder(event.getEntity()))
            return;
        event.getDrops().removeIf(stack -> CrownOfTheHeavens.INSTANCE.IsCrownOfTheHeavens(stack));

        crownOfTheHeavens.OnCrownHolderRemoved();
        crownOfTheHeavens.BroadcastEventMessage(crownOfTheHeavens.ARTIFACT_DIED, crownOfTheHeavens.CROWN_NAME, event.getEntity().getDisplayName());
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event)
    {
        CrownOfTheHeavens crownOfTheHeavens = CrownOfTheHeavens.INSTANCE;
        if(crownOfTheHeavens.IsNotCrownHolder(event.getPlayer()))
            return;

        if(event.getItem().getType() == Material.POTION || event.getItem().getType() == Material.CHORUS_FRUIT)
            return;

        event.getPlayer().sendMessage(crownOfTheHeavens.CROWN_ERROR_EATING);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event)
    {
        if(CrownOfTheHeavens.INSTANCE.IsCrownOfTheHeavens(event.getPlayerItem()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryPickupEvent(InventoryPickupItemEvent event)
    {
        if(CrownOfTheHeavens.INSTANCE.IsCrownOfTheHeavens(event.getItem().getItemStack()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event)
    {
        CrownOfTheHeavens crownOfTheHeavens = CrownOfTheHeavens.INSTANCE;
        if(!crownOfTheHeavens.IsCrownOfTheHeavens(event.getCursor()) && !crownOfTheHeavens.IsCrownOfTheHeavens(event.getCurrentItem()))
            return;

        boolean IsQuickMoveToOther = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getView().getTopInventory().getType() != InventoryType.PLAYER;
        if(event.getClickedInventory() == null || !IsQuickMoveToOther && event.getClickedInventory().getType() == InventoryType.PLAYER)
            return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if(event.getRightClicked().getType() != EntityType.ITEM_FRAME)
            return;

        CrownOfTheHeavens crownOfTheHeavens = CrownOfTheHeavens.INSTANCE;
        if(event.getPlayer().getUniqueId() != crownOfTheHeavens.crownHolder)
            return;

        ItemStack mainHandItem = event.getPlayer().getInventory().getItemInMainHand();
        ItemStack offHandItem = event.getPlayer().getInventory().getItemInOffHand();
        boolean isMainHandCrown = crownOfTheHeavens.IsCrownOfTheHeavens(mainHandItem);
        boolean mainHandAir = mainHandItem.getType() == Material.AIR;
        boolean offHandCrown = crownOfTheHeavens.IsCrownOfTheHeavens(offHandItem);
        if((mainHandAir && !offHandCrown) || !isMainHandCrown)
            return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event)
    {
        if(event.getEntity().getKiller() == null || CrownOfTheHeavens.INSTANCE.IsNotCrownHolder(event.getEntity().getKiller()))
            return;

        if(event.getEntity().getType() == EntityType.PLAYER)
        {
            event.getEntity().getKiller().addPotionEffect(PotionEffectType.SATURATION.createEffect(1, 5));
            event.getEntity().getKiller().addPotionEffect(PotionEffectType.REGENERATION.createEffect(5, 1));
            event.getEntity().getKiller().addPotionEffect(PotionEffectType.GLOWING.createEffect(30, 1));
        }
    }

    @EventHandler
    public void onItemDespawnEvent(ItemDespawnEvent event)
    {
        if(!CrownOfTheHeavens.INSTANCE.IsCrownOfTheHeavens(event.getEntity().getItemStack()))
            return;

        CrownOfTheHeavens.INSTANCE.RespawnCrown();
    }
}
