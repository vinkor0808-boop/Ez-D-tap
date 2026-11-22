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

import net.minecraft.block.Blocks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DTapClient implements ClientModInitializer {
    /** Wie viele Crystals pro Klick (2 = Double Pop, 3 = Triple Pop). */
    public static final int POPS = 2;

    // Pausen in Ticks (1 Tick = 50 ms). Hoeher = langsamer, aber stabiler auf Servern.
    /** Pause nach dem Obby-Setzen, bevor auf Crystal gewechselt wird. */
    public static final int SWITCH_DELAY = 2;
    /** Pause zwischen Crystal setzen und Crystal zerstoeren. */
    public static final int BREAK_DELAY = 1;
    /** Pause nach dem Zerstoeren, bevor der naechste Crystal gesetzt wird. */
    public static final int PLACE_DELAY = 2;

    static int wait = 0;
    /** Wie lange schon auf den Crystal gewartet wird, und wie oft neu gesetzt wurde. */
    static int spawnWait = 0, retries = 0;

    static BlockPos obbyPos;
    static int stage = 0, timeout = 0, popped = 0;
    /** Slot mit dem Obby (zum Zurueckwechseln) und Slot mit den Crystals. */
    static int obbySlot = -1, crystalSlot = -1;
    /** Crystal-Wechsel erst im naechsten Tick, nicht im selben Moment wie das Obby-Setzen. */
    static boolean pendingSwitch = false;
    /** IDs der Crystals, die wir schon angegriffen haben (bis der Server sie entfernt, sind sie client-seitig noch da). */
    static final Set<Integer> attacked = new HashSet<>();

    @Override
    public void onInitializeClient() {
        // START, damit der Klick abgefangen wird, bevor Vanilla ihn benutzt
        ClientTickEvents.START_CLIENT_TICK.register(DTapClient::tick);
    }

    /** Wird vom Mixin aufgerufen, sobald Obsidian platziert wurde. */
    public static void onObbyPlaced(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        int slot = findCrystalSlot(mc);
        if (slot < 0) return;
        obbySlot = mc.player.getInventory().getSelectedSlot();
        crystalSlot = slot;
        pendingSwitch = true;   // Wechsel passiert nach SWITCH_DELAY
        wait = SWITCH_DELAY;
        obbyPos = pos;
        stage = 1;
        popped = 0;
        retries = 0;
        spawnWait = 0;
        attacked.clear();
        timeout = 60; // 3 Sekunden Zeit fuer den Klick
    }

    static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || stage == 0) return;
        if (--timeout <= 0) { stage = 0; return; }

        if (stage == 1) {               // warten auf deinen einen Linksklick
            // Server hat das Obby abgelehnt (Ghost Block)? -> zurueck auf Obby, damit du direkt neu setzen kannst
            if (obbyGone(mc)) { backToObby(mc); return; }
            if (pendingSwitch) {
                if (wait > 0) { wait--; return; }
                mc.player.getInventory().setSelectedSlot(crystalSlot);
                pendingSwitch = false;
                return;
            }
            if (mc.options.attackKey.wasPressed()) {
                stage = 2;
                timeout = 40; // nach dem Klick nochmal 2 Sekunden fuer die Pops
            }
            return;
        }
        if (wait > 0) { wait--; return; }   // Pause zwischen den Aktionen
        if (stage == 2) {               // Crystal aufs Obby setzen
            if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) { stage = 0; return; }
            if (obbyGone(mc)) { backToObby(mc); return; }
            // Alter Crystal noch da? Erst warten, bis der Server ihn entfernt hat, sonst blockiert er den Platz
            if (!crystalsOnObby(mc, true).isEmpty()) return;
            BlockHitResult hit = new BlockHitResult(
                    Vec3d.ofCenter(obbyPos).add(0, 0.5, 0), Direction.UP, obbyPos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);
            stage = 3;
            wait = BREAK_DELAY;
            spawnWait = 0;
            return;
        }
        if (stage == 3) {               // Crystal zerstoeren
            if (obbyGone(mc)) { backToObby(mc); return; }
            List<EndCrystalEntity> list = crystalsOnObby(mc, false);
            if (list.isEmpty()) {       // Server hat den neuen noch nicht gespawnt
                // Crystal kam nicht an (Setzen verschluckt)? -> nochmal setzen, max. 2 Versuche
                if (++spawnWait > 8) {
                    spawnWait = 0;
                    if (++retries > 2) { stage = 0; return; }
                    stage = 2;
                }
                return;
            }
            EndCrystalEntity crystal = list.get(0);
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            attacked.add(crystal.getId());
            popped++;
            stage = popped < POPS ? 2 : 0;
            wait = PLACE_DELAY;
        }
    }

    /** Crystals auf dem Obby: alreadyAttacked=true -> nur schon angegriffene, false -> nur neue. */
    static List<EndCrystalEntity> crystalsOnObby(MinecraftClient mc, boolean alreadyAttacked) {
        return mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(obbyPos.up()).expand(0.5),
                e -> e.isAlive() && attacked.contains(e.getId()) == alreadyAttacked);
    }

    static boolean obbyGone(MinecraftClient mc) {
        return !mc.world.getBlockState(obbyPos).isOf(Blocks.OBSIDIAN);
    }

    /** Obby ist weg (Ghost Block) -> zurueck auf den Obby-Slot, damit du direkt neu setzen kannst. */
    static void backToObby(MinecraftClient mc) {
        if (obbySlot >= 0 && mc.player.getInventory().getStack(obbySlot).isOf(Items.OBSIDIAN))
            mc.player.getInventory().setSelectedSlot(obbySlot);
        pendingSwitch = false;
        stage = 0;
    }

    static int findCrystalSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) return i;
        return -1;
    }
}
