import java.util.*;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileSplitor {
	public static void main(String[] args){
		String datasetDir = args[0];
		long lineCounter = 0;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datasetDir)));
			for (int i = 0; i < 10; i++) {
				PrintWriter writer = new PrintWriter("twitter_rv_" + i, "UTF-8");
				String line;
				long startTime = System.nanoTime();
				for (long j = 0; j < 160000000; j++) {
					if((line = reader.readLine()) == null) {
						break;
					}
					writer.println(line);
					lineCounter++;
					
				}
				long endTime = System.nanoTime();
				long duration = (endTime - startTime);
				System.out.println("######## loading time #######  " + Long.toString(duration/1000000) + " ms");
				writer.close();
			}
			reader.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("---- done ----, total lines: " + lineCounter);
	}
}
