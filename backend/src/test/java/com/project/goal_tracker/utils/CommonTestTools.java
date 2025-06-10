package com.project.goal_tracker.utils;


import org.springframework.test.web.servlet.MvcResult;




public class CommonTestTools {


    public static void debugLog(MvcResult result){

        int status = result.getResponse().getStatus();

        String responseBody = "Could not retrieve content as string: \n";
        try{
            responseBody = result.getResponse().getContentAsString();
        }catch (Exception e){
            responseBody += e.getMessage();
            responseBody += "\n Stack trace:";
            responseBody += e.getStackTrace();

        }

        String endpoint = result.getRequest().getRequestURI();

        System.out.println("Endpoint hit: " + endpoint);
        System.out.println("Response status: " + status);
        System.out.println("Response body: " + responseBody);
    }
}
