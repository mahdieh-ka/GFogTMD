GFogTMD contains two different modules:

1- collection module: A module to record ground truth for generating a new Neural Network classifier.

2- Detection module: A module using an MLP classifier to detect the user's modes. 

collection module:
The app uses three different sensors including GPS, accelerometer, and magnetometer. The app relies on Firebase datastore for collecting your tips and fetching them. Please request access from the developer to define your access to the datastore in order to use the app. 

Detection module:
The detection module uses the collection module for reading sensor data in real-time. It then sends the raw data to the classifier and generates a list of the probabilities for 9 mode classes: still, walk, bike, car, bus, train, tram, subway, and run. 
This module contains a TensorflowLite MLP model exported from Keras. This model is a base model trained on data from four different cities.

