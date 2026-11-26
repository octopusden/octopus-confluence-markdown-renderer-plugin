(function ($) {
    var MarkdownRendererMacro = function () {};
    var selectedSource;
    var jsSourcesMap = new Map();
    const gitSourceType = "GIT";
    const svnSourceType = "SUBVERSION";
    const plainUrlSourceType = "PLAIN_URL";
    const selectNodes = ["sourceName"];
    const inputNodes = ["svnCountry", "projectKey", "mainRepository", "pathToMarkdownFile", "branch", "plainUrl", "svnBranchPath"];
    const i18nKeyDescNameTempl = "MarkdownRendererPlugin.mdrenderer-macro.param.{0}.{1}.desc";
    const i18nKeyLabelNameTempl = "MarkdownRendererPlugin.mdrenderer-macro.param.{0}.{1}.label";
    var sourceType = "";
    
    var sourceTypeDescs = {
        "MarkdownRendererPlugin.mdrenderer-macro.param.projectKey.git.desc" : "Key of the project",
        "MarkdownRendererPlugin.mdrenderer-macro.param.projectKey.subversion.desc" : "Customer code",
        "MarkdownRendererPlugin.mdrenderer-macro.param.mainRepository.git.desc" : "Repository within project/client folder where markdown file is located",
        "MarkdownRendererPlugin.mdrenderer-macro.param.pathToMarkdownFile.git.desc" : "Path from Main Repository to markdown file",
        "MarkdownRendererPlugin.mdrenderer-macro.param.pathToMarkdownFile.subversion.desc" : "Path from Branch folder to markdown file",
        "MarkdownRendererPlugin.mdrenderer-macro.param.branch.git.desc" : "Branch name (Optional, default value: master)"
    };
    
    var sourceTypeLabels = {
        "MarkdownRendererPlugin.mdrenderer-macro.param.projectKey.git.label" : "Project Key",
        "MarkdownRendererPlugin.mdrenderer-macro.param.projectKey.subversion.label" : "Client Code"
    };
    
    MarkdownRendererMacro.prototype.fields = {
        "enum": {
            "sourceName": function (param, options) {
                let paramDiv = AJS.$(Confluence.Templates.MacroBrowser.macroParameterSelect());
                let sourceDropDown = AJS.$("select", paramDiv);
                let prevSourceType = "";
                getSourcesList(sourceDropDown)
                
                sourceDropDown.change(function () {
                    const selectedSource = $("select#macro-param-sourceName option:selected").text();
                    sourceType = jsSourcesMap.get(selectedSource)
                    if (prevSourceType !== sourceType) {
                        prevSourceType = sourceType;
                        showIfNeeded(selectNodes, "select");
                        showIfNeeded(inputNodes, "input");
                        
                        let nodesToHideInput = [];
                        let nodesDescToChange = ["projectKey", "mainRepository", "pathToMarkdownFile", "branch"];
                        let nodesLabelsToChange = ["projectKey"];
                        if (gitSourceType === sourceType) {
                            nodesToHideInput = ["svnCountry", "plainUrl", "svnBranchPath"];
                            setDescriptions(nodesDescToChange, "git");
                            setLabels(nodesLabelsToChange, "git");
                        } else if (svnSourceType === sourceType) {
                            nodesToHideInput = ["plainUrl", "branch", "mainRepository"];
                            setDescriptions(nodesDescToChange, "subversion");
                            setLabels(nodesLabelsToChange, "subversion");
                        } else if (plainUrlSourceType === sourceType) {
                            nodesToHideInput = ["svnCountry", "projectKey", "mainRepository", "pathToMarkdownFile", "branch", "svnBranchPath"];
                        } else { // sourceType is undefined --> hide everything 
                            nodesToHideInput = inputNodes;
                        }
                        hideIfNeeded(nodesToHideInput, "input")
                    }
                });
                
                return new AJS.MacroBrowser.Field(paramDiv, sourceDropDown, options);
            }
        }
    };
    
    MarkdownRendererMacro.prototype.beforeParamsSet = function (selectedParams, macroSelected) {
        selectedSource = selectedParams.sourceName;
        
        return selectedParams;
    };
    
    function getSourcesList(sourceDropDown) {
        let gitSources = [];
        let svnSources = [];
        let plainUrlSources = [];
        let selectedSourcePresent = false;
        sourceDropDown.empty();
        
        AJS.$.ajax({
            async: true,
            url: AJS.contextPath() + "/rest/mdrenderer_rest/1.0/mdrenderer/sourcesList",
            dataType: 'json',
            timeout: 10000,
            error: function (xhr, textStatus, errorThrown) {
                AJS.logError(errorThrown);
                console.log(`There was an error during dynamic sources list building: ${errorThrown}`);
            },
            success: function (response) {
                gitSources = [];
                svnSources = [];
                plainUrlSources = [];
                
                for (let key in response) {
                    jsSourcesMap.set(key, response[key]);
                    
                    if (gitSourceType === response[key]) {
                        gitSources.push(key);
                    } else if (svnSourceType === response[key]) {
                        svnSources.push(key);
                    } else if (plainUrlSourceType === response[key]) {
                        plainUrlSources.push(key);
                    }
                }
                
                selectedSourcePresent = fillDropdown(sourceDropDown, gitSources, selectedSource, "Git source(s)");
                selectedSourcePresent = fillDropdown(sourceDropDown, svnSources, selectedSource, "Subversion source(s)") || selectedSourcePresent;
                selectedSourcePresent = fillDropdown(sourceDropDown, plainUrlSources, selectedSource, "Plain URL source(s)") || selectedSourcePresent;
                
                if (!selectedSource || !selectedSourcePresent) {
                    sourceDropDown.prepend($("<option selected disabled hidden></option>").val('').html("-- Select Source --")); // Empty val to prevent saving in macro config screen
                    selectedSource = '';
                } 
                $("select#macro-param-sourceName").val(selectedSource).change();
            }
        });
    }
    
    function showIfNeeded(nodesNames, nodeType) {
        for (let node in nodesNames) {
            let fullDivName = `div#macro-param-div-${nodesNames[node]}`;
            let fullParmName = `${nodeType}#macro-param-${nodesNames[node]}`;
            if ($(fullDivName).is(":hidden")) {
                if ($(fullParmName).val() === " ") {
                    $(fullParmName).val("");
                }
                $(fullDivName).show();
            }
        }
    }
    
    function hideIfNeeded(nodesNames, nodeType) {
        for (let node in nodesNames) {
            let fullDivName = `div#macro-param-div-${nodesNames[node]}`;
            let fullParmName = `${nodeType}#macro-param-${nodesNames[node]}`;
            if ($(fullDivName).is(":visible")) {
                if ($(fullParmName).val().trim() === "") {
                    $(fullParmName).val(" ");
                }
                $(fullDivName).hide();
            }
        }
    }
    
    function setDescriptions(nodesNames, sourceType) {
        for (let node in nodesNames) {
            let descText = sourceTypeDescs[(AJS.format(i18nKeyDescNameTempl, nodesNames[node], sourceType.toLowerCase()))];
            $(`div#macro-param-div-${nodesNames[node]}`).find(".macro-param-desc").text(descText);
        }
    }
    
    function setLabels(nodesNames, sourceType) {
        for (let node in nodesNames) {
            let labelText = sourceTypeLabels[(AJS.format(i18nKeyLabelNameTempl, nodesNames[node], sourceType.toLowerCase()))];
            let className = $(`div#macro-param-div-${nodesNames[node]}`).attr("class");
            if (className.endsWith(" required")) {
               labelText += " *";
            }
            $(`div#macro-param-div-${nodesNames[node]}`).find("label").text(labelText);
        }
    }
        
    function fillDropdown(sourceDropDown, sourcesArray, selectedSource, labelText) {
        let selectedSourcePresent = false;
        if (sourcesArray.length !== 0) {
            let appended = sourceDropDown.append(`<optgroup label="${labelText}">`);
            for (let i in sourcesArray) {
                if (selectedSource === sourcesArray[i]) {
                    selectedSourcePresent = true;
                }
                appended.append($("<option></option>").val(sourcesArray[i]).html(sourcesArray[i]));
            }
            sourceDropDown.append("</optgroup>");
        }
        return selectedSourcePresent;
    }
    
    AJS.MacroBrowser.setMacroJsOverride("mdrenderer-macro", new MarkdownRendererMacro());
})(AJS.$);

