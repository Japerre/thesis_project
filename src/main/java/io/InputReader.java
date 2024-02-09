package io;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.Data;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class InputReader {

    public static ImmutablePair<Data, String> loadData(String inputDataDefenitionPath, String inputDataPath, boolean ldiv) throws IOException {

        FileReader fileReader = new FileReader(inputDataDefenitionPath);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(fileReader, JsonObject.class);

        Data data = Data.create(inputDataPath, StandardCharsets.UTF_8, ';');

        JsonArray qidArray = jsonObject.getAsJsonArray("QID");
        for (int i = 0; i < qidArray.size(); i++) {
            JsonObject qidObject = qidArray.get(i).getAsJsonObject();
            String colName = qidObject.get("colName").getAsString();
            String hierarchyPath = qidObject.get("hierarchyPath").getAsString();
            data.getDefinition().setAttributeType(colName, AttributeType.Hierarchy.create(hierarchyPath, StandardCharsets.UTF_8, ';'));
        }

        JsonArray ISArray = jsonObject.getAsJsonArray("IS");
        for (int i = 0; i < ISArray.size(); i++) {
            data.getDefinition().setAttributeType(ISArray.get(i).getAsString(), AttributeType.INSENSITIVE_ATTRIBUTE);
        }

        //SA attributes need to be put as IS because otherwise the ARX library doesn't let us use solely k-anonimity,
        //it wants us to use something like l-diversity as well to protect against attribute disclosure.
        JsonArray SAarray = jsonObject.getAsJsonArray("SA");
        for (int i = 0; i < SAarray.size(); i++) {
            data.getDefinition().setAttributeType(
                    SAarray.get(i).getAsString(),
                    ldiv ? AttributeType.SENSITIVE_ATTRIBUTE : AttributeType.INSENSITIVE_ATTRIBUTE
            );
        }

        String target = jsonObject.get("target").getAsString();

        return ImmutablePair.of(data, target);
    }

    public static String getTarget(String inputDataDefenitionPath) throws FileNotFoundException {
        FileReader fileReader = new FileReader(inputDataDefenitionPath);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(fileReader, JsonObject.class);
        return jsonObject.get("target").getAsString();
    }

    public static SampleInput readSampleInput(String experimentStatsFile, Configuration cfg) throws IOException {
        Reader in = new FileReader(experimentStatsFile);
        String[] headers = {
                "k",
                "b",
                "experimentBasePath",
                "kAnonFolderPath",
                "inputDatasetPath",
                "inputDataDefenitionPath",
                "inputDataDefenitionAbsolutePath",
                "QID"
        };
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .setSkipHeaderRecord(true)
                .setDelimiter(';')
                .build();
        Iterable<CSVRecord> records = csvFormat.parse(in);

        String kValues = "";
        String experimentBasePath=""; String kAnonFolderPath=""; String inputDatasetPath=""; String inputDataDefenitionPath="";
        for (CSVRecord record : records){
            kValues = record.get("k");
            experimentBasePath = record.get("experimentBasePath");
            kAnonFolderPath = record.get("kAnonFolderPath");
            inputDatasetPath = record.get("inputDatasetPath");
            inputDataDefenitionPath= record.get("inputDataDefenitionPath");
        }

        // Remove brackets and split by ","
        String[] values = kValues.substring(1, kValues.length() - 1).split(", ");

        // Convert array to int array
        int[] intArray = Arrays.stream(values)
                .mapToInt(Integer::parseInt)
                .toArray();

        return new SampleInput(intArray, experimentBasePath, kAnonFolderPath, inputDatasetPath, inputDataDefenitionPath, cfg.getString("foldsPath"));

    }

    public static class SampleInput {
        public final int[] kArr;
        public final String experimentBasePath;
        public final String kAnonFolderPath;
        public final String inputDatasetPath;
        public final String inputDataDefenitionPath;
        public final String foldsDirPath;

        public SampleInput(int[] kArr, String experimentBasePath, String kAnonFolderPath, String inputDatasetPath, String inputDataDefenitionPath, String foldsDirPath) {
            this.kArr = kArr;
            this.experimentBasePath = experimentBasePath;
            this.kAnonFolderPath = kAnonFolderPath;
            this.inputDatasetPath = inputDatasetPath;
            this.inputDataDefenitionPath = inputDataDefenitionPath;
            this.foldsDirPath = foldsDirPath;
        }

        @Override
        public String toString() {
            return "SampleInput{" +
                    "kArr=" + Arrays.toString(kArr) +
                    ", experimentBasePath='" + experimentBasePath + '\'' +
                    ", kAnonFolderPath='" + kAnonFolderPath + '\'' +
                    ", inputDatasetPath='" + inputDatasetPath + '\'' +
                    '}';
        }
    }

}
