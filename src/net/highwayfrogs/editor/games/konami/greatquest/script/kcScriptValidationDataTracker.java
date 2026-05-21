package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks kcScriptValidationData entries per entity.
 * Created by Kneesnap on 5/21/2026.
 */
@RequiredArgsConstructor
public class kcScriptValidationDataTracker {
    private final ILogger logger;
    private final Map<kcCResourceEntityInst, kcScriptValidationData> entityMap = new HashMap<>();

    /**
     * Gets or creates validation data for the given entity.
     * @param entity the entity to obtain validation data for
     * @return validationData
     */
    public kcScriptValidationData getOrCreateValidationData(kcCResourceEntityInst entity) {
        if (entity == null)
            return null;

        kcScriptValidationData validationData = this.entityMap.get(entity);
        if (validationData == null) {
            ILogger tempLogger = new AppendInfoLoggerWrapper(logger, entity.getName(), AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
            this.entityMap.put(entity, validationData = new kcScriptValidationData(this, entity, tempLogger));
        }

        return validationData;

    }
}
