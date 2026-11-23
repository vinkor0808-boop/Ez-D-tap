package snow.dtap.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snow.dtap.DTapClient;

@Mixin(ClientPlayerInteractionManager.class)
public class InteractMixin {
    @Unique private boolean snow$wasObby;

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void snow$head(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
                           CallbackInfoReturnable<ActionResult> cir) {
        snow$wasObby = player.getStackInHand(hand).isOf(Items.OBSIDIAN);
        // Waehrend die Mod gleich auf Crystal wechselt: kein zweites Obby setzen, wenn du schon Rechtsklick spammst
        if (snow$wasObby && DTapClient.isSwitchPending()) {
            snow$wasObby = false;
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void snow$ret(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
                          CallbackInfoReturnable<ActionResult> cir) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (!snow$wasObby || !cir.getReturnValue().isAccepted() || world == null) return;
        // Normalerweise landet das Obby neben dem angeklickten Block.
        // Bei Gras/Schnee/Blumen ersetzt es aber den angeklickten Block selbst.
        BlockPos side = hit.getBlockPos().offset(hit.getSide());
        if (world.getBlockState(side).isOf(Blocks.OBSIDIAN))
            DTapClient.onObbyPlaced(side);
        else if (world.getBlockState(hit.getBlockPos()).isOf(Blocks.OBSIDIAN))
            DTapClient.onObbyPlaced(hit.getBlockPos());
    }
}
