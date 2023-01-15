package makamys.mclib.updatecheck;

import static makamys.mclib.updatecheck.UpdateCheckLib.LOGGER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import makamys.mclib.updatecheck.UpdateCheckLib.UpdateCategory;
import makamys.mclib.updatecheck.ResultHTMLRenderer;

public class ResultHTMLRenderer {
    
    private static final String TABLE_TEMPLATE = 
              "<h1>%s</h1>\n"
            + "<table>\n"
            + "	<thead>\n"
            + "		<th>%s</th>\n"
            + "		<th>%s</th>\n"
            + "		<th>%s</th>\n"
            + "		<th>%s</th>\n"
            + "	</thead>\n"
            + "	%s\n"
            + "</table>\n\n";
    
    private static final String TABLE_ROW_TEMPLATE = 
              "	<tr>\n"
            + "		<td>%s</td>\n"
            + "		<td>%s</td>\n"
            + "		<td>%s</td>\n"
            + "		<td>%s</td>\n"
            + "	</tr>";
    
    private static final String TABLE_HOMEPAGE_TEMPLATE = "<a href=\"%s\">%s</a>";
    private static final String TABLE_HOMEPAGE_SEPARATOR = " | ";
    
    private static final String
    FIELD_NAME = "Name",
    FIELD_CURRENT_VERSION = "Installed version",
    FIELD_NEW_VERSION = "Latest version",
    FIELD_URL = "Update link",
    TABLE_TITLE_TEMPLATE = "%s updates";
    
    public ResultHTMLRenderer() {
        
    }
    
    private boolean hasAnythingToDisplay() {
        return UpdateCheckLib.categories.values().stream().anyMatch(cat -> cat.results.stream().anyMatch(r -> r.isInteresting()));
    }
    
    public boolean render(File outFile) {
        if(!hasAnythingToDisplay()) {
            outFile.delete();
        } else {
            try (FileOutputStream out = new FileOutputStream(outFile)){
                String template = IOUtils.toString(ResultHTMLRenderer.class.getClassLoader().getResourceAsStream("resources/mclib/updatecheck/updates.template.html"));
                String html = template.replace("{table}", generateTables());
                IOUtils.write(html, out, "utf8");
                LOGGER.info("Wrote update check results to " + outFile);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
    
    private String generateTables() {
        final StringBuffer tables = new StringBuffer();
        
        List<UpdateCategory> first = Arrays.asList(UpdateCheckLib.MODS, UpdateCheckLib.RESOURCE_PACKS); 
        
        Stream.concat(first.stream(), UpdateCheckLib.categories.values().stream().sorted().filter(c -> !first.contains(c))).forEach(cat -> {
            List<UpdateCheckTask.Result> interestingResults = cat.results.stream().filter(r -> r.isInteresting()).collect(Collectors.toList());
            
            if(!interestingResults.isEmpty()) {
                String tableTitle = cat.displayName;
                String rows = "";
                for(UpdateCheckTask.Result result : interestingResults) {
                    String newVersionStr = result.newVersion != null ? htmlEscape(result.newVersion.toString()) : "<b>ERROR</b>";
                    String homepagesStr = String.join(TABLE_HOMEPAGE_SEPARATOR, result.task.homepages.stream().map(hp -> String.format(TABLE_HOMEPAGE_TEMPLATE, urlEscape(hp.url), htmlEscape(hp.display))).toArray(String[]::new));
                    rows += String.format(TABLE_ROW_TEMPLATE, htmlEscape(result.task.name), htmlEscape(result.task.currentVersion.toString()), newVersionStr, homepagesStr);
                }
                
                tables.append(String.format(TABLE_TEMPLATE, String.format(TABLE_TITLE_TEMPLATE, htmlEscape(tableTitle)), FIELD_NAME, FIELD_CURRENT_VERSION, FIELD_NEW_VERSION, FIELD_URL, rows));
            }
        });
        return tables.toString();
    }

    private static String htmlEscape(String str) {
        return StringEscapeUtils.escapeHtml4(str);
    }
    
    private static String urlEscape(String str) {
        try {
            // I don't know the proper way to do this, hopefully this is good enough!
            String encoded = URLEncoder.encode(str, StandardCharsets.UTF_8.toString())
                .replaceAll("%3A", ":")
                .replaceAll("%2F", "/");
            return encoded;
        } catch (UnsupportedEncodingException e) {
            return "ERROR";
        }
    }
    
}
