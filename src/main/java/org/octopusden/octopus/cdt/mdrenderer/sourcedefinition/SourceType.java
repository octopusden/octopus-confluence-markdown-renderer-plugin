package org.octopusden.octopus.cdt.mdrenderer.sourcedefinition;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum SourceType {
    GIT("Git"),
    SUBVERSION("SVN"),
    PLAIN_URL("Plain URL"),
    DEPRECATED("!DEPRECATED!");

    private final String displayName;
    private static final Map<String, SourceType> SOURCE_TYPE_MAP;
    
    static {
        Map<String, SourceType> map = new HashMap();
        for (SourceType sourceType : SourceType.values()) {
            map.put(sourceType.getDisplayName(), sourceType);
        }
        SOURCE_TYPE_MAP = Collections.unmodifiableMap(map);
    }
    
    public static SourceType getFromDisplayName(String displayName) {
        return SOURCE_TYPE_MAP.get(displayName);
    }
    
    public static SourceType valueOfOrDeprecated(String displayName) {
        try {
            return valueOf(displayName);
        } catch (IllegalArgumentException e) {
            return DEPRECATED;
        }
    }
}
