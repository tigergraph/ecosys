#!/bin/bash
cnt=0
while [ $cnt -lt 200 ]; do
    cnt=$((cnt+1))
    echo "iter: $cnt"
    gsql -g NeuralNetwork "run query training(1)"
    gsql -g NeuralNetwork "run query prediction_accuracy()"
done

