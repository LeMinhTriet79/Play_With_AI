package com.minhtriet.util;

public final class MarkdownParser {
    private MarkdownParser() {}

    public static String convertMarkdownToHtml(String md) {
        if (md == null || md.isBlank()) return "";

        StringBuilder html = new StringBuilder();
        boolean inCodeBlock = false;
        boolean inTable = false;
        boolean isFirstRow = false;
        boolean inUl = false;
        boolean inOl = false;
        String[] lines = md.split("\n");

        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                if (inTable) { html.append("</table><br>\n"); inTable = false; }
                if (inUl) { html.append("</ul>\n"); inUl = false; }
                if (inOl) { html.append("</ol>\n"); inOl = false; }
                if (inCodeBlock) {
                    html.append("</pre>");
                    inCodeBlock = false;
                } else {
                    html.append("<pre style='background-color: #FFFFFF; border-top: 2px solid #808080; border-left: 2px solid #808080; border-right: 2px solid #FFFFFF; border-bottom: 2px solid #FFFFFF; padding: 8px; font-family: \"Courier New\", monospace; margin: 5px 0; color: #000000;'>");
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                html.append(line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")).append("\n");
                continue;
            }

            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("|") && trimmedLine.endsWith("|")) {
                if (inUl) { html.append("</ul>\n"); inUl = false; }
                if (inOl) { html.append("</ol>\n"); inOl = false; }

                if (!inTable) {
                    html.append("<table cellpadding='6' cellspacing='0' style='border-collapse: separate; border-top: 2px solid #808080; border-left: 2px solid #808080; border-right: 2px solid #FFFFFF; border-bottom: 2px solid #FFFFFF; width: 100%; margin: 10px 0; background-color: #FFFFFF;'>\n");
                    inTable = true;
                    isFirstRow = true;
                }

                if (trimmedLine.matches("^\\|?\\s*[-:]+\\s*\\|.*")) continue;

                html.append("<tr>");
                String inner = trimmedLine.substring(1, trimmedLine.length() - 1);
                String[] cells = inner.split("\\|");

                for (String cellText : cells) {
                    if (isFirstRow) {
                        html.append("<th style='background-color: #D4D0C8; border-top: 1px solid #FFFFFF; border-left: 1px solid #FFFFFF; border-right: 1px solid #808080; border-bottom: 1px solid #808080; padding: 6px; text-align: left; font-weight: bold; color: #000000;'>");
                        html.append(processInlineMarkdown(cellText.trim()));
                        html.append("</th>");
                    } else {
                        html.append("<td style='border: 1px solid #808080; background-color: #FFFFFF; padding: 6px; color: #000000; vertical-align: top;'>");
                        html.append(processInlineMarkdown(cellText.trim()));
                        html.append("</td>");
                    }
                }
                html.append("</tr>\n");
                isFirstRow = false;
                continue;
            } else if (inTable) {
                html.append("</table><br>\n");
                inTable = false;
            }

            if (trimmedLine.matches("^[-*]\\s+.*")) {
                if (!inUl) { html.append("<ul style='margin-left: 20px; color: #000000;'>\n"); inUl = true; }
                html.append("<li>").append(processInlineMarkdown(trimmedLine.substring(2).trim())).append("</li>\n");
                continue;
            } else if (inUl) { html.append("</ul>\n"); inUl = false; }

            if (trimmedLine.matches("^\\d+\\.\\s+.*")) {
                if (!inOl) { html.append("<ol style='margin-left: 20px; color: #000000;'>\n"); inOl = true; }
                html.append("<li>").append(processInlineMarkdown(trimmedLine.substring(trimmedLine.indexOf(".") + 1).trim())).append("</li>\n");
                continue;
            } else if (inOl) { html.append("</ol>\n"); inOl = false; }

            String parsed = processInlineMarkdown(line);
            if (parsed.startsWith("### ")) {
                parsed = "<h3 style='margin: 8px 0; color: #333; font-weight: bold;'>" + parsed.substring(4) + "</h3>";
            } else if (parsed.startsWith("## ")) {
                parsed = "<h2 style='margin: 10px 0; color: #222; font-weight: bold;'>" + parsed.substring(3) + "</h2>";
            } else if (parsed.startsWith("# ")) {
                parsed = "<h1 style='margin: 12px 0; color: #111; font-weight: bold;'>" + parsed.substring(2) + "</h1>";
            } else {
                parsed += "<br>";
            }
            html.append(parsed).append("\n");
        }

        if (inTable) html.append("</table><br>\n");
        if (inUl) html.append("</ul>\n");
        if (inOl) html.append("</ol>\n");

        return html.toString();
    }

    public static String processInlineMarkdown(String text) {
        String parsed = text.replaceAll("(?i)<br\\s*/?>", "[[[BR]]]");
        parsed = parsed.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        parsed = parsed.replace("[[[BR]]]", "<br>");
        parsed = parsed.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        parsed = parsed.replaceAll("\\*([^\\*]+)\\*", "<i>$1</i>");
        parsed = parsed.replaceAll("`([^`]+)`", "<code style='background-color: #E8E8E8; color: #800000; font-family: \"Courier New\", monospace; padding: 1px 3px; border: 1px solid #CCC;'>$1</code>");
        return parsed;
    }
}
