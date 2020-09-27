M6809 - An ET-3404A simulator for Java
by Dave Sherman

This is a functional simulation of the Heathkit ET-3400A/ET-3404 written in Java.
It simulates a virtual Motorola 6809 CPU, and mimics the behavior of the
ET-3400A buttons and 7-segment display.  I am an embedded
systems engineer, which helped considerably when writing the 6809 simulator,
but I'm a newb when it comes to Java.  I'm sure the GUI could have been done
better, but the buttons work, the 7 segment displays work, and I have tested
the functionality of all the ET-3400A functions.  Auto entry, breakpoints,
single stepping, examining memory contents, changing memory contents, and
viewing registers all behave as expected.


How to use:

You'll need Java installed.  In the M6809/dist directory, there is a Java
runtime, M6809.jar.  Windows 10 seems to recognize Java runtimes as
executable programs, so you may be able to double-click on it and have it
run.  If not, open a command prompt, go to the directory where the M6809.jar
file is located, and issue "java -jar M6809.jar".

This has been tested in Windows 10 and in Linux Mint 19.3

The rest of the directory includes the source and Netbeans project for the
simulator.

Known issues:
-Any bugs in the ET-3400A ROM are still present since it runs the exact ET-3400A
ROM image.  There may be bugs in the simulator I haven't found yet, but I fixed a
few in the development of it.
