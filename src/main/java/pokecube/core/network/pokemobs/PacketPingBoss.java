package pokecube.core.network.pokemobs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import thut.core.common.network.Packet;

public class PacketPingBoss extends Packet
{
    public static void onNewBossEvent(final IPokemob pokemob)
    {
        final PacketPingBoss packet = new PacketPingBoss();
        packet.entityId = pokemob.getEntity().getId();
        PokecubeCore.packets.sendToTracking(packet, pokemob.getEntity());
    }

    public int entityId;

    public PacketPingBoss()
    {
    }

    public PacketPingBoss(final PacketBuffer buffer)
    {
        this.entityId = buffer.readInt();
    }

    @Override
    public void handleServer(final ServerPlayerEntity player)
    {
        final int id = this.entityId;
        final Entity e = PokecubeCore.getEntityProvider().getEntity(player.getCommandSenderWorld(), id, true);
        final IPokemob pokemob = CapabilityPokemob.getPokemobFor(e);
        if (pokemob != null && pokemob.getBossInfo() != null) pokemob.getBossInfo().addPlayer(player);
    }

    @Override
    public void handleClient()
    {
        final PacketPingBoss packet = new PacketPingBoss();
        packet.entityId = this.entityId;
        PokecubeCore.packets.sendToServer(packet);
    }

    @Override
    public void write(final PacketBuffer buffer)
    {
        buffer.writeInt(this.entityId);
    }
}