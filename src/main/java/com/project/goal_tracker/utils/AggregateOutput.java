package com.project.goal_tracker.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AggregateOutput<T>{

    public static final String USER_NOT_FOUND = "user_not_found";
    public static final String GOAL_NOT_FOUND = "goal_not_found";
    public static final String PROGRESS_NOT_FOUND = "progress_not_found";

    private final String ERROR = "errors";
    private final String WARNING = "warnings";
    private final String INFO = "info";

    private List<T> data = new ArrayList<>();

    private HttpStatusCode status = HttpStatus.OK;

    private HashMap<String, HashMap<String,String>> outputDict = new HashMap<>();

    public AggregateOutput(){
        outputDict.put(ERROR,new HashMap<String,String>());
        outputDict.put(WARNING,new HashMap<String,String>());
        outputDict.put(INFO, new HashMap<String,String>());
    }

    public void gLog(String level, String key, String text){

        outputDict.get(level).put(key,text);
    }

    public void gLog(String level, String key, String text, HttpStatusCode status){
        outputDict.get(level).put(key,text);
        this.status = status;
    }

    // Error logs
    public void error(String key, String text) {
        gLog(ERROR, key, text);
    }

    public void error(String key, String text, HttpStatusCode status) {
        gLog(ERROR, key, text, status);
    }

    // Warning logs
    public void warning(String key, String text) {
        gLog(WARNING, key, text);
    }

    public void warning(String key, String text, HttpStatusCode status) {
        gLog(WARNING, key, text, status);
    }

    // Info logs
    public void info(String key, String text) {
        gLog(INFO, key, text);
    }

    public void info(String key, String text, HttpStatusCode status) {
        gLog(INFO, key, text, status);
    }

    public void append(T value){
        this.data.add(value);
    }

    public void setData(List<T> values){
        this.data = values;
    }

    public HashMap<String, Object> getOutput() {
        HashMap<String, Object> fullOutput = new HashMap<>();
        fullOutput.put("data", data);
        fullOutput.put(ERROR,this.outputDict.get(ERROR));
        fullOutput.put(WARNING,this.outputDict.get(WARNING));
        fullOutput.put(INFO,this.outputDict.get(INFO));
        return fullOutput;
    }

    public boolean haveErrors(){
        return this.outputDict.get(ERROR).size() != 0;
    }


    public HttpStatusCode getStatus() {
        return this.status;
    }

    public AggregateOutput<T> setStatus(HttpStatusCode status){
        this.status = status;
        return this;
    }

    public ResponseEntity<HashMap<String, Object>> toResponseEntity() {
        return ResponseEntity.status(status).body(getOutput());
    }
}
