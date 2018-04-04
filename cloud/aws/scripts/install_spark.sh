sudo apt-get install default-jdk -y
sudo apt-get install scala -y
wget http://apache.claz.org/spark/spark-2.2.1/spark-2.2.1-bin-hadoop2.7.tgz
tar xvzf spark-2.2.1-bin-hadoop2.7.tgz
rm -rf spark-2.2.1-bin-hadoop2.7.tgz
sudo mv spark-2.2.1-bin-hadoop2.7 /usr/local/spark
echo 'export SPARK_HOME=/usr/local/spark' >> ~/.bashrc
echo 'export PATH=$PATH:$SPARK_HOME/bin' >> ~/.bashrc
source ~/.bashrc
