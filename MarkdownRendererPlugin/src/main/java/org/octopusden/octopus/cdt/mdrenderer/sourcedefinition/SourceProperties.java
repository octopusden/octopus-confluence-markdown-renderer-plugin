package org.octopusden.octopus.cdt.mdrenderer.sourcedefinition;

import lombok.*;
import org.json.JSONObject;

@AllArgsConstructor
@Getter
@Setter
public class SourceProperties {
    private SourceType sourceType;
    private String sourceUrl;
    private String sourceUsername;
    private String sourcePassword;

    public SourceProperties(String classAsJsonString) {
        JSONObject jsonObject = new JSONObject(classAsJsonString);
        this.sourceType = SourceType.valueOfOrDeprecated(jsonObject.getString("sourceType"));
        this.sourceUrl = (jsonObject.has("sourceUrl")) ? jsonObject.getString("sourceUrl") : null;
        this.sourceUsername = (jsonObject.has("sourceUsername")) ? jsonObject.getString("sourceUsername"): null;
        this.sourcePassword = (jsonObject.has("sourcePassword")) ? jsonObject.getString("sourcePassword") : null;
    }
}
