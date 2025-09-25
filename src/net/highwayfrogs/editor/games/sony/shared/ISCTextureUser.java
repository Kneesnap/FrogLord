package net.highwayfrogs.editor.games.sony.shared;

import net.highwayfrogs.editor.games.sony.shared.utils.SCAnalysisUtils.SCTextureUsage;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a file which uses textures.
 * Created by Kneesnap on 8/14/2025.
 */
public interface ISCTextureUser {
    /**
     * Gets a list of all texture IDs used by this object.
     */
    List<Short> getUsedTextureIds();

    /**
     * Gets the name of this texture user, for the purposes of display to the user/identification.
     * @return textureUserName
     */
    String getTextureUserName();

    /**
     * Gets a set of texture usages used by this user.
     */
    default Set<SCTextureUsage> getTextureUsages() {
        List<Short> usedTextureIds = getUsedTextureIds();
        if (usedTextureIds == null || usedTextureIds.isEmpty())
            return null;

        Set<SCTextureUsage> results = new HashSet<>();
        addTextureIdUsages(results, usedTextureIds, getTextureUserName());
        return results;
    }

    /**
     * Gets a list of texture IDs from the provided set of texture usages
     * @param textureUsages the texture usage collection to add usages to
     * @param textureIds the texture IDs to add usages of
     * @param locationDescription the location to display to the user if they want to know where the textures are from
     */
    default void addTextureIdUsages(Set<SCTextureUsage> textureUsages, List<Short> textureIds, String locationDescription) {
        if (textureIds == null || textureIds.isEmpty())
            return;

        for (int i = 0; i < textureIds.size(); i++) {
            Short usedTextureId = textureIds.get(i);
            if (usedTextureId == null || usedTextureId < 0)
                continue;

            textureUsages.add(new SCTextureUsage(this, usedTextureId, locationDescription));
        }
    }

    /**
     * Gets a list of texture IDs from the provided set of texture usages
     * @param usages the texture usages to get IDs from
     * @return textureIds
     */
    static List<Short> getTextureIdsFromUsages(Set<SCTextureUsage> usages) {
        if (usages == null || usages.isEmpty())
            return null;

        IndexBitArray seenTextureIds = new IndexBitArray();
        List<Short> textureIds = new ArrayList<>();
        for (SCTextureUsage textureUsage : usages)
            if (textureUsage != null && textureUsage.getTextureId() >= 0 && seenTextureIds.setBit(textureUsage.getTextureId(), true))
                textureIds.add(textureUsage.getTextureId());

        return textureIds;
    }
}
