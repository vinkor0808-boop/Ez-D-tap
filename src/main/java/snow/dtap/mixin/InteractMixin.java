package snow.dtap.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snow.dtap.DTapClient;

@Mixin(ClientPlayerInteractionManager.class)
public class InteractMixin {
    @Unique private boolean snow$wasObby;

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void snow$head(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
                           CallbackInfoReturnable<ActionResult> cir) {
        snow$wasObby = player.getStackInHand(hand).isOf(Items.OBSIDIAN);
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void snow$ret(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
                          CallbackInfoReturnable<ActionResult> cir) {
        if (snow$wasObby && cir.getReturnValue().isAccepted())
            DTapClient.onObbyPlaced(hit.getBlockPos().offset(hit.getSide()));
    }
}
