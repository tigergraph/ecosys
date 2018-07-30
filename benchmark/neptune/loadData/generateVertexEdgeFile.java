import java.io.BufferedReader;
import java.util.*;
import java.io.FileWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class generateVertexEdgeFile{
  /**
   * This function generate vertex file and edge file for Amazon Neptune
   */
  public static void main(String[] args){
    try{
      File file = new File("/data/graph500-22");
      String filePath = "/data/graph500_edge";
      String vertexPath = "/data/graph500_vertex";
      String label = "graph500";

      FileReader fileReader = new FileReader(file);
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      StringBuffer stringBuffer = new StringBuffer();
      String line;
      FileWriter vertexWriter = new FileWriter(vertexPath);
      FileWriter writer = new FileWriter(filePath);

      HashSet<String> vertex = new HashSet<String>();

      writer.write("~id,~from,~to,~label\n");
      vertexWriter.write("~id,~label\n");
      while((line = bufferedReader.readLine()) != null){
        String A[] = line.split("\\t");

        // generate vertex file
        if(!vertex.contains(A[0])){
          vertexWriter.write(A[0] + "," + label + '\n');
          vertex.add(A[0]);
        }
        if(!vertex.contains(A[1])){
          vertexWriter.write(A[1] + "," + label + '\n');
          vertex.add(A[1]);
        }

        // generate edge file
        String edgeId = A[0] + "_" +A[1];
        writer.write(edgeId + ",");
        writer.write(A[0] + ",");
        writer.write(A[1] + "," + label + '\n');
      }
     writer.flush();
     writer.close();
     vertexWriter.flush();
     vertexWriter.close();
     bufferedReader.close();
    }catch(IOException e){
      e.printStackTrace();
    }

  }
}
