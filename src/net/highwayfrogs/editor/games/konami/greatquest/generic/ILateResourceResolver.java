package net.highwayfrogs.editor.games.konami.greatquest.generic;

import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents an object which may depend on referenced resources, but those resources must be available at the time of resolution.
 * This allows a uniform way of resolving resources at a later time.
 * Created by Kneesnap on 11/6/2025.
 */
public interface ILateResourceResolver {
    /**
     * Resolves any pending warnings related to referenced resources.
     * @param logger the logger to print any warnings to
     */
    void resolvePendingResources(ILogger logger);
}