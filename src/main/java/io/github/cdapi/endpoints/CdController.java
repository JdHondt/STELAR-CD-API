package io.github.cdapi.endpoints;

import _aux.lib;
import algorithms.performance.CorrelationDetective;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.RunParameters;
import core.StatBag;
import data_io.DataHandler;
import io.github.cdapi.enums.DatasetEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import netscape.javascript.JSObject;
import org.springframework.boot.logging.LoggerGroup;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import queries.QueryTypeEnum;
import queries.ResultSet;
import similarities.SimEnum;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/cd")
@Api(value = "Correlation Detective API", description = "Main API for running Correlation Detective jobs")
public class CdController {

    @PostMapping("/run")
    @ApiOperation(value = "Run a Correlation Detective job", response = UUID.class)
    public ResponseEntity runCdJob(
            @RequestBody Map<String, String> payLoad
    ){
        String[] requiredParams = new String[]{
                "inputPath",
                "outputPath",
                "maxPLeft",
                "maxPRight",
                "simMetric",
                "MINIO_ENDPOINT_URL",
                "MINIO_ACCESS_KEY",
                "MINIO_SECRET_KEY"
        };


//      0. Check if the required parameters are present
        for (String param : requiredParams){
            if (!payLoad.containsKey(param)){
                return ResponseEntity.badRequest().body(String.format("Missing required parameter: %s", param));
            }
        }

//        Generate a random runId
        UUID runId = UUID.randomUUID();

//      1. Get the required parameters
//        1.1 Set MINIO credentials
        System.setProperty("MINIO_ENDPOINT_URL", payLoad.remove("MINIO_ENDPOINT_URL"));
        System.setProperty("MINIO_ACCESS_KEY", payLoad.remove("MINIO_ACCESS_KEY"));
        System.setProperty("MINIO_SECRET_KEY", payLoad.remove("MINIO_SECRET_KEY"));

//        1.2 Input and output paths
        String inputPath = payLoad.remove("inputPath");
        String outputPath = payLoad.remove("outputPath");
//        Parse the inputPath and output parameter
        if (!inputPath.startsWith("s3://")){
            return ResponseEntity.badRequest().body("inputPath must be a Minio URL");
        }
        if (!outputPath.startsWith("s3://")){
            return ResponseEntity.badRequest().body("outputPath must be a Minio URL");
        }
        outputPath += "/" + runId;

//        1.3 maxPLeft and maxPRight
        String maxPLeftTmp = payLoad.remove("maxPLeft");
        String maxPRightTmp = payLoad.remove("maxPRight");
        int maxPLeft;
        int maxPRight;
        try {
            maxPLeft = Integer.parseInt(maxPLeftTmp);
            maxPRight = Integer.parseInt(maxPRightTmp);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("maxPLeft and maxPRight must be integers");
        }

//        1.4 simMetricName
        String simMetricName = payLoad.remove("simMetric");
        SimEnum simEnum;
        try {
            simEnum = SimEnum.valueOf(simMetricName);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(String.format("Unsupported simMetricName: %s, please choose any of: %s",
                    simMetricName, Arrays.toString(SimEnum.values())));
        }

//        2. Create a RunParameters object
        RunParameters runParameters = new RunParameters(inputPath, simEnum, maxPLeft, maxPRight);
        runParameters.setOutputPath(outputPath);

//        Set some default parameters
        runParameters.setQueryType(QueryTypeEnum.TOPK);
        runParameters.setParallel(false);
//        runParameters.setLogLevel(Level.WARNING);

//        3. Parse the rest of the parameters in the payload, and set them in the RunParameters object
        for (Map.Entry<String, String> entry : payLoad.entrySet()){
            try{
//                Try to parse the value
                Object value = lib.parseString(entry.getValue());
                runParameters.set(entry.getKey(), value);
            } catch (NoSuchFieldException | IllegalAccessException e){
                return ResponseEntity.badRequest().body(String.format("Unsupported parameter: %s", entry.getKey()));
            }
        }

//        Remove all current global logger handlers
        Arrays.stream(Logger.getGlobal().getHandlers()).forEach(Logger.getGlobal()::removeHandler);

//        4. Run the query
        CorrelationDetective cd = new CorrelationDetective(runParameters);
        try{
            cd.run();
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

//        5. Save the results and stats
        saveResults(runParameters);

//        6. Create graph visualization of results
        generateGraphViz(runParameters, runId);

//        6. Return the output path to the user
        return ResponseEntity.ok(outputPath);
    }

    private static void saveResults(RunParameters runParameters){
        String outputPath = runParameters.getOutputPath();

        Logger.getGlobal().info("Saving results and stats to " + outputPath);

        //        Create the output directory
        lib.createDir(outputPath);

        DataHandler outputHandler = runParameters.getOutputHandler();
        StatBag statBag = runParameters.getStatBag();
        ResultSet resultSet = runParameters.getResultSet();

//        Save the parameters as a json file
        outputHandler.writeToFile(outputPath + "/parameters.json", runParameters.toJson());

//        Save the statBag as a json file
        outputHandler.writeToFile(outputPath + "/stats.json", statBag.toJson());

//        Save the results as a json file
        outputHandler.writeToFile(outputPath + "/results.json", resultSet.toJson());
    }

    private static void saveAllResults(RunParameters runParameters, String outputPath) {
        //        Prepare the response
        Gson gson = new Gson();
        JsonElement runParametersJsonElement = runParameters.toJsonElement();
        JsonElement statBagJsonElement = runParameters.getStatBag().toJsonElement();
        JsonElement resultSetJsonElement = runParameters.getResultSet().toJsonElement(gson);

//        Combine the three json elements into one
        JsonObject response = new JsonObject();
        response.add("run_parameters", runParametersJsonElement);
        response.add("run_statistics", statBagJsonElement);
        response.add("results", resultSetJsonElement);

        //        Save the response as a json file
        data_io.FileHandler fileHandler = new data_io.FileHandler();
        fileHandler.writeToFile(outputPath, response.toString());
    }

    private static void generateGraphViz(RunParameters runParameters, UUID runId){
        Logger.getGlobal().info("Generating graph visualization of results");

//        Temporarily save the results as a json file
        String tmpJsonPath = "/tmp/" + runId + ".json";
        saveAllResults(runParameters, tmpJsonPath);

//        Generate the graph visualization by calling the python script
        String tmpHtmlPath = "/tmp/" + runId + ".html";
        String[] cmd = new String[]{
                "python3",
                "gvis.py",
                tmpJsonPath,
                "-o",
                tmpHtmlPath
        };

        Logger.getGlobal().info("Running command: " + String.join(" ", cmd));

//        Call python script, wait for it to finish, and print the output
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

//        Move the output to the correct location
        try{
            DataHandler outputHandler = runParameters.getOutputHandler();
            String html = Files.readString(java.nio.file.Path.of(tmpHtmlPath));
            outputHandler.writeToFile(runParameters.getOutputPath() + "/visualization.html", html);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
