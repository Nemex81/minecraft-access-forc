package org.mcaccess.minecraftaccess.features.academy;

import java.util.Collections;
import java.util.List;

/**
 * Definition of an Academy training mission.
 */
public record Mission(
        String id,
        String titleKey,
        String descriptionKey,
        boolean creativeOnly,
        List<MissionStep> steps
) {
    public List<MissionStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }
}
