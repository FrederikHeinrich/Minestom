package net.minestom.server.network.packet.server.play;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minestom.server.network.NetworkBuffer.*;

/**
 * See <a href="https://wiki.vg/Protocol#Damage_Event">https://wiki.vg/Protocol#Damage_Event</a> for more info.
 *
 * @param targetEntityId ID of the entity being damaged
 * @param damageTypeId   0 if there is no damage type, otherwise the damage type ID + 1
 * @param sourceEntityId 0 if there is no source entity, otherwise it is entityId + 1
 * @param sourceDirectId 0 if there is no direct source. For direct attacks (e.g. melee), this is the same as sourceEntityId. For indirect attacks (e.g. projectiles), this is the projectile entity id + 1
 * @param sourcePos      null if there is no source position, otherwise the position of the source
 */
public record DamageEventPacket(int targetEntityId, int damageTypeId, int sourceEntityId, int sourceDirectId, @Nullable Point sourcePos) implements ServerPacket {

    public DamageEventPacket(@NotNull NetworkBuffer reader) {
        this(reader.read(VAR_INT), reader.read(VAR_INT), reader.read(VAR_INT), reader.read(VAR_INT),
            reader.read(BOOLEAN) ? new Vec(reader.read(DOUBLE), reader.read(DOUBLE), reader.read(DOUBLE)) : null
        );
    }
    @Override
    public int getId() {
        return ServerPacketIdentifier.DAMAGE_EVENT;
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(VAR_INT, targetEntityId);
        writer.write(VAR_INT, damageTypeId);
        writer.write(VAR_INT, sourceEntityId);
        writer.write(VAR_INT, sourceDirectId);
        boolean hasSourcePos = sourcePos != null;
        writer.write(BOOLEAN, hasSourcePos);
        if (hasSourcePos) {
            writer.write(DOUBLE, sourcePos.x());
            writer.write(DOUBLE, sourcePos.y());
            writer.write(DOUBLE, sourcePos.z());
        }
    }
}
