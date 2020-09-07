package net.thegrimsey.crownoftheheavens;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public class CrownOfTheHeavens extends JavaPlugin {
    public static CrownOfTheHeavens INSTANCE;

    ItemStack Crown = null;
    NamespacedKey crownKey = null;

    // Config
    Location crownSpawnLocation = null;

    String CROWN_NAME;
    List<String> CROWN_LORE;

    double CROWN_HEALTH;
    double CROWN_ATTACK_DAMAGE;
    double CROWN_ATTACK_KNOCKBACK;
    double CROWN_RESIST_KNOCKBACK;
    double CROWN_MOVEMENTSPEED;
    String CROWN_ERROR_EATING;

    String ARTIFACT_PICKED_UP;
    String ARTIFACT_DROPPED;
    String ARTIFACT_DIED;
    String ARTIFACT_LOGGED_OUT;
    String ARTIFACT_RETURNED;


    // Runtime
    UUID crownHolder = null;
    Item crownItem = null;

    @Override
    public void onEnable() {
        INSTANCE = this;
        crownKey = new NamespacedKey(this, "CROWN");

        LoadConfig();

        getServer().getPluginManager().registerEvents(new CrownEventListener(), this);

        CreateCrownItemStack();
        crownSpawnLocation = new Location(getServer().getWorlds().get(0), 0.5, 65.5, 0.5);
        RespawnCrown();
    }

    private void LoadConfig() {
        saveDefaultConfig();
        CROWN_NAME = getConfig().getString("crown.name");
        CROWN_LORE = getConfig().getStringList("crown.lore");
        CROWN_HEALTH = getConfig().getDouble("crown.health");
        CROWN_ATTACK_DAMAGE = getConfig().getDouble("crown.attack-damage");
        CROWN_ATTACK_KNOCKBACK = getConfig().getDouble("crown.attack-knockback");
        CROWN_RESIST_KNOCKBACK = getConfig().getDouble("crown.resist-knockback");
        CROWN_MOVEMENTSPEED = getConfig().getDouble("crown.movementspeed");

        CROWN_ERROR_EATING = getConfig().getString("crown.error-eating");

        ARTIFACT_PICKED_UP = getConfig().getString("artifact.picked-up");
        ARTIFACT_DROPPED = getConfig().getString("artifact.dropped");
        ARTIFACT_DIED = getConfig().getString("artifact.died");
        ARTIFACT_LOGGED_OUT = getConfig().getString("artifact.logged-out");
        ARTIFACT_RETURNED = getConfig().getString("artifact.returned");
    }

    @Override
    public void onDisable()
    {
        // Remove crown from holder when we shut down.
        if(HasCrownHolder())
        {
            Player player = getServer().getPlayer(crownHolder);
            if(player != null)
                RemoveCrownFromPlayer(player);
        }
        else if(crownItem != null)
        {
            crownItem.remove();
        }
    }

    private void CreateCrownItemStack() {
        Crown = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta crownMeta = Crown.getItemMeta();
        assert crownMeta != null;
        // Names & Lore.
        crownMeta.setDisplayName(ChatColor.RESET + CROWN_NAME);
        crownMeta.setLore(CROWN_LORE);
        // Flags & Enchants
        crownMeta.setUnbreakable(true);
        crownMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        crownMeta.addEnchant(Enchantment.OXYGEN, 12, true);
        crownMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        // Attributes.
        crownMeta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(UUID.randomUUID(), "GENERIC_MAX_HEALTH", CROWN_HEALTH, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        crownMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "GENERIC_ATTACK_DAMAGE", CROWN_ATTACK_DAMAGE, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        crownMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_KNOCKBACK, new AttributeModifier(UUID.randomUUID(), "GENERIC_ATTACK_KNOCKBACK", CROWN_ATTACK_KNOCKBACK, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        crownMeta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(UUID.randomUUID(), "GENERIC_KNOCKBACK_RESISTANCE", CROWN_RESIST_KNOCKBACK, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        crownMeta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(UUID.randomUUID(), "MOVEMENT_SPEED", CROWN_MOVEMENTSPEED, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        crownMeta.getPersistentDataContainer().set(crownKey, PersistentDataType.INTEGER, 1);

        Crown.setItemMeta(crownMeta);
    }

    void RemoveCrownFromPlayer(Player player)
    {
        if(player.getEquipment().getHelmet() != null && IsCrownOfTheHeavens(player.getEquipment().getHelmet()))
            player.getEquipment().setHelmet(new ItemStack(Material.AIR));

        ItemStack CrownCopy = Crown.clone();
        CrownCopy.setAmount(64*32);
        player.getInventory().removeItem(CrownCopy);
    }

    public void OnCrownHolderRemoved()
    {
        Player player = getServer().getPlayer(crownHolder);
        crownHolder = null;
        if(player == null)
            return;

        DropCrownAtLocation(player.getLocation());
        player.setHealth(0);
    }

    public boolean HasCrownHolder() { return crownHolder != null; }
    public boolean IsNotCrownHolder(Player player) { return crownHolder != player.getUniqueId(); }
    public boolean IsCrownOfTheHeavens(ItemStack itemStack)
    {
        return itemStack != null && itemStack.getItemMeta() != null &&
                itemStack.getItemMeta().getPersistentDataContainer().has(crownKey, PersistentDataType.INTEGER);
    }

    public void AssignNewCrownHolder(Player player)
    {
        crownHolder = player.getUniqueId();

        player.getWorld().strikeLightningEffect(player.getLocation());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10.0F, 0.5F);
        BroadcastEventMessage(ARTIFACT_PICKED_UP, CROWN_NAME, player.getDisplayName());
    }

    public void RespawnCrown()
    {
        DropCrownAtLocation(crownSpawnLocation);
        crownItem.setPersistent(true);

        BroadcastEventMessage(ARTIFACT_RETURNED, CROWN_NAME, "");
    }
    public void DropCrownAtLocation(Location location)
    {
        assert location.getWorld() != null;
        location.getWorld().strikeLightningEffect(location);

        if(crownItem != null)
        {
            crownItem.remove();
            crownItem = null;
        }

        crownItem = location.getWorld().dropItem(location, Crown);
        crownItem.setCustomName(CROWN_NAME);
        crownItem.setCustomNameVisible(true);
        crownItem.setInvulnerable(true);
        crownItem.setPickupDelay(4*20);
        crownItem.setVelocity(new Vector(0,0,0));
    }

    public void BroadcastEventMessage(String message, String artifactName, String playerName)
    {
        String formattedString = message.replace("{artifact}", artifactName);
        formattedString = formattedString.replace("{player}", playerName);

        getServer().broadcastMessage(formattedString);

    }
}
