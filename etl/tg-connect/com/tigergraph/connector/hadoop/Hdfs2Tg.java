package com.tigergraph.connector.hadoop;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Hdfs2Tg {
    public static class LineCountMapper extends
        Mapper<Object, Text, Text, IntWritable> {

            HadoopTGWriter myTGWriter;

            protected void setup(Context context) throws IOException, InterruptedException {
                try {
                    /*
                     * In task's setup, get TigerGraph system and loading job configuration
                     * and construct the TigerGraph data writer.
                     * For illustration, the configure file is hard coded and duplicated to 
                     * each data node.
                     */
                    TGHadoopConfig tghadoopcfg = new TGHadoopConfig("/tmp/tg_demo.json");
                    this.myTGWriter = new HadoopTGWriter(tghadoopcfg);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }


            public void map(Object key, Text value, Context context)
                    throws IOException, InterruptedException {
                    try {
                        /*
                         *  Send each line to TG. Note the batch size is set
                         *  in configuration.
                         */

                        myTGWriter.BatchPost(value.toString());
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
            }


            protected void cleanup(Context context)
                    throws IOException, InterruptedException{
                    try {
                        /*
                         * In task's cleanup, close the TG writer which will 
                         * flush out any data in its buffer.
                         */  
                        myTGWriter.close();
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
            }
        }


    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "HDFS to TG");
        job.setJarByClass(Hdfs2Tg.class);
        job.setMapperClass(LineCountMapper.class);
        job.setNumReduceTasks(0);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
