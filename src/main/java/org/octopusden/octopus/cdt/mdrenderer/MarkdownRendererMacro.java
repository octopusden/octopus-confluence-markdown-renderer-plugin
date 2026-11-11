package org.octopusden.octopus.cdt.mdrenderer;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import org.octopusden.octopus.cdt.mdrenderer.git.GitMarkdownFetcher;
import org.octopusden.octopus.cdt.mdrenderer.parmspersistence.MarkdownRendererSettings;
import org.octopusden.octopus.cdt.mdrenderer.plainurl.PlainUrlMarkdownFetcher;
import org.octopusden.octopus.cdt.mdrenderer.renderingengine.MarkdownRenderer;
import org.octopusden.octopus.cdt.mdrenderer.sourcedefinition.SourceProperties;
import org.octopusden.octopus.cdt.mdrenderer.subversion.SubversionMarkdownFetcher;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;

public class MarkdownRendererMacro implements Macro {
    private static final String SOURCE_NAME_PARM = "sourceName";
    private static final String SOURCE_NAME_LABEL = "Source Name";
    private static final String SVN_COUNTRY_PARM = "svnCountry";
    private static final String SVN_COUNTRY_LABEL = "Client Country";
    private static final String PROJECT_KEY_PARM = "projectKey";
    private static final String PROJECT_KEY_LABEL = "Project Key/Client Code";
    private static final String MAIN_REPOSITORY_PARM = "mainRepository";
    private static final String MAIN_REPOSITORY_LABEL = "Main Repository";
    private static final String PATH_MD_FILE_PARM = "pathToMarkdownFile";
    private static final String PATH_MD_FILE_LABEL = "Path to Markdown File";
    private static final String BRANCH_PARM = "branch";
    private static final String SVN_BRANCH_PATH_PARM = "svnBranchPath";
    private static final String PLAIN_URL_PARM = "plainUrl";
    private static final String PLAIN_URL_LABEL = "Plain URL";

    private final MarkdownRendererSettings markdownRendererSettings;
    private Map<String, String> pluginExecParms;

    @Inject
    public MarkdownRendererMacro(MarkdownRendererSettings markdownRendererSettings) {
        this.markdownRendererSettings = markdownRendererSettings;
    }

    @Override
    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

    @Override
    public String execute(Map<String, String> parameters, String bodyContent, ConversionContext conversionContext) {
        pluginExecParms = parameters;
        String markdown;
        String sourceName = getParmValueOrDisplayErrorMsgIfNull(SOURCE_NAME_PARM, SOURCE_NAME_LABEL);
        SourceProperties sourceProperties = markdownRendererSettings.getDeserializedSourcesMap().get(sourceName);

        if (sourceProperties == null) {
            markdown = "Source with name[" + sourceName + "] no longer available. Contact Space Admin and/or pick another available source.";
        } else {
            try {
                String branch, projectKey, pathMdFile;
                MarkdownRenderer markdownRenderer = new MarkdownRenderer();
                switch (sourceProperties.getSourceType()) {
                    case GIT:
                        String mainRepository = getParmValueOrDisplayErrorMsgIfNull(MAIN_REPOSITORY_PARM, MAIN_REPOSITORY_LABEL);
                        projectKey = getParmValueOrDisplayErrorMsgIfNull(PROJECT_KEY_PARM, PROJECT_KEY_LABEL);
                        pathMdFile = getParmValueOrDisplayErrorMsgIfNull(PATH_MD_FILE_PARM, PATH_MD_FILE_LABEL).replace('\\', '/');
                        branch = parameters.getOrDefault(BRANCH_PARM, "master");

                        GitMarkdownFetcher gitMarkdownDownloader =
                                new GitMarkdownFetcher(sourceProperties,
                                        projectKey,
                                        mainRepository,
                                        pathMdFile,
                                        branch);
                        markdown = markdownRenderer.fetchAndRender(gitMarkdownDownloader);
                        break;
                    case SUBVERSION:
                        String svnCountry = getParmValueOrDisplayErrorMsgIfNull(SVN_COUNTRY_PARM, SVN_COUNTRY_LABEL);
                        projectKey = getParmValueOrDisplayErrorMsgIfNull(PROJECT_KEY_PARM, PROJECT_KEY_LABEL);
                        pathMdFile = getParmValueOrDisplayErrorMsgIfNull(PATH_MD_FILE_PARM, PATH_MD_FILE_LABEL).replace('\\', '/');
                        branch = parameters.getOrDefault(SVN_BRANCH_PATH_PARM, "branches/int");

                        SubversionMarkdownFetcher subversionMarkdownFetcher =
                                new SubversionMarkdownFetcher(sourceProperties,
                                        svnCountry,
                                        projectKey,
                                        branch,
                                        pathMdFile);
                        markdown = markdownRenderer.fetchAndRender(subversionMarkdownFetcher);
                        break;
                    case PLAIN_URL:
                        String plainUrl = getParmValueOrDisplayErrorMsgIfNull(PLAIN_URL_PARM, PLAIN_URL_LABEL);

                        PlainUrlMarkdownFetcher plainUrlMarkdownFetcher = new PlainUrlMarkdownFetcher(plainUrl);
                        markdown = markdownRenderer.fetchAndRender(plainUrlMarkdownFetcher);
                        break;
                    default:
                        markdown = "Unsupported Source Type defined[" + sourceProperties.getSourceType() + "] for used Source URL[" + sourceName + "]";
                }
            } catch (Exception e) {
                markdown = "Exception[" + e.getMessage() + "] during " + sourceProperties.getSourceType().getDisplayName() + " source rendering.";
            }
        }

        return markdown;
    }

    String getParmValueOrDisplayErrorMsgIfNull(String parmKey, String parmName) {
        return Objects.requireNonNull(pluginExecParms.get(parmKey), "Parameter [" + parmName + "] is not defined.");
    }
}
