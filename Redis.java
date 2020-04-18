
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.ArrayList;

public class Redis {
    //hashtable is synchronized, so it's thread safe
    private static Hashtable<String, String> globalHashtable = new Hashtable<>();
    private static Hashtable<String, TreeMap<String, String>> globalTreeMap = new Hashtable<>();

    public static void main (String[] args)
    {
        // your code goes here
        String input;
        Scanner scanner = new Scanner(System.in);
        System.out.print("redis>");
        while ((input = scanner.nextLine()).length()!=0){
            String [] partition = input.split(" ");
            String operationName = partition[0];

            switch (operationName) {
                case "GET": {
                    String value = GET(partition[1]);
                    System.out.println(value);
                    break;
                }
                case "SET":
                    if (partition.length == 3) {
                        SET(partition[1], partition[2]);
                        System.out.println("OK");
                    } else if (partition.length == 4) {
                        boolean isPossible = SET(partition[1], partition[2], partition[3]);
                        if (isPossible) {
                            System.out.println("OK");
                        } else {
                            System.out.println("ERROR");
                        }
                    } else if (partition.length == 5) {
                        long time = Long.parseLong(partition[4]);
                        SET(partition[1], partition[2], partition[3], time);
                    }
                    break;
                case "EXPIRE": {
                    String keyName = partition[1];
                    int seconds = Integer.parseInt(partition[2]);
                    int value = EXPIRE(keyName, seconds);
                    System.out.println(value);
                    break;
                }
                case "ZADD": {
                    String setName = partition[1];
                    int length = partition.length;
                    if (partition[length - 1].equals("XX") || partition[length - 1].equals("NX") || partition[length - 1].equals("CH")) {
                        int returnValue = ZADD(setName, Arrays.copyOfRange(partition, 2, length - 1), partition[length - 1]);
                        System.out.println(returnValue);
                    } else {
                        int returnValue = ZADD(setName, Arrays.copyOfRange(partition, 2, length));
                        System.out.println(returnValue);
                    }
                    break;
                }
                case "ZRANK": {
                    int value = ZRANK(partition[1], partition[2]);
                    if (value == -1) {
                        System.out.println("Nil");
                    } else {
                        System.out.println(value);
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

                case "QUIT":{
                	return;
                }
            }

            System.out.print("redis>");
        }
    }

    private static String GET(String key){
        if (globalHashtable.containsKey(key)){
            return globalHashtable.get(key);
        }
        return "Nil";
    }

    private static void SET(String key, String value){
        globalHashtable.put(key, value);
    }

    private static boolean SET(String key, String value, String operation){
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

    private static void SET(final String key, String value, String operation, long time){
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

    private static int ZADD(String setName, String [] partition){

        int i=0;
        int numberOfOperation =0;

        TreeMap<String, String> tempTreeMap;
        if (globalTreeMap.containsKey(setName)){
            tempTreeMap = globalTreeMap.get(setName);/*
            System.out.print(" treemap exists");*/
        } else{
            tempTreeMap = new TreeMap<>();/*
            System.out.print(" treemap does not exists");*/
        }

        while (i<partition.length){
            if (!tempTreeMap.containsKey(partition[i])) {
                numberOfOperation += 1;
            }
            tempTreeMap.put(partition[i+1], partition[i]);
            i+=2;
        }
        globalTreeMap.put(setName, tempTreeMap);

        return numberOfOperation;
    }

    private static int ZADD(String setName, String [] partition, String operations){
        int i=0;
        int numberOfNewElementsAdded =0;
        int numberOfOperations=0;

        TreeMap<String, String> tempTreeMap;
        if (globalTreeMap.containsKey(setName)){
            tempTreeMap = globalTreeMap.get(setName);
        } else{
            tempTreeMap = new TreeMap<>();
        }

        switch (operations) {
            //Only update elements that already exist. Never add elements.
            case "XX":
                while (i < partition.length) {
                    if (tempTreeMap.containsKey(partition[i])) {
                        numberOfNewElementsAdded += 1;
                        tempTreeMap.put(partition[i+1], partition[i]);
                    }
                    i += 2;
                }
                globalTreeMap.put(setName, tempTreeMap);
                return numberOfNewElementsAdded;

            //Don't update already existing elements. Always add new elements.
            case "NX":
                while (i < partition.length) {
                    if (!tempTreeMap.containsKey(partition[i])) {
                        numberOfNewElementsAdded += 1;
                        tempTreeMap.put(partition[i+1], partition[i]);
                    }
                    i += 2;
                }
                globalTreeMap.put(setName, tempTreeMap);
                return numberOfNewElementsAdded;

            //Don't update already existing elements. Always add new elements.
            case "CH":
                while (i < partition.length) {
                    numberOfOperations += 1;
                    tempTreeMap.put(partition[i+1], partition[i]);
                    i += 2;
                }
                globalTreeMap.put(setName, tempTreeMap);
                return numberOfOperations;
        }

        return 0;
    }

    private static int ZRANK(String setName, String value){
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

    private static void ZRANGE(String setName, int startRange, int endRange){
        if (globalTreeMap.containsKey(setName)){
            TreeMap<String, String> tempTreeMap = globalTreeMap.get(setName);
            if (tempTreeMap!=null){
                int length = tempTreeMap.size();
                if (startRange<0){
                	startRange = (startRange+length);
                }
                if (endRange<0) {
                	endRange = (endRange+length);
                }

                String[] keys = tempTreeMap.keySet().toArray(new String[0]);
                
                for (int i = startRange; i<endRange && i<keys.length; i++){
                    System.out.println(tempTreeMap.get(keys[i]));
                }
            } else{
            	System.out.println("Nil");
            }
        } else{
        	System.out.println("Nil");
        }
    }

    private static void ZRANGE(String setName, int startRange, int endRange, String operation){
        if (globalTreeMap.containsKey(setName)){
            TreeMap<String, String> tempTreeMap = globalTreeMap.get(setName);
            if (tempTreeMap!=null){
                int length = tempTreeMap.size();
                if (startRange<0){
                	startRange = (startRange+length);
                }
                if (endRange<0) {
                	endRange = (endRange+length);
                }

                String[] keys = tempTreeMap.keySet().toArray(new String[0]);
                for (int i = startRange; i<endRange && i<keys.length; i++){
                	System.out.println(keys[i]);
                	System.out.println(tempTreeMap.get(keys[i]));
                }
            } else{
            	System.out.println("Nil");
            }
        }else{
        	System.out.println("Nil");
        }
    }

    private static int EXPIRE(final String keyName, final int seconds){
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
