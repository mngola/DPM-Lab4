package lab4Code;

import lejos.hardware.*;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.*;
import lejos.robotics.SampleProvider;

public class Lab4 {

	// Static Resources:
	// Left motor connected to output A
	// Right motor connected to output D
	// Ultrasonic sensor port connected to input S1
	// Color sensor port connected to input S2
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	private static final Port usPort = LocalEV3.get().getPort("S1");		
	private static final Port colorPort = LocalEV3.get().getPort("S2");
	private static TextLCD LCD = LocalEV3.get().getTextLCD();
	
	public static void main(String[] args) {
		
		//Setup ultrasonic sensor
		// 1. Create a port object attached to a physical port (done above)
		// 2. Create a sensor instance and attach to port
		// 3. Create a sample provider instance for the above and initialize operating mode
		// 4. Create a buffer for the sensor data
		@SuppressWarnings("resource")							    	// Because we don't bother to close this resource
		SensorModes usSensor = new EV3UltrasonicSensor(usPort);
		SampleProvider usValue = usSensor.getMode("Distance");			// colorValue provides samples from this instance
		float[] usData = new float[usValue.sampleSize()];				// colorData is the buffer in which data are returned
		
		//Setup color sensor
		// 1. Create a port object attached to a physical port (done above)
		// 2. Create a sensor instance and attach to port
		// 3. Create a sample provider instance for the above and initialize operating mode
		// 4. Create a buffer for the sensor data
		SensorModes colorSensor = new EV3ColorSensor(colorPort);
		SampleProvider colorValue = colorSensor.getMode("Red");			// colorValue provides samples from this instance
		float[] colorData = new float[colorValue.sampleSize()];			// colorData is the buffer in which data are returned
				
		//Select the edge type
		int buttonChoice;
		do
		{
			LCD.clear();

			LCD.drawString("<  Left  | Right  > ", 0, 0);
			LCD.drawString("         |          ", 0, 1);
			LCD.drawString(" Falling | Rising ", 0, 2);
			LCD.drawString("  Edge   | Edge   ", 0, 3);

			buttonChoice = Button.waitForAnyPress();
		} while (buttonChoice != Button.ID_LEFT 
				&& buttonChoice != Button.ID_RIGHT);
		USLocalizer.LocalizationType locType; 

		if (buttonChoice == Button.ID_LEFT)
		{ 
			locType = USLocalizer.LocalizationType.FALLING_EDGE;
		} else {
			locType = USLocalizer.LocalizationType.RISING_EDGE;
		}
		
		// setup the odometer and display
		Odometer odo = new Odometer(leftMotor, rightMotor, 30, true);
		LCDInfo lcd = new LCDInfo(odo);
		
		//Setup the Navigator
		Navigation navigator = new Navigation(odo);
		// perform the ultrasonic localization
		USLocalizer usl = new USLocalizer(odo, usValue, usData, locType,navigator);
		usl.doLocalization();
		
		//Orient the robot to the 0 axis and set the odometer
		navigator.turnTo(0.0, true);
		odo.setPosition(new double[] { 0.0, 0.0, 0.0 }, new boolean[] { true, true, true });
		Button.waitForAnyPress();
		
		// perform the light sensor localization
		LightLocalizer lsl = new LightLocalizer(odo, colorValue, colorData, navigator);
		lsl.doLocalization();			
		
		
		while (Button.waitForAnyPress() != Button.ID_ESCAPE);
		System.exit(0);
		
	}

}
