package net.minestom.demo.commands;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class DisplayCommand extends Command {

    public DisplayCommand() {
        super("display");

        addSyntax(this::spawnItem, ArgumentType.Literal("item"));
        addSyntax(this::spawnBlock, ArgumentType.Literal("block"));
        addSyntax(this::spawnText, ArgumentType.Literal("text"));
        addSyntax(this::spawnTextWithContext, ArgumentType.Literal("text"), ArgumentType.Component("component"));
    }

    public void spawnItem(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player))
            return;

        var entity = new Entity(EntityType.ITEM_DISPLAY);
        var meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setHasNoGravity(true);
        meta.setItemStack(ItemStack.of(Material.STICK));
        entity.setInstance(player.getInstance(), player.getPosition().add(0,1.8,0));
    }

    public void spawnBlock(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player))
            return;

        var entity = new Entity(EntityType.BLOCK_DISPLAY);
        var meta = (BlockDisplayMeta) entity.getEntityMeta();
        meta.setHasNoGravity(true);
        meta.setBlockState(Block.STONE_STAIRS.stateId());
        entity.setInstance(player.getInstance(), player.getPosition().add(0,1.8,0));
    }

    public void spawnText(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player))
            return;

        var entity = new Entity(EntityType.TEXT_DISPLAY);
        var meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setHasNoGravity(true);
        meta.setText(Component.text("Hello, world!"));
        entity.setInstance(player.getInstance(), player.getPosition().add(0,1.8,0));
    }public void spawnTextWithContext(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player))
            return;

        var entity = new Entity(EntityType.TEXT_DISPLAY);
        var meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        Component component = context.get("component");
        meta.setHasNoGravity(true);
        meta.setText(component);
        meta.setUseDefaultBackground(false);
        entity.setInstance(player.getInstance(), player.getPosition().add(0,1.8,0));
    }
}
