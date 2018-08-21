if [ "$#" -lt 1 ]
then
  echo "Please provide graph name: graph500 OR twitter"
elif [ "$1" = "graph500" ]
then
  echo "Running khop on graph500: k = 1"
  nohup java -cp .:\* khop graph500 1 180

  echo "Running khop on graph500: k = 2"
  nohup java -cp .:\* khop graph500 2 180

  echo "Running khop on graph500: k = 3"
  nohup java -cp .:\* khop graph500 3 9000

  echo "Running khop on graph500: k = 6"
  nohup java -cp .:\* khop graph500 6 9000
elif [ "$1" = "twitter" ]
then
  echo "Running khop on twitter: k = 1"
  nohup java -cp .:\* khop twitter 1 180

  echo "Running khop on twitter: k = 2"
  nohup java -cp .:\* khop twitter 2 180

  echo "Running khop on twitter: k = 3"
  nohup java -cp .:\* khop twitter 3 9000

  echo "Running khop on twitter: k = 6"
  nohup java -cp .:\* khop twitter 6 9000
fi
