package com.kreidev.cmverticaladditions;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;

@EventBusSubscriber(Dist.CLIENT)
public class VerticalBeltConnectorHandler {

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;

        if (player == null || level == null)
            return;
        if (Minecraft.getInstance().screen != null)
            return;

        RandomSource random = level.random;

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack heldItem = player.getItemInHand(hand);

            if (!VerticalAdditions.VERTICAL_BELT_CONNECTOR.isIn(heldItem))
                continue;

            if (!heldItem.has(AllDataComponents.BELT_FIRST_SHAFT))
                continue;

            BlockPos first = heldItem.get(AllDataComponents.BELT_FIRST_SHAFT);

            //noinspection DataFlowIssue
            if (!level.getBlockState(first)
                    .hasProperty(BlockStateProperties.AXIS))
                continue;
            Direction.Axis axis = level.getBlockState(first)
                    .getValue(BlockStateProperties.AXIS);

            HitResult rayTrace = Minecraft.getInstance().hitResult;
            if (!(rayTrace instanceof BlockHitResult)) {
                if (random.nextInt(50) == 0) {
                    level.addParticle(new DustParticleOptions(new Vector3f(.3f, .9f, .5f), 1),
                            first.getX() + .5f + randomOffset(random, .25f), first.getY() + .5f + randomOffset(random, .25f),
                            first.getZ() + .5f + randomOffset(random, .25f), 0, 0, 0);
                }
                return;
            }

            BlockPos selected = ((BlockHitResult) rayTrace).getBlockPos();

            if (level.getBlockState(selected)
                    .canBeReplaced())
                return;
            if (!ShaftBlock.isShaft(level.getBlockState(selected)))
                selected = selected.relative(((BlockHitResult) rayTrace).getDirection());
            if (!selected.closerThan(first, AllConfigs.server().kinetics.maxBeltLength.get()))
                return;

            boolean canConnect = VerticalBeltConnectorItem.validateAxis(level, selected) && VerticalBeltConnectorItem.canConnect(level, first, selected);

            Vec3 start = Vec3.atLowerCornerOf(first);
            Vec3 end = Vec3.atLowerCornerOf(selected);
            Vec3 actualDiff = end.subtract(start);
            end = end.subtract(axis.choose(actualDiff.x, 0, 0), axis.choose(0, actualDiff.y, 0),
                    axis.choose(0, 0, actualDiff.z));
            Vec3 diff = end.subtract(start);

            double length = Math.abs(diff.y);

            Vec3 step = new Vec3(0, Math.signum(diff.y), 0);
            for (float f = 0; f < length; f += .0625f) {
                Vec3 position = start.add(step.scale(f));
                if (random.nextInt(10) == 0) {
                    level.addParticle(
                            new DustParticleOptions(new Vector3f(canConnect ? .3f : .9f, canConnect ? .9f : .3f, .5f), 1),
                            position.x + .5f, position.y + .5f, position.z + .5f, 0, 0, 0);
                }
            }

            return;
        }
    }

    private static float randomOffset(RandomSource random, float range) {
        return (random.nextFloat() - .5f) * 2 * range;
    }
}
