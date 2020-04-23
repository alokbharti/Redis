package alok.redis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.ArrayList;

import java.io.DataOutputStream;
import java.io.IOException;

public class Redis {
    //hashtable is synchronized, so it's thread safe
    private Hashtable<String, String> globalHashtable = new Hashtable<>();
    private Hashtable<String, TreeMap<String, String>> globalTreeMap = new Hashtable<>();
    private DataOutputStream dataOutputStream;

    public Redis(DataOutputStream dataOutputStream)
    {
        this.dataOutputStream = dataOutputStream;
    }

    public void executeAction(String input){
        //partition array for seperating commands, values and other possible options
        String [] partition = input.split(" ");
        String operationName = partition[0];
        try{
            switch (operationName) {
                case "GET": {
                    String value = GET(partition[1]);
                    dataOutputStream.writeUTF(value);
                    break;
                }
                case "SET":{
                    //get value from double quotes
                    String [] partition2 = input.split("\"");
                    String value = partition2[1];
                    if (partition2.length == 3) {
                        //look for the extra options...
                        String [] extraOps = partition2[2].split(" ");
                        //when SET is requested with commands like XX, NX
                        if (extraOps.length == 2) {
                            boolean isPossible = SET(partition[1], value, extraOps[1]);
                            if (isPossible) {
                                dataOutputStream.writeUTF("OK");
                            } else {
                                dataOutputStream.writeUTF("ERROR");
                            }
                        }
                        //when SET is requested with commands like PX, EX along with time
                        else if (extraOps.length == 3) {
                            long time = Long.parseLong(extraOps[2]);
                            SET(partition[1], value, extraOps[1], time);
                            dataOutputStream.writeUTF("OK");
                        }
                    }
                    else{
                    // when only SET is used without any extra options
                        SET(partition[1], value);
                        dataOutputStream.writeUTF("OK");
                    }
                    break;
                }
                case "EXPIRE": {
                    String keyName = partition[1];
                    int seconds = Integer.parseInt(partition[2]);
                    int value = EXPIRE(keyName, seconds);
                    dataOutputStream.writeUTF(String.valueOf(value));
                    break;
                }
                case "ZADD": {
                    String setName = partition[1];
                    int length = partition.length;
                    //extract only key-value from input
                    String keyValuePairs = String.join(" ", Arrays.copyOfRange(partition, 2, length));
                        
                    if (partition[length - 1].equals("XX") || partition[length - 1].equals("NX") || partition[length - 1].equals("CH")) {
                        int returnValue = ZADD(setName, keyValuePairs, partition[length - 1]);
                        dataOutputStream.writeUTF(String.valueOf(returnValue));
                    } else {
                        int returnValue = ZADD(setName, keyValuePairs);
                        dataOutputStream.writeUTF(String.valueOf(returnValue));
                    }
                    break;
                }
                case "ZRANK": {
                    String [] partition2 = input.split("\"");
                    String value = partition2[1];
                    int rank = ZRANK(partition[1], value);
                    if (rank == -1) {
                        dataOutputStream.writeUTF("Nil");
                    } else {
                        dataOutputStream.writeUTF(String.valueOf(rank));
                    }
                    break;
                }
                case "ZRANGE": {
                    int length = partition.length;
                    int startIndex = Integer.parseInt(partition[2]);
                    int endIndex = Integer.parseInt(partition[3]);
                    if (length == 4) {
                        ZRANGE(partition[1], startIndex, endIndex);
                    } else {
                        ZRANGE(partition[1], startIndex, endIndex, partition[4]);
                    }
                    break;
                }
            }
        } catch(IOException exception){
            //failed to write
        }
    }

    private String GET(String key){
        if (globalHashtable.containsKey(key)){
            return globalHashtable.get(key);
        }
        return "Nil";
    }

    private void SET(String key, String value){
        globalHashtable.put(key, value);
    }

    private boolean SET(String key, String value, String operation){
        switch(operation) {
            case "NX": //Only set the key if it does not already exist.
                if (globalHashtable.containsKey(key)) {
                    return false;
                } else {
                    globalHashtable.put(key, value);
                    return true;
                }

            case "XX": //Only set the key if it already exist.
                if (globalHashtable.containsKey(key)) {
                    globalHashtable.put(key, value);
                    return true;
                } else {
                    return false;
                }
        }
        return false;
    }

