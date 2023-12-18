import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

public class ConfigReader {

    private static Map<String, Object> readConfig(String directory) {
        Map<String, Object> indices = new HashMap<>();

        File folder = new File(directory);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".yaml")) {
                    try {
                        FileInputStream input = new FileInputStream(file);
                        Yaml yaml = new Yaml();
                        Map<String, Object> config = yaml.load(input);
                        indices.putAll(config);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return indices;
    }

    private static String search(String needle, String haystack) {
        Pattern pattern = Pattern.compile(needle);
        Matcher matcher = pattern.matcher(haystack);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    private static void processIndices(Map<String, Object> dd, IndexCallback callback) {
        if (dd == null || dd.isEmpty()) {
            return;
        }

        if (dd.containsKey("indices")) {
            callback.execute(dd);
        } else {
            for (Map.Entry<String, Object> entry : dd.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    processIndices((Map<String, Object>) entry.getValue(), callback);
                }
            }
        }
    }

    private static void extract(Map<String, Object> iidx, String pages) {
        IndexCallback grabCallback = d -> {
            Map<String, Object> regex = (Map<String, Object>) d.get("regex");
            List<Map<String, Object>> indices = (List<Map<String, Object>>) d.get("indices");

            for (Map<String, Object> index : indices) {
                for (Map.Entry<String, Object> entry : index.entrySet()) {
                    String indexKey = entry.getKey();
                    Map<String, Object> indexValue = (Map<String, Object>) entry.getValue();

                    if (regex.containsKey("sub")) {
                        indexKey = indexKey.replaceAll((String) regex.get("sub"), (String) regex.getOrDefault("repl", ""));
                    }

                    String value = search((String) regex.getOrDefault("pre", "") + indexKey + (String) regex.getOrDefault("post", ""), pages);
                    indexValue.put("value", value);
                }
            }
        };

        processIndices(iidx, grabCallback);
    }

    private static List<String[]> flatten(Map<String, Object> iidx) {
        List<String[]> indicesList = new ArrayList<>();

        IndexCallback showCallback = d -> {
            String group = (String) d.getOrDefault("group", "");
            String pre = (String) d.getOrDefault("short-pre", "");

            List<Map<String, Object>> indices = (List<Map<String, Object>>) d.get("indices");

            for (Map<String, Object> index : indices) {
                for (Map.Entry<String, Object> entry : index.entrySet()) {
                    String indexKey = entry.getKey();
                    Map<String, Object> indexValue = (Map<String, Object>) entry.getValue();

                    indicesList.add(new String[]{group, pre + indexValue.get("short").toString(), indexValue.get("value").toString()});
                }
            }
        };

        processIndices(iidx, showCallback);
        indicesList.sort((a, b) -> a[1].compareTo(b[1]));

        return indicesList;
    }

    private static void parseIndices(String[] pages) {
        Map<String, Object> indices = readConfig("indices");

        for (Map.Entry<String, Object> entry : indices.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> idx = (Map<String, Object>) entry.getValue();

            StringBuilder patternBuilder = new StringBuilder();
            for (String page : pages) {
                if (page.contains(key)) {
                    patternBuilder.append(page.replaceAll(".*" + key + ".*", ""));
                }
            }
            String pattern = patternBuilder.toString().replaceAll("\\n+\\s+\\d\\s+\\n+", " ").replaceAll("\\s+", " ");

            extract(idx, pattern);
        }

        List<String[]> result = flatten(indices);
        for (String[] entry : result) {
            System.out.println(entry[1] + ": " + entry[2]);
        }
    }
=

    interface IndexCallback {
        void execute(Map<String, Object> d);
    }
}
