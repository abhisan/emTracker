package me.pushy.sdk.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;

public class PushyIO {
    public static void writeToFile(String path, String data) throws Exception {
        new File(path).getParentFile().mkdirs();
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(path));
        writer.write(data);
        writer.close();
    }

    public static String readFromFile(String path) throws Exception {
        String contents = null;
        File file = new File(path);
        if (file != null) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            contents = bufferedReader.readLine();
        }
        return contents;
    }
}

