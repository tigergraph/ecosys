#!/bin/bash
gsql -g NeuralNetwork "DROP QUERY ALL"
gsql -g NeuralNetwork ./query/training.gsql
gsql -g NeuralNetwork ./query/prediction_accuracy.gsql
gsql -g NeuralNetwork ./query/prediction_sketchpad.gsql
gsql -g NeuralNetwork install query all


