package ae2desktopping.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    CRAFT_COMPLETE_HOOK(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.AE2)
        .addClientMixins("MixinPacketCraftingCompleteNotification")),
    DISCORD_TOGGLE_BUTTON(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.AE2)
        .addClientMixins("MixinGuiCraftConfirm"));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