    private void SET(final String key, String value, String operation, long time){
        final int[] counter = {0};
        if (operation.equals("PX")){
            //time in milliseconds, convert it to seconds
            time = time/1000;
        }

        globalHashtable.put(key, value);
        final long finalTime = time;
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (counter[0] == finalTime){
                    this.cancel();
                    globalHashtable.remove(key);
                } else{
                    counter[0]++;
                }
            }
        }, 0,1000);

    }

    private int ZADD(String setName, String keyValuePair){

        int i=0;
        int numberOfOperation =0;

        TreeMap<String, String> tempTreeMap;
        //check if treemap already exists or not
        if (globalTreeMap.containsKey(setName)){
            tempTreeMap = globalTreeMap.get(setName);
        } else{
            tempTreeMap = new TreeMap<>();
        }

      	String [] key_value_pairs = keyValuePair.split("\"");
      	int keyValuePairLength = key_value_pairs.length;
        while (i < keyValuePairLength){
        	//It returns number of new addition not total changes

        	//remove spaces from keys which can come after partitioning
        	String tempKey = key_value_pairs[i].replaceAll("\\s","");
            if (!tempTreeMap.containsKey(tempKey)) {
                numberOfOperation += 1;
            }
            tempTreeMap.put(tempKey, key_value_pairs[i+1]);
            i+=2;
        }
        globalTreeMap.put(setName, tempTreeMap);

        return numberOfOperation;
    }

    private int ZADD(String setName, String keyValuePair, String operations){
        int i=0;
        int numberOfNewElementsAdded =0;
        int numberOfOperations=0;

        TreeMap<String, String> tempTreeMap;
        if (globalTreeMap.containsKey(setName)){
            tempTreeMap = globalTreeMap.get(setName);
        } else{
            tempTreeMap = new TreeMap<>();
        }

      	String [] key_value_pairs = keyValuePair.split("\"");
      	int keyValuePairLength = key_value_pairs.length;

        switch (operations) {
            //Only update elements that already exist. Never add elements.
            case "XX":
                while (i < keyValuePairLength) {
        			String tempKey = key_value_pairs[i].replaceAll("\\s","");
                    if (tempTreeMap.containsKey(tempKey)) {
                        numberOfNewElementsAdded += 1;
                        tempTreeMap.put(tempKey, key_value_pairs[i+1]);
                    }
                    i += 2;
                }
                globalTreeMap.put(setName, tempTreeMap);
                return numberOfNewElementsAdded;

            //Don't update already existing elements. Always add new elements.
            case "NX":
                while (i < keyValuePairLength) {
        			String tempKey = key_value_pairs[i].replaceAll("\\s","");
                    if (!tempTreeMap.containsKey(tempKey)) {
                        numberOfNewElementsAdded += 1;
                        tempTreeMap.put(tempKey, key_value_pairs[i+1]);
                    }
                    i += 2;
                }
                globalTreeMap.put(setName, tempTreeMap);
                return numberOfNewElementsAdded;

            //Don't update already existing elements. Always add new elements.
            case "CH":
                while (i < keyValuePairLength) {
        			String tempKey = key_value_pairs[i].replaceAll("\\s","");
                    numberOfOperations += 1;
                    tempTreeMap.put(tempKey, key_value_pairs[i+1]);
                    i += 2;
                }
                globalTreeMap.put(setName, tempTreeMap);
                return numberOfOperations;
        }

        return 0;
    }

    private int ZRANK(String setName, String value){
        int count=0;
        if (globalTreeMap.containsKey(setName)){
            TreeMap<String, String> tempTreeMap = globalTreeMap.get(setName);
            if (tempTreeMap!=null && tempTreeMap.containsValue(value)){
                for (String tempValue: tempTreeMap.values()){
                    if (tempValue.equals(value)){
                        return count;
                    }
                    count++;
                }
            }
        }
        return -1;
    }

    private void ZRANGE(String setName, int startRange, int endRange) {
        try{
            if (globalTreeMap.containsKey(setName)){
                TreeMap<String, String> tempTreeMap = globalTreeMap.get(setName);
                if (tempTreeMap!=null){
                    int length = tempTreeMap.size();
                    if (startRange<0){
                    	startRange = (startRange+length);
                    }
                    if (endRange<0) {
                    	endRange = (endRange+length)+1;
                    }

                    String[] keys = tempTreeMap.keySet().toArray(new String[0]);
                    
                    for (int i = startRange; i<endRange && i<keys.length; i++){
                        dataOutputStream.writeUTF(tempTreeMap.get(keys[i]));
                    }
                } else{
                	dataOutputStream.writeUTF("Nil");
                }
            } else{
            	dataOutputStream.writeUTF("Nil");
            }
        } catch(IOException exception){
            //failed to write
        }
    }

    //ZRANGE with WITHSCORES option
    private void ZRANGE(String setName, int startRange, int endRange, String operation) {
        try{    
            if (globalTreeMap.containsKey(setName)){
                TreeMap<String, String> tempTreeMap = globalTreeMap.get(setName);
                if (tempTreeMap!=null){
                    int length = tempTreeMap.size();
                    if (startRange<0){
                    	startRange = (startRange+length);
                    }
                    if (endRange<0) {
                    	endRange = (endRange+length)+1;
                    }

                    String[] keys = tempTreeMap.keySet().toArray(new String[0]);
                    for (int i = startRange; i<endRange && i<keys.length; i++){
                    	dataOutputStream.writeUTF(tempTreeMap.get(keys[i]));
                    	dataOutputStream.writeUTF(keys[i]);
                    }
                } else{
                	dataOutputStream.writeUTF("Nil");
                }
            }else{
            	dataOutputStream.writeUTF("Nil");
            }
        } catch(IOException exception){
            //failed to write
        }
    }

    private int EXPIRE(final String keyName, final int seconds){
        final int[] counter = {0};
        if (globalHashtable.containsKey(keyName)){
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (counter[0] == seconds){
                        globalHashtable.remove(keyName);
                        this.cancel();
                    }
                    counter[0]++;
                }
            },0,1000);
            return 1;
        }
        return 0;
    }

}
