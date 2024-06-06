package org.octopusden.octopus.cdt.mdrenderer.rest;

import com.atlassian.plugins.rest.common.security.UnrestrictedAccess;
import org.octopusden.octopus.cdt.mdrenderer.parmspersistence.MarkdownRendererSettings;
import org.octopusden.octopus.cdt.mdrenderer.sourcedefinition.SourceProperties;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.TreeMap;

@Path("/mdrenderer")
public class MarkdownRendererRestResource {
    private final MarkdownRendererSettings markdownRendererSettings;

    @Inject
    public MarkdownRendererRestResource(MarkdownRendererSettings markdownRendererSettings) {
        this.markdownRendererSettings = markdownRendererSettings;
    }

    @GET
    @Path("sourcesList")
    @UnrestrictedAccess
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getSourcesList() {
        if (markdownRendererSettings.getSerializedSourcesMap() == null) {
            return Response.ok(null).build();
        }
        Map<String, String> restSources = new TreeMap<>();
        for (Map.Entry<String, SourceProperties> entry : markdownRendererSettings.getDeserializedSourcesMap().entrySet()) {
            restSources.put(entry.getKey(), entry.getValue().getSourceType().toString());
        }
        return Response.ok(restSources).build();
    }
}
