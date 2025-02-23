package net.minestom.demo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.EntityVelocityEvent;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemHideFlag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.BundleMeta;
import net.minestom.server.listener.PlayerVehicleListener;
import net.minestom.server.monitoring.BenchmarkManager;
import net.minestom.server.monitoring.TickMonitor;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerRotationPacket;
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.utils.time.TimeUnit;
import net.minestom.server.world.DimensionType;

import java.time.Duration;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerInit {

    private static final Inventory inventory;

    private static final EventNode<Event> DEMO_NODE = EventNode.all("demo")
            .addListener(EntityAttackEvent.class, event -> {
                final Entity source = event.getEntity();
                final Entity entity = event.getTarget();

                entity.takeKnockback(0.4f, Math.sin(source.getPosition().yaw() * 0.017453292), -Math.cos(source.getPosition().yaw() * 0.017453292));

                if (entity instanceof Player) {
                    Player target = (Player) entity;
                    target.damage(Damage.fromEntity(source, 5));
                }

                if (source instanceof Player) {
                    ((Player) source).sendMessage("You attacked something!");
                }
            })
            .addListener(PlayerDeathEvent.class, event -> event.setChatMessage(Component.text("custom death message")))
            .addListener(PickupItemEvent.class, event -> {
                final Entity entity = event.getLivingEntity();
                if (entity instanceof Player) {
                    // Cancel event if player does not have enough inventory space
                    final ItemStack itemStack = event.getItemEntity().getItemStack();
                    event.setCancelled(!((Player) entity).getInventory().addItemStack(itemStack));
                }
            })
            .addListener(ItemDropEvent.class, event -> {
                final Player player = event.getPlayer();
                ItemStack droppedItem = event.getItemStack();

                Pos playerPos = player.getPosition();
                ItemEntity itemEntity = new ItemEntity(droppedItem);
                itemEntity.setPickupDelay(Duration.of(500, TimeUnit.MILLISECOND));
                itemEntity.setInstance(player.getInstance(), playerPos.withY(y -> y + 1.5));
                Vec velocity = playerPos.direction().mul(6);
                itemEntity.setVelocity(velocity);
            })
            .addListener(PlayerDisconnectEvent.class, event -> System.out.println("DISCONNECTION " + event.getPlayer().getUsername()))

            .addListener(PlayerLoginEvent.class, event -> {
                final Player player = event.getPlayer();

                var instances = MinecraftServer.getInstanceManager().getInstances();
                Instance instance = instances.stream().skip(new Random().nextInt(instances.size())).findFirst().orElse(null);
                event.setSpawningInstance(instance);
                int x = Math.abs(ThreadLocalRandom.current().nextInt()) % 500 - 250;
                int z = Math.abs(ThreadLocalRandom.current().nextInt()) % 500 - 250;
                player.setRespawnPoint(new Pos(0, 42f, 0));
            })
            .addListener(PlayerSpawnEvent.class, event -> {
                final Player player = event.getPlayer();
                player.setGameMode(GameMode.CREATIVE);
                player.setPermissionLevel(4);
                player.getInventory().addItemStack(ItemStack.builder(Material.STONE)
                        .displayName("<rainbow>Stones ;)")
                        .lore("<red>Stone 1", "<green>Stone 2", "<blue>Stone 3")
                        .amount(64)
                        .meta(itemMetaBuilder ->
                                itemMetaBuilder.canPlaceOn(Set.of(Block.STONE))
                                        .canDestroy(Set.of(Block.DIAMOND_ORE)))
                        .build());

                player.getInventory().addItemStack(ItemStack.of(Material.STONE)
                        .withDisplayName("<rainbow>Stones ;)")
                        .withLore("<red>Stone 1", "<green>Stone 2", "<blue>Stone 3")
                        .withAmount(64)
                        .withMeta(itemMetaBuilder ->
                                itemMetaBuilder.canPlaceOn(Set.of(Block.STONE))
                                        .canDestroy(Set.of(Block.DIAMOND_ORE)))
                );

                ItemStack bundle = ItemStack.builder(Material.BUNDLE)
                        .lore("<red>Bundle 1", "<green>Bundle 2", "<blue>Bundle 3")
                        .meta(BundleMeta.class, bundleMetaBuilder -> {
                            bundleMetaBuilder.addItem(ItemStack.of(Material.DIAMOND, 5));
                            bundleMetaBuilder.addItem(ItemStack.of(Material.RABBIT_FOOT, 5));
                            bundleMetaBuilder.hideFlag(ItemHideFlag.values());
                        })
                        .build();
                player.getInventory().addItemStack(bundle);

            })
            .addListener(PlayerSettingsChangeEvent.class, event -> {
                //event.getPlayer().sendMessage(event.getPlayer().getSettings().getDisplayedSkinParts() + " Byte");
            })
            .addListener(PlayerPacketOutEvent.class, event -> {
                //System.out.println("out " + event.getPacket().getClass().getSimpleName());
            })
            .addListener(PlayerPacketEvent.class, event -> {
                //System.out.println("in " + event.getPacket().getClass().getSimpleName());
            })
            .addListener(PlayerEntityInteractEvent.class, event -> {
                if (event.getHand() == Player.Hand.MAIN)
                    if (event.getPlayer().getItemInMainHand().material() == Material.AIR) {
                        if (event.getTarget().getVehicle() != null) {
                            if (event.getTarget().getVehicle().equals(event.getPlayer())) {
                                event.getTarget().getVehicle().removePassenger(event.getTarget());
                            }
                        } else {
                            if (event.getPlayer().isSneaking())
                                event.getTarget().addPassenger(event.getPlayer());
                            else
                                event.getPlayer().addPassenger(event.getTarget());

                        }
                    }
            })
            .addListener(PlayerUnmountVehicleEvent.class, event -> {

            });

    static {
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        InstanceContainer instanceContainer = instanceManager.createInstanceContainer(DimensionType.OVERWORLD);
        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));

        if (false) {
            System.out.println("start");
            ChunkUtils.forChunksInRange(0, 0, 10, (x, z) -> instanceContainer.loadChunk(x, z).join());
            System.out.println("load end");
        }

        inventory = new Inventory(InventoryType.CHEST_1_ROW, Component.text("Test inventory"));
        inventory.setItemStack(3, ItemStack.of(Material.DIAMOND, 34));
    }

    private static final AtomicReference<TickMonitor> LAST_TICK = new AtomicReference<>();

    public static void init() {
        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addChild(DEMO_NODE);

        eventHandler.addListener(ServerTickMonitorEvent.class, event -> LAST_TICK.set(event.getTickMonitor()));

        BenchmarkManager benchmarkManager = MinecraftServer.getBenchmarkManager();
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            Collection<Player> players = MinecraftServer.getConnectionManager().getOnlinePlayers();
            if (players.isEmpty())
                return;

            long ramUsage = benchmarkManager.getUsedMemory();
            ramUsage /= 1e6; // bytes to MB

            TickMonitor tickMonitor = LAST_TICK.get();
            final Component header = Component.text("RAM USAGE: " + ramUsage + " MB")
                    .append(Component.newline())
                    .append(Component.text("TICK TIME: " + MathUtils.round(tickMonitor.getTickTime(), 2) + "ms"))
                    .append(Component.newline())
                    .append(Component.text("ACQ TIME: " + MathUtils.round(tickMonitor.getAcquisitionTime(), 2) + "ms"));
            final Component footer = benchmarkManager.getCpuMonitoringMessage();
            Audiences.players().sendPlayerListHeaderAndFooter(header, footer);
        }).repeat(10, TimeUnit.SERVER_TICK).schedule();
       /* MinecraftServer.getSchedulerManager().buildTask(() -> {
            Collection<Player> players = MinecraftServer.getConnectionManager().getOnlinePlayers();
            if (players.isEmpty())
                return;
            players.forEach(player -> {
                player.sendMessage("shouldUnmount: " + player.getVehicleInformation().shouldUnmount());
            });
        }).repeat(20, TimeUnit.SERVER_TICK).schedule();*/
    }
}
