package lab4Code;

import lejos.robotics.SampleProvider;

public class LightLocalizer {
	//Constants
	private static final double LS_DIST = 12.5;
	private static final double upperLight = 0.45;
	private static final double lowerLight = 0.4;
	public static int ROTATION_SPEED = 30;

	private Odometer odo;
	private SampleProvider colorSensor;
	private float[] colorData;	
	private Navigation navigator;
	
	public LightLocalizer(Odometer odometer, SampleProvider colSen, float[] colData, Navigation navi) {
		odo = odometer;
		colorSensor = colSen;
		colorData = colData;
		navigator = navi;
	}
	
	public void doLocalization() {
		// Drive to the localization location (close to (0,0))
		navigator.turnTo(45.0, true);
		navigator.goForward(10.0);
		
		/*
		 * Track the angles using an array
		 *  index 0: -x axis, index 1: +y axis, index 2: +x axis index 3: -y axis 
		 */
		int lines = 0;
		double[] lineAngles = new double[4];
		// start rotating and clock all 4 gridlines
		navigator.setSpeeds(ROTATION_SPEED, -ROTATION_SPEED);
		
		boolean overLine = false;
		//Used correcting the line angles
		double lineAngle1 = 0.0;
		double lineAngle2 = 0.0;
		
		/*
		 * Loop through 4 lines
		 * When the light lowers, and you haven't crossed a line yet record the and
		 * When the rises, record the value and store in the array the average of the two values
		 */
		while (lines < 4) {
			colorSensor.fetchSample(colorData, 0);
			if ((colorData[0] < lowerLight) && (!overLine)) 
			{
				lineAngle1 = odo.getAng();
				overLine = true;
			}
			if ((colorData[0] > upperLight) && (overLine)) 
			{
				lineAngle2 = odo.getAng();
				lineAngles[lines] = (lineAngle2 + lineAngle1) / 2.0;
				lines++;
				lejos.hardware.Sound.playTone(4000, 100);
				overLine = false;
			}
		}
		
		navigator.stop();
		// do trig to compute (0,0) and 0 degrees 
		
		//Compute the offset of the x distance
		double thetaX = Math.abs(lineAngles[0] - lineAngles[2]);
		if(thetaX > 180.0) {
			thetaX = 360.0 - thetaX;
		}
		double correctX = -LS_DIST * Math.cos(Math.toRadians(thetaX / 2.0));
		
		//Compute the offset of the y distance
		double thetaY = Math.abs(lineAngles[1] - lineAngles[3]);
		if(thetaY > 180.0) {
			thetaY = 360.0 - thetaX;
		}
		double correctY = -LS_DIST * Math.cos(Math.toRadians(thetaY / 2.0));

		//Correct Theta
		double deltaTheta = 270 + (thetaX/2.0) - lineAngles[0];
		double correctTheta = odo.getAng() + deltaTheta;
		if (correctTheta > 180.0) {
			correctTheta -= 180.0;
		}
		
		//Set the odometer to the corrected position 
		odo.setPosition(new double[] { correctX, correctY, correctTheta }, new boolean[] { true, true, true });
		
		lejos.utility.Delay.msDelay(1000);
		// when done travel to (0,0) and turn to 0 degrees
		navigator.travelTo(0.0, 0.0);
		navigator.turnTo(0.0, true);
	}

}
