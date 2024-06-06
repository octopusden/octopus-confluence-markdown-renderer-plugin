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
    private static final String SOURCE_URL_PARM = "sourceUrl";
    private static final String SOURCE_URL_LABEL = "Source URL";
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
    private static final String PARM_NOT_DEFINED_ERR_MSG = "Parameter [$parmNameTempl] is not defined.";

    private final MarkdownRendererSettings markdownRendererSettings;

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
        String markdown;
        String sourceUrl = Objects.requireNonNull(parameters.get(SOURCE_URL_PARM), PARM_NOT_DEFINED_ERR_MSG.replaceAll("\\$parmNameTempl", SOURCE_URL_LABEL));
        String svnCountry = Objects.requireNonNull(parameters.get(SVN_COUNTRY_PARM), PARM_NOT_DEFINED_ERR_MSG.replaceAll("\\$parmNameTempl", SVN_COUNTRY_LABEL));
        String projectKey = Objects.requireNonNull(parameters.get(PROJECT_KEY_PARM), PARM_NOT_DEFINED_ERR_MSG.replaceAll("\\$parmNameTempl", PROJECT_KEY_LABEL));
        String mainRepository = Objects.requireNonNull(parameters.get(MAIN_REPOSITORY_PARM), PARM_NOT_DEFINED_ERR_MSG.replaceAll("\\$parmNameTempl", MAIN_REPOSITORY_LABEL));
        String pathMdFile = Objects.requireNonNull(parameters.get(PATH_MD_FILE_PARM), PARM_NOT_DEFINED_ERR_MSG.replaceAll("\\$parmNameTempl", PATH_MD_FILE_LABEL));
        String plainUrl = Objects.requireNonNull(parameters.get(PLAIN_URL_PARM), PARM_NOT_DEFINED_ERR_MSG.replaceAll("\\$parmNameTempl", PLAIN_URL_LABEL));
        SourceProperties sourceProperties = markdownRendererSettings.getDeserializedSourcesMap().get(sourceUrl);
        pathMdFile = pathMdFile.replace('\\', '/');
        
        if (sourceProperties == null) {
            markdown = "Source with name[" + sourceUrl +"] no longer available. Contact Space Admin and/or pick another available source.";
        } else {
            try {
                String branch;
                MarkdownRenderer markdownRenderer = new MarkdownRenderer();
                switch (sourceProperties.getSourceType()) {
                    case GIT:
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
                        PlainUrlMarkdownFetcher plainUrlMarkdownFetcher = new PlainUrlMarkdownFetcher(plainUrl);
                        markdown = markdownRenderer.fetchAndRender(plainUrlMarkdownFetcher);
                        break;
                    default:
                        markdown = "Unsupported Source Type defined[" + sourceProperties.getSourceType() + "] for used Source URL[" + sourceUrl + "]";
                }
            } catch (Exception e) {
                markdown = "Exception[" + e.getMessage() + "] during " + sourceProperties.getSourceType().getDisplayName() + " source rendering.";
            }
        }

        return markdown;
    }
}
