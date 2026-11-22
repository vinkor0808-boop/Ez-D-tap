```java
package snow.dtap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class DTapClient implements ClientModInitializer {
/** Wie viele Crystals pro Klick (2 = Double Pop, 3 = Triple Pop). */
public static final int POPS = 2;

static BlockPos obbyPos;
static int stage = 0, timeout = 0, popped = 0;

@Override
public void onInitializeClient() {
ClientTickEvents.START_CLIENT_TICK.register(DTapClient::tick);
}

public static void onObbyPlaced(BlockPos pos) {
MinecraftClient mc = MinecraftClient.getInstance();
if (mc.player == null) return;
int slot = findCrystalSlot(mc);
if (slot < 0) return;
mc.player.getInventory().setSelectedSlot(slot);
obbyPos = pos;
stage = 1;
popped = 0;
timeout = 60;
}

static void tick(MinecraftClient mc) {
if (mc.player == null || mc.world == null || mc.interactionManager == null || stage == 0) return;
if (--timeout <= 0) { stage = 0; return; }

if (stage == 1) {
if (mc.options.attackKey.wasPressed()) stage = 2;
return;
}
if (stage == 2) {
if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) { stage = 0; return; }
BlockHitResult hit = new BlockHitResult(
Vec3d.ofCenter(obbyPos).add(0, 0.5, 0), Direction.UP, obbyPos, false);
mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
mc.player.swingHand(Hand.MAIN_HAND);
stage = 3;
return;
}
if (stage == 3) {
List<EndCrystalEntity> list = mc.world.getEntitiesByClass(
EndCrystalEntity.class, new Box(obbyPos.up()).expand(0.5), e -> e.isAlive());
if (list.isEmpty()) return;
mc.interactionManager.attackEntity(mc.player, list.get(0));
mc.player.swingHand(Hand.MAIN_HAND);
popped++;
stage = popped < POPS ? 2 : 0;
}
}

static int findCrystalSlot(MinecraftClient mc) {
for (int i = 0; i < 9; i++)
if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) return i;
return -1;
}
}
```
