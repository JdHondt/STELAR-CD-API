package io.github.cdapi.endpoints;

import _aux.lib;
import algorithms.performance.CorrelationDetective;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.RunParameters;
import io.github.cdapi.enums.DatasetEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import netscape.javascript.JSObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import queries.QueryTypeEnum;
import queries.ResultSet;
import similarities.SimEnum;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/cd")
@Api(value = "Correlation Detective API", description = "Main API for running Correlation Detective jobs")
public class CdController {

    @PostMapping("/run")
    @ApiOperation(value = "Run a Correlation Detective job", response = String.class)
    public ResponseEntity runCdJob(
            @RequestParam(value = "inputPath") String inputPath,
            @RequestParam(value = "simMetricName") String simMetricName,
            @RequestParam(value = "maxPLeft") String maxPLeftTmp,
            @RequestParam(value = "maxPRight") String maxPRightTmp,
            @RequestBody Map<String, String> payLoad
    ){

//        Process the request parameters
        int maxPLeft;
        int maxPRight;
        SimEnum simEnum;
//        Parse the inputPath parameter
        if (inputPath.startsWith("http://") || inputPath.startsWith("https://")){
//            Check if a dataToken is passed in the payload
            if (!payLoad.containsKey("dataToken")){
                return ResponseEntity.badRequest().body("When using a URL as inputPath, a dataToken must be passed in the payload");
            }
        } else { // If not an external URL, path needs to be one of the example datasets
            DatasetEnum datasetEnum;
            try {
                datasetEnum = DatasetEnum.valueOf(inputPath.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e){
                return ResponseEntity.badRequest().body(String.format("Unsupported inputPath: %s, please choose any of: %s",
                        inputPath, Arrays.toString(DatasetEnum.values())));
            }

            inputPath = String.format("src/main/resources/datasets/%s.csv", datasetEnum.name().toLowerCase(Locale.ROOT));
        }

//        Parse the maxPLeft and maxPRight parameters
        try {
            maxPLeft = Integer.parseInt(maxPLeftTmp);
            maxPRight = Integer.parseInt(maxPRightTmp);
        } catch (NumberFormatException e){
            return ResponseEntity.badRequest().body("maxPLeft and maxPRight must be integers");
        }

//        Parse the simMetricName parameter
        try{
            simEnum = SimEnum.valueOf(simMetricName);
        } catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(String.format("Unsupported simMetricName: %s, please choose any of: %s, or provide a minio URL",
                    simMetricName, Arrays.toString(SimEnum.values())));
        }

//        Create a RunParameters object
        RunParameters runParameters = new RunParameters(inputPath, simEnum, maxPLeft, maxPRight);

//        Set some default parameters
        runParameters.setQueryType(QueryTypeEnum.TOPK);
        runParameters.setParallel(false);
//        runParameters.setLogLevel(Level.WARNING);

//        Parse the rest of the parameters in the payload, and set them in the RunParameters object
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

//        Run the query
        CorrelationDetective cd = new CorrelationDetective(runParameters);
        cd.run();

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

        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }
}
