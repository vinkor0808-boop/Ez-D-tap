package snow.dtap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

/**
 * Obby setzen -> Mod wechselt automatisch auf den Crystal-Slot. Den Rest machst du selbst.
 * An/Aus mit Taste J (aenderbar unter Optionen -> Steuerung -> Verschiedenes).
 */
public class DTapClient implements ClientModInitializer {
    /** Pause in Ticks (1 Tick = 50 ms) zwischen Obby setzen und Wechsel auf Crystal.
     *  Nicht im selben Moment wechseln, sonst lehnen manche Server das Obby ab (Ghost Block). */
    public static final int SWITCH_DELAY = 2;
    /** So lange (in Ticks) nach dem Wechsel wird noch geprueft, ob das Obby weggebuggt ist. */
    public static final int WATCH_TICKS = 10;

    static boolean enabled = true;
    static KeyBinding toggleKey;

    static BlockPos obbyPos;
    static int obbySlot = -1, crystalSlot = -1;
    static int wait = 0, watch = 0;
    static boolean pendingSwitch = false;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.snowdtap.toggle", GLFW.GLFW_KEY_J, KeyBinding.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(DTapClient::tick);
    }

    /** Wird vom Mixin aufgerufen, sobald Obsidian platziert wurde. */
    public static void onObbyPlaced(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled || mc.player == null) return;
        int slot = findCrystalSlot(mc);
        if (slot < 0) return;
        obbySlot = mc.player.getInventory().getSelectedSlot();
        crystalSlot = slot;
        obbyPos = pos;
        wait = SWITCH_DELAY;
        watch = 0;
        pendingSwitch = true;
    }

    static void tick(MinecraftClient mc) {
        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            pendingSwitch = false;
            watch = 0;
            if (mc.player != null)
                mc.player.sendMessage(Text.literal("Snow DTap: ")
                        .append(enabled ? Text.literal("AN").formatted(Formatting.GREEN)
                                        : Text.literal("AUS").formatted(Formatting.RED)), true);
        }
        if (mc.player == null || mc.world == null || obbyPos == null) return;

        if (pendingSwitch) {
            if (obbyGone(mc)) { pendingSwitch = false; return; } // Obby schon weg -> du hast noch Obby in der Hand
            if (wait > 0) { wait--; return; }
            mc.player.getInventory().setSelectedSlot(crystalSlot);
            pendingSwitch = false;
            watch = WATCH_TICKS;
            return;
        }

        // Kurz nach dem Wechsel: Obby doch weggebuggt? -> zurueck auf Obby, damit du direkt neu setzen kannst
        if (watch > 0) {
            watch--;
            if (obbyGone(mc)) {
                watch = 0;
                if (obbySlot >= 0 && mc.player.getInventory().getStack(obbySlot).isOf(Items.OBSIDIAN))
                    mc.player.getInventory().setSelectedSlot(obbySlot);
            }
        }
    }

        /** true, solange die Mod gleich auf Crystal wechselt (dann blockt das Mixin weitere Obby-Klicks). */
    public static boolean isSwitchPending() {
        return enabled && pendingSwitch;
    }
static boolean obbyGone(MinecraftClient mc) {
        return !mc.world.getBlockState(obbyPos).isOf(Blocks.OBSIDIAN);
    }

    static int findCrystalSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) return i;
        return -1;
    }
}
