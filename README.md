# Video Camera Alignment
Alignment of two cameras in case you would like to use them alternately, and need a seamless transition between their preview.

Problem: When you need to use two cameras for streaming or making a video (most of the time cameras with different specifications, for example, one with a wide angle and another one with narrow lenses) even if you put them very close to each other, they will see the scene from a different angle. Switching between the two cameras, you will see a shift in the preview, it won’t be seamless.

I wrote this software to align the two camera’s previews. It can be a part of the calibration process for companies, that manufacture conference cameras with built-in camera systems, or for individuals who glue together two cameras… 😊

Dependencies:

    OpenCV 4.6.0
    Java 1.8.0_341

It's written and developed on Windows 10, using IntelliJ.

About the cameras I used:

I used an “HD pro webcam C920” from Logitech and my laptop’s built-in camera. It is better to use the same type of camera with the same light sensitivity, color spectrum...


Describing the software:

The first step towards the solution is detecting keyPoints in both cameras' previews and then finding the corresponding ones.

As a feature detector, I used ORB (Oriented FAST and rotated BRIEF), which detects the keyPoints and computes the binary String descriptors. To find the common keyPoints I used BruteForce matcher based on comparing Hamming distance of the descriptors. The method finds a lot of matches, but several are wrong among them. I filtered the matching keyPoints even more, with the help of Calib3d.findHomography() method; as a result, I got a small but good subset of them. (better_matches)

Then the software calculates the average of the coordinates of the better_matches, resulting in one average point for each camera preview. This way the software can compute the shift direction and degree, using only the difference between the two average point coordinates.

With the direction and degree of the shift, the software automatically selects selects the part of the second camera image (by setting the viewport to the imageView), which fits the image of the first camera.

 ![gitar](https://user-images.githubusercontent.com/58810213/227979299-e1db3467-f845-40b8-a238-5765cfa15979.jpg)
The picture is a screenshot of the working app. The red dots are keypoints what the detector doesn't find match, blue ones the matching keypoints, the green is the matching keypoint average in every preview, the white one is the center of the screen.

