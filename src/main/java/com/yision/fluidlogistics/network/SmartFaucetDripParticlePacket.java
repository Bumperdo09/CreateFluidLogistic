package com.yision.fluidlogistics.network;

import com.simibubi.create.content.fluids.FluidFX;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

public record SmartFaucetDripParticlePacket(BlockPos faucetPos, FluidStack fluid) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, SmartFaucetDripParticlePacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, SmartFaucetDripParticlePacket::faucetPos,
            FluidStack.STREAM_CODEC, SmartFaucetDripParticlePacket::fluid,
            SmartFaucetDripParticlePacket::new
        );

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (fluid.isEmpty()) {
            return;
        }

        Vec3 spoutPos = Vec3.atCenterOf(faucetPos).add(0, -0.3, 0);
        player.level().addParticle(FluidFX.getDrippingParticle(fluid), spoutPos.x, spoutPos.y - 0.02, spoutPos.z, 0.0, 0.0,
            0.0);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.SMART_FAUCET_DRIP_PARTICLE;
    }
}
