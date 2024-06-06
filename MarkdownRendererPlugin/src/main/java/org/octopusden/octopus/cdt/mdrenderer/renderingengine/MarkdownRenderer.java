package org.octopusden.octopus.cdt.mdrenderer.renderingengine;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.util.Arrays;

public class MarkdownRenderer {
    private static final String TABLE_FIX_JS = "<script> AJS.$('.bitbucket-md-render-macro-table thead th').each(function(i, block) {\n" +
            "    block.classList.add(\"confluenceTh\");\n" +
            "});\n" +
            "\n" +
            "AJS.$('.bitbucket-md-render-macro-table tbody td').each(function(i, block) {\n" +
            "    block.classList.add(\"confluenceTd\");\n" +
            "});</script>";

    public MarkdownRenderer() {
    }

    public String fetchAndRender(IMarkdownFetcher iMarkdownFetcher) {
        String markdown = iMarkdownFetcher.fetch();
         
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
                            CodeBlockExtension.create()));
            Parser parser = Parser.builder(options).build();
            HtmlRenderer.Builder builder = HtmlRenderer.builder(options);
            HtmlRenderer renderer = builder.build();
            markdown = renderer.render(parser.parse(markdown)) + TABLE_FIX_JS;
        } catch (Exception e) {
            markdown = "Exception[" + e.getMessage() + "] during markdown rendering.";
        }
        
        return markdown;
    }
}
