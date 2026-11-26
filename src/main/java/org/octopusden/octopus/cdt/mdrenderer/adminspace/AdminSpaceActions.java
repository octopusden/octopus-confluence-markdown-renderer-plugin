package org.octopusden.octopus.cdt.mdrenderer.adminspace;

import com.atlassian.confluence.spaces.actions.SpaceAdminAction;
import org.octopusden.octopus.cdt.mdrenderer.parmspersistence.MarkdownRendererSettings;
import org.octopusden.octopus.cdt.mdrenderer.sourcedefinition.SourceProperties;
import org.octopusden.octopus.cdt.mdrenderer.sourcedefinition.SourceType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Getter
@Setter
public class AdminSpaceActions extends SpaceAdminAction {
    private String errMessage;
    private String confirm;
    private String inputName;
    private String inputPwd;
    private String plainUrlSourceName = SourceType.PLAIN_URL.getDisplayName();
    private Map<String, SourceProperties> savedSources;
    private SourceType[] configurableSourcesTypes = {SourceType.GIT, SourceType.SUBVERSION};
    private String testSave;
    private MarkdownRendererSettings markdownRendererSettings;
    private boolean allowPlainUrl;
    private String editedSrcName;
    private SourceProperties editedSrcProperties;
    

    @Inject
    public AdminSpaceActions(MarkdownRendererSettings markdownRendererSettings) {
        this.markdownRendererSettings = markdownRendererSettings;
    }

    @Override
    public String doDefault() {
        savedSources = markdownRendererSettings.getDeserializedSourcesMap();
        allowPlainUrl = markdownRendererSettings.getAllowPlainUrlParm();
        HttpServletRequest request = ServletActionContext.getRequest();

        if (request != null && request.getParameter("rqAction") != null) {
            String rqAction = request.getParameter("rqAction");
            if ("delete".equals(rqAction) || "edit".equals(rqAction)) {
                String pickedKey = request.getParameter("pickedKey");
                if (!StringUtils.isEmpty(pickedKey.trim())) {
                    SourceProperties deletedSrc = savedSources.remove(pickedKey);
                    markdownRendererSettings.serializeAndSetMap(savedSources);
                    if ("edit".equals(rqAction)) {
                        editedSrcName = pickedKey;
                        editedSrcProperties = deletedSrc;
                    }
                }
            } else if ("add".equals(rqAction)) {
                String newSourceName = request.getParameter("newSourceName");
                String newSourceType = request.getParameter("newSourceType");
                String newSourceUrl = request.getParameter("newSourceUrl");
                String newSourceUser = request.getParameter("newSourceUser");
                String newSourcePwd = request.getParameter("newSourcePwd");

                if (StringUtils.isEmpty(newSourceName.trim()) || StringUtils.isEmpty(newSourceType.trim()) || StringUtils.isEmpty(newSourceUrl.trim()) || StringUtils.isEmpty(newSourceUser.trim()) || StringUtils.isEmpty(newSourcePwd.trim())) {
                    errMessage = "Cannot save new repository, please fill all fields and retry.";
                } else {
                    if (!savedSources.containsKey(newSourceName)) {
                        SourceProperties newSourceProps = new SourceProperties(SourceType.getFromDisplayName(newSourceType), newSourceUrl, newSourceUser, newSourcePwd);
                        savedSources.put(newSourceName, newSourceProps);
                        markdownRendererSettings.serializeAndSetMap(savedSources);
                    } else {
                        errMessage = "Source with that name already exists, please delete existing one or retry with different name.";
                    }
                }
            } else if ("savePlainUrlChoice".equals(rqAction)) {
                allowPlainUrl = Boolean.parseBoolean(request.getParameter("isChecked"));
                markdownRendererSettings.setAllowPlainUrlParm(allowPlainUrl);
                savedSources = markdownRendererSettings.getDeserializedSourcesMap(); // Refresh sources list to include Plain Url
            } else {
                errMessage = "Technical error rqAction[" + rqAction + "], please retry.";
            }
        }
        return INPUT;
    }

}
