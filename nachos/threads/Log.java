package nachos.threads;

import nachos.machine.*;
import java.io.*;

public class Log {
    public static void write(String s){
        if(fileName == null)
            System.out.println(s);
        else
            writer.println(s);
    }

    public static long getTimeInMillis(){
        return System.currentTimeMillis() - startTime;
    }

    public static void init(){
        //get file name specified
        fileName = Config.getString("statistics.logFile");
        
        if(fileName != null){
            try{
                writer = new PrintWriter(fileName, "UTF-8");
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        //get the initial system time
        startTime = System.currentTimeMillis();
        
    }

    public static void destroy(){
        if(fileName != null)
            writer.close();
    }

    private static long startTime = 0;
    private static PrintWriter writer;
    private static String fileName = null;
}
