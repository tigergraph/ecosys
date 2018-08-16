import java.io.BufferedReader;
import java.util.*;
import java.io.FileWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class dedup{
  public static void main(String[] args){
    try{
      File file = new File("/Ebs/raw/graph500-22/graph500-22");
      FileReader fileReader = new FileReader(file);
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      StringBuffer stringBuffer = new StringBuffer();
      String line;

      String resultPath = "/Ebs/raw/graph500-22/graph500-22-dedup";
      FileWriter writer = new FileWriter(resultPath);

      HashSet<String> vertex = new HashSet<String>();

      while((line = bufferedReader.readLine()) != null){
        if(!vertex.contains(line)){
          writer.write(line + '\n');
          vertex.add(line);
        }

      }
     writer.flush();
     writer.close();
     bufferedReader.close();

    }catch(IOException e){
      e.printStackTrace();
    }

  }
}

