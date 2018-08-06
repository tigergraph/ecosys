import java.util.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.io.FileWriter;

public class generateId {
	public static void main(String[] args){
		// args 0: root file
                String rootFile = args[0];

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(rootFile)));
			String resultFileName = "twitter_ids";
			String resultFilePath = "/Ebs/raw/twitter_rv/" + resultFileName;
			FileWriter writer = new FileWriter(resultFilePath);
			String line;
			while((line = reader.readLine()) != null){
				writer.write(line + "\t");
				writer.flush();
			}
			writer.flush();
			writer.close();
			reader.close();
		
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}

