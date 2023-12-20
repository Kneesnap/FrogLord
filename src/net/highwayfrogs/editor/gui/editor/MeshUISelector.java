package net.highwayfrogs.editor.gui.editor;

import lombok.Getter;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.Utils;

/**
 * A basic mesh UI selector, for selecting something.
 * Created by Kneesnap on 9/26/2023.
 */
@Getter
public abstract class MeshUISelector<TMesh extends DynamicMesh, TTarget> {
    private MeshUISelectionManager<TMesh, TTarget> manager;
    private boolean active;

    /**
     * Enables the selector display and ability to be interacted with.
     */
    public final void enable(MeshUISelectionManager<TMesh, TTarget> manager) {
        if (this.active)
            throw new IllegalStateException("The selector was already enabled.");

        this.manager = manager;
        this.active = true;

        try {
            onEnable();
        } catch (Throwable th) {
            throw new RuntimeException("There was an error running the onEnable() hook for " + Utils.getSimpleName(this), th);
        }
    }

    /**
     * Re-enables the selector.
     */
    public void enableAgain() {
        if (this.manager == null)
            throw new RuntimeException("The " + Utils.getSimpleName(this) + " was not enabled previously.");

        enable(this.manager);
    }

    /**
     * Disables the selector from its active state.
     */
    public void disable() {
        if (!this.active)
            throw new IllegalStateException("Cannot disable " + Utils.getSimpleName(this) + " which wasn't enabled.");

        try {
            onDisable();
        } catch (Throwable th) {
            System.out.println(Utils.getSimpleName(this) + " had an error in the onDisable() hook.");
            th.printStackTrace();
            // Nothing else we can do really.
        }

        this.active = false;
    }

    /**
     * Handles the selection of a value.
     * @param selection The selected value to handle.
     */
    public final void select(TTarget selection) {
        try {
            onSelect(selection);
        } catch (Throwable th) {
            System.out.println(Utils.getSimpleName(this) + " had an error in the onSelect() hook.");
            th.printStackTrace();
            // We want to disable, so don't do much.
        }

        // Disable if active.
        if (this.active)
            disable();
    }

    public final void cancel() {
        try {
            onCancel();
        } catch (Throwable th) {
            System.out.println(Utils.getSimpleName(this) + " had an error in the onCancel() hook.");
            th.printStackTrace();
            // We want to disable, so don't do much.
        }

        // Unregister from manager.
        try {
            this.manager.onCancel(this);
        } catch (Throwable th) {
            System.out.println(Utils.getSimpleName(this) + " had an error in the manager's cancellation handler.");
            th.printStackTrace();
            // We want to disable, so don't do much.
        }

        // Disable if active.
        // This must occur after the manager's cancellation handler is run because it wants to know if this was enabled when it got disabled.
        if (this.active)
            disable();
    }

    /**
     * Called when the selection becomes active.
     */
    protected abstract void onEnable();

    /**
     * Called when the selection is disabled.
     */
    protected abstract void onDisable();

    /**
     * Called when a selection occurs.
     * @param selection The selection occurring.
     */
    protected abstract void onSelect(TTarget selection);

    /**
     * Called when the selection is cancelled.
     */
    protected abstract void onCancel();
}