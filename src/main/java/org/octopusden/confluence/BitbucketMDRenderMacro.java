package org.octopusden.confluence;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import org.octopusden.confluence.BitbucketFileDownloader.BitbucketFileDownloaderException;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlRenderer.Builder;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

public class BitbucketMDRenderMacro extends BaseMacro implements Macro {
    private static final String IMAGE_PATH_GROUP = "path";
    private static final Pattern replacePattern = Pattern.compile("!\\[.*]\\((?<" + IMAGE_PATH_GROUP + ">.+)\\)");
    private static final String tableFixJs = "<script> AJS.$('.bitbucket-md-render-macro-table thead th').each(function(i, block) {\n" +
            "    block.classList.add(\"confluenceTh\");\n" +
            "});\n" +
            "\n" +
            "AJS.$('.bitbucket-md-render-macro-table tbody td').each(function(i, block) {\n" +
            "    block.classList.add(\"confluenceTd\");\n" +
            "});</script>";

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
        try {
            Objects.requireNonNull(parameters.get("BitbucketURL"), "`BitbucketURL` is not defined.");
            Objects.requireNonNull(parameters.get("ProjectKey"), "`ProjectKey` is not defined.");
            Objects.requireNonNull(parameters.get("Repository"), "`Repository` is not defined.");
            Objects.requireNonNull(parameters.get("Path"), "`Path` is not defined.");
            Objects.requireNonNull(parameters.get("Revision"), "`Revision` is not defined.");
            Objects.requireNonNull(parameters.get("User"), "`User` is not defined.");
            Objects.requireNonNull(parameters.get("Password"), "`Password` is not defined.");
            String path = parameters.get("Path").replace('\\', '/');
            String imageBasicPath = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
            BitbucketFileDownloader bitbucketFileDownloader = new BitbucketFileDownloader(
                    parameters.get("BitbucketURL"),
                    parameters.get("ProjectKey"),
                    parameters.get("Repository"),
                    parameters.get("User"),
                    parameters.get("Password")
            );
            String fileText = new String(bitbucketFileDownloader.get(path, parameters.get("Revision")), StandardCharsets.UTF_8);
            StringBuilder markdownBuilder = new StringBuilder();
            Matcher matcher = replacePattern.matcher(fileText);
            int cursor = 0;
            while (matcher.find(cursor)) {
                markdownBuilder.append(fileText, cursor, matcher.start(IMAGE_PATH_GROUP));
                String imageRelativePath = matcher.group(IMAGE_PATH_GROUP).replace('\\', '/');
                int imageRelativePathLength = imageRelativePath.length();
                if (imageRelativePathLength > 4 && ".png".equalsIgnoreCase(imageRelativePath.substring(imageRelativePathLength - 4))) {
                    String imagePath = imageBasicPath + (imageRelativePath.startsWith("/") ? imageRelativePath : "/" + imageRelativePath);
                    try {
                        String imageData = printBase64Binary(bitbucketFileDownloader.get(imagePath, parameters.get("Revision")));
                        markdownBuilder.append("data:image/png;base64,").append(imageData);
                    } catch (Exception e) {
                        markdownBuilder.append(matcher.group(IMAGE_PATH_GROUP));
                    }
                } else {
                    markdownBuilder.append(matcher.group(IMAGE_PATH_GROUP));
                }
                cursor = matcher.end(IMAGE_PATH_GROUP);
            }
            markdownBuilder.append(fileText.substring(cursor));
            markdown = markdownBuilder.toString();
        } catch (BitbucketFileDownloaderException bfde) {
            markdown = bfde.getMessage();
        } catch (MalformedURLException mue) {
            markdown = "**Invalid URL.**\n" + mue.getMessage();
        } catch (ClassCastException cce) {
            markdown = "**Unsupported protocol. Use HTTPS.**";
        } catch (IndexOutOfBoundsException iaobe) {
            markdown = "**Resource is too large.**";
        } catch (Exception e) {
            markdown = "**Exception**\n" + e.getMessage() + "\n```\n" + getStackTrace(e) + "\n```";
        }
        try {
            MutableDataSet options = new MutableDataSet()
                    .set(HtmlRenderer.INDENT_SIZE, 2)
                    .set(HtmlRenderer.PERCENT_ENCODE_URLS, true)
                    .set(TablesExtension.COLUMN_SPANS, false)
                    .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
                    .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                    .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)
                    .set(TablesExtension.CLASS_NAME, "confluenceTable bitbucket-md-render-macro-table")
                    .set(Parser.EXTENSIONS, Arrays.asList(
                            TablesExtension.create(),
                            TocExtension.create(),
                            CodeBlockExtension.create())
                    );
            Parser parser = Parser.builder(options).build();
            Builder builder = HtmlRenderer.builder(options);
            HtmlRenderer renderer = builder.build();
            return renderer.render(parser.parse(markdown)) + tableFixJs;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    public boolean hasBody() {
        return true;
    }

    @Override
    public RenderMode getBodyRenderMode() {
        return RenderMode.NO_RENDER;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String execute(Map map, String s, RenderContext renderContext) {
        return execute(map, s, new DefaultConversionContext(renderContext));
    }

    private static String getStackTrace(Exception e) {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            return stringWriter.toString();
        } catch (Exception e2) {
            return "getStackTrace exception: " + e2.getMessage();
        }
    }
}
