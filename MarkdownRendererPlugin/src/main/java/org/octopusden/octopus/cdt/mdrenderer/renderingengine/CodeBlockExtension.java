package org.octopusden.octopus.cdt.mdrenderer.renderingengine;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.misc.Extension;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CodeBlockExtension implements HtmlRenderer.HtmlRendererExtension {
    static Extension create() {
        return new CodeBlockExtension();
    }

    @Override
    public void rendererOptions(MutableDataHolder mutableDataHolder) {

    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
        rendererBuilder.nodeRendererFactory(new Factory());
    }

    public static class Factory implements NodeRendererFactory {
        @Override
        public NodeRenderer apply(DataHolder options) {
            return new ConfluenceCodeBlockNodeRenderer();
        }
    }

    public static class ConfluenceCodeBlockNodeRenderer implements NodeRenderer {
        static final String CONFLUENCE_CODE_BLOCK_HTML_OPEN_TEMPLATE = "<div class=\"code panel pdl conf-macro output-block\" data-hasbody=\"true\" data-macro-name=\"code\" style=\"border-width: 1px;\">" +
                "<div class=\"codeContent panelContent pdl\">" +
                "<pre class=\"syntaxhighlighter-pre\" data-syntaxhighlighter-params=\"brush: %s; gutter: false; theme: Confluence\" data-theme=\"Confluence\">";
        static final String CONFLUENCE_CODE_BLOCK_HTML_CLOSE = "</pre></div></div>";

        @Override
        public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
            HashSet<NodeRenderingHandler<?>> handlers = new HashSet<NodeRenderingHandler<?>>();
            NodeRenderingHandler<FencedCodeBlock> fencedCodeBlockHandler = new NodeRenderingHandler<FencedCodeBlock>(FencedCodeBlock.class, new NodeRenderingHandler.CustomNodeRenderer<FencedCodeBlock>() {
                @Override
                public void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter htmlWriter) {
                    ConfluenceCodeBlockNodeRenderer.this.renderFencedCodeBlock(node, htmlWriter);
                }
            });
            NodeRenderingHandler<IndentedCodeBlock> indentedCodeBlockHandler = new NodeRenderingHandler<IndentedCodeBlock>(IndentedCodeBlock.class, new NodeRenderingHandler.CustomNodeRenderer<IndentedCodeBlock>() {
                @Override
                public void render(IndentedCodeBlock node, NodeRendererContext context, HtmlWriter htmlWriter) {
                    ConfluenceCodeBlockNodeRenderer.this.renderIndentedCodeBlock(node, htmlWriter);
                }
            });
            handlers.add(fencedCodeBlockHandler);
            handlers.add(indentedCodeBlockHandler);
            return handlers;
        }

        private void renderFencedCodeBlock(FencedCodeBlock fencedCodeBlock, HtmlWriter htmlWriter) {
            String language = fencedCodeBlock.getInfo().toString();
            String[] supportedLanguages = new String[]{
                    "actionsscript3",
                    "applescript",
                    "bash",
                    "c#",
                    "cpp",
                    "css",
                    "coldfusion",
                    "delphi",
                    "diff",
                    "erl",
                    "groovy",
                    "xml",
                    "java",
                    "jfx",
                    "js",
                    "php",
                    "perl",
                    "text",
                    "powershell",
                    "py",
                    "ruby",
                    "sql",
                    "sass",
                    "scala",
                    "vb"
            };
            if (language.isEmpty() || !Arrays.asList(supportedLanguages).contains(language)) {
                // confluence defaults to java
                language = "java";
            }
            String code = fencedCodeBlock.getChildChars().toString();
            write(code, language, htmlWriter, fencedCodeBlock);
        }

        private void renderIndentedCodeBlock(IndentedCodeBlock indentedCodeBlock, HtmlWriter htmlWriter) {
            StringBuilder builder = new StringBuilder();
            for (BasedSequence line : indentedCodeBlock.getContentLines()) {
                builder.append(line.toString());
            }
            String code = builder.toString();
            // Confluence defaults to java
            write(code, "java", htmlWriter, indentedCodeBlock);
        }

        private void write(String code, String language, HtmlWriter htmlWriter, Block processedBlock) {
            code = removeLeadingQuoteInQuotedCode(processedBlock, code);
            String htmlOpen = String.format(CONFLUENCE_CODE_BLOCK_HTML_OPEN_TEMPLATE, language);
            htmlWriter.raw(htmlOpen);
            htmlWriter.openPre();
            htmlWriter.raw("\n");
            htmlWriter.raw(StringEscapeUtils.escapeHtml4(code)); // Escaping to avoid conflict with XML tags
            htmlWriter.closePre();
            htmlWriter.raw(CONFLUENCE_CODE_BLOCK_HTML_CLOSE);
        }

        private String removeLeadingQuoteInQuotedCode(Block processedBlock, String code) {
            if (processedBlock.getParent() != null && FlexmarkHtmlConverter.BLOCKQUOTE_NODE.equalsIgnoreCase(processedBlock.getParent().getNodeName())) {
                code = code.replaceAll("\n\\s*>", "\n");
            }
            return code;
        }
    }
}
