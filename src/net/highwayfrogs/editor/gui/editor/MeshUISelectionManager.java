package net.highwayfrogs.editor.gui.editor;

import net.highwayfrogs.editor.gui.mesh.DynamicMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * This object manages the selection of data of a particular type.
 * Created by Kneesnap on 9/26/2023.
 */
public class MeshUISelectionManager<TMesh extends DynamicMesh, TTarget> {
    private final MeshViewController<TMesh> meshViewController;
    private final List<MeshUISelector<TMesh, TTarget>> selectors = new ArrayList<>();

    public MeshUISelectionManager(MeshViewController<TMesh> meshViewController) {
        if (meshViewController == null)
            throw new NullPointerException("meshViewController");

        this.meshViewController = meshViewController;
    }

    /**
     * Adds and activates a new selector.
     * @param selector The selector to enable.
     */
    public void add(MeshUISelector<TMesh, TTarget> selector) {
        if (selector == null)
            throw new IllegalArgumentException("The provided selector was null.");

        if (this.selectors.contains(selector))
            throw new IllegalStateException("The provided selector was already registered.");

        MeshUISelector<TMesh, ?> lastActiveSelector = this.meshViewController.getSelectors().size() > 0
                ? this.meshViewController.getSelectors().get(this.meshViewController.getSelectors().size() - 1) : null;

        boolean hadPrevSelectorActive = lastActiveSelector != null && lastActiveSelector.isActive();
        if (hadPrevSelectorActive)
            lastActiveSelector.disable();

        try {
            selector.enable(this);

            // Activate the selector before enabling it. This way, if there is an error it never gets added.
            this.selectors.add(selector);
            this.meshViewController.getSelectors().add(selector);
        } catch (Throwable th) {
            th.printStackTrace();

            // Cancel the selector.
            selector.cancel();

            // Re-activate the previous selection if there is one.
            if (hadPrevSelectorActive)
                lastActiveSelector.enableAgain();
        }
    }

    /**
     * This hook runs when the selector is cancelled.
     * @param selector The selector which was cancelled.
     */
    public void onCancel(MeshUISelector<TMesh, TTarget> selector) {
        if (!this.selectors.remove(selector))
            return; // Not registered here, not registered globally.

        int globalIndex = this.meshViewController.getSelectors().lastIndexOf(selector);
        if (globalIndex == -1)
            return; // Not registered globally.

        this.meshViewController.getSelectors().remove(globalIndex);
        boolean wasLastValue = (globalIndex == this.meshViewController.getSelectors().size());

        if (wasLastValue || selector.isActive()) {
            // Re-activate last one.
            MeshUISelector<TMesh, ?> lastActiveSelector = this.meshViewController.getSelectors().size() > 0
                    ? this.meshViewController.getSelectors().get(this.meshViewController.getSelectors().size() - 1) : null;

            if (lastActiveSelector != null)
                lastActiveSelector.enableAgain();
        }
    }
}