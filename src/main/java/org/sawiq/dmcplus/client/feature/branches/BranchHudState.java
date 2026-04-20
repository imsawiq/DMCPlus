package org.sawiq.dmcplus.client.feature.branches;

import net.minecraft.util.math.MathHelper;

public class BranchHudState {

    private float visibility;
    private float displayedProgress;

    public void tick(boolean shown, float targetProgress) {
        this.visibility = MathHelper.lerp(0.18F, this.visibility, shown ? 1.0F : 0.0F);
        this.displayedProgress = MathHelper.lerp(0.16F, this.displayedProgress, shown ? targetProgress : 0.0F);
    }

    public float visibility() {
        return this.visibility;
    }

    public float displayedProgress() {
        return this.displayedProgress;
    }
}
