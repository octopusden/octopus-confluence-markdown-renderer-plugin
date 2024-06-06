package org.octopusden.octopus.cdt.mdrenderer.parmspersistence;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.gson.Gson;
import org.octopusden.octopus.cdt.mdrenderer.sourcedefinition.SourceProperties;
import org.octopusden.octopus.cdt.mdrenderer.sourcedefinition.SourceType;

import java.util.Map;
import java.util.TreeMap;

public class MarkdownRendererSettings {
    private final PluginSettingsFactory pluginSettingsFactory;
    private final static String APP_KEY = "markdownRendererKey";
    private final static String SOURCES_MAP_PARM_NAME = "sourcesMap";
    private final static String ALLOW_PLAIN_URL_PARM_NAME = "allowPlainUrl";

    public MarkdownRendererSettings(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    public void setPluginParm(String key, Object value) {
        final PluginSettings settings = pluginSettingsFactory.createSettingsForKey(APP_KEY);
        settings.put(key, value);
    }

    public Object getPluginParm(String key) {
        final PluginSettings settings = pluginSettingsFactory.createSettingsForKey(APP_KEY);
        return settings.get(key);
    }
    
    public void setAllowPlainUrlParm(Boolean allowPlainUrl) {
        setPluginParm(ALLOW_PLAIN_URL_PARM_NAME, allowPlainUrl.toString());
    }
    
    public boolean getAllowPlainUrlParm() {
        if (getPluginParm(ALLOW_PLAIN_URL_PARM_NAME) != null) {
            return Boolean.parseBoolean((String) getPluginParm(ALLOW_PLAIN_URL_PARM_NAME));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getSerializedSourcesMap() {
        return (Map<String, String>) getPluginParm(SOURCES_MAP_PARM_NAME);
    }

    @SuppressWarnings("unchecked")
    public Map<String, SourceProperties> getDeserializedSourcesMap() {
        return deserializeMap((Map<String, String>) getPluginParm(SOURCES_MAP_PARM_NAME));
    }

    public void serializeAndSetMap(Map<String, SourceProperties> deserializedMap) {
        setPluginParm(SOURCES_MAP_PARM_NAME, serializeMap(deserializedMap));
    }

    public Map<String, SourceProperties> deserializeMap(Map<String, String> serializedMap) {
        Map<String, SourceProperties> deserializedMap = new TreeMap<>();
        
        if (serializedMap != null) {
            serializedMap.forEach((key, value) -> deserializedMap.put(key, new SourceProperties(value)));
        }
        
        if (getAllowPlainUrlParm()) {
            deserializedMap.put(SourceType.PLAIN_URL.getDisplayName(), new SourceProperties(SourceType.PLAIN_URL, null, null, null));
        } else {
            deserializedMap.remove(SourceType.PLAIN_URL.getDisplayName());
        }

        return deserializedMap;
    }

    public Map<String, String> serializeMap(Map<String, SourceProperties> map) {
        Map<String, String> serializedMap = new TreeMap<>();
        if (map != null) {
            map.forEach((key, value) -> serializedMap.put(key, new Gson().toJson(value)));
        }

        return serializedMap;
    }
}
