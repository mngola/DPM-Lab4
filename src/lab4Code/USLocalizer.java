package lab4Code;

import lejos.hardware.Sound;
import lejos.robotics.SampleProvider;

public class USLocalizer {
	public enum LocalizationType { FALLING_EDGE, RISING_EDGE }
	private static final int THRESHOLD = 3;
	public static int ROTATION_SPEED = 80;

	private Odometer odo;
	private SampleProvider usSensor;
	private float[] usData;
	private LocalizationType locType;

	private Navigation navigator;

	double lowNoise = 101.0;
	double upperNoise = 107.0;

	private int count = 0;
	private float lastValidDistance = 255;

	public USLocalizer(Odometer odo,  SampleProvider usSensor, float[] usData, LocalizationType locType, Navigation navi) {
		this.odo = odo;
		this.usSensor = usSensor;
		this.usData = usData;
		this.locType = locType;
		navigator = navi;
	}

	public void doLocalization() {
		//Initialize the position
		double [] pos = {0.0,0.0,0.0};
		double angleA=0.0, angleB=0.0,angleD;
		boolean wall = false, inNoiseMargin = false;

		//if the filter is under the average noise, you're facing a wall
		if(getFilteredData() < (lowNoise + upperNoise)/2.0)
		{
			wall = true;
		}
		//Check the localization type
		if (locType == LocalizationType.FALLING_EDGE) {

			//begin rotating 
			navigator.setSpeeds(ROTATION_SPEED, -ROTATION_SPEED);

			//If the robot starts facing a wall, adjust until it's not 
			while(wall) {
				if(inNoiseMargin && (getFilteredData() > upperNoise)) {
					wall = false;
					inNoiseMargin = false;
				}
				else if(getFilteredData() > lowNoise) {
					inNoiseMargin = true;
				}
			}
			Sound.playTone(4000, 200);
			
			inNoiseMargin = false;
			/*
			 * Loop while there is no wall
			 * When the robot detect the falling edge, compute the angle A to latch to
			 * Stop the robot
			 */
			while(!wall){
				if(inNoiseMargin && (getFilteredData() < lowNoise))
				{
					angleA = latchEdge(false);
					Sound.playTone(4000, 200);
					navigator.stop();
					wall = true;
				}
				else if(getFilteredData() < upperNoise)
				{
					inNoiseMargin = true;
				}
			}
			
			//Rotate in the other direction
			navigator.setSpeeds(-ROTATION_SPEED, ROTATION_SPEED);
			
			wall = false;
			inNoiseMargin = false;
			//Keep rotating till the robot finds the other angle, then latch to it
			while(!wall){
				if(inNoiseMargin && (getFilteredData() < lowNoise))
				{
					angleB = latchEdge(false);
					Sound.playTone(4000, 100);
					navigator.stop();
					wall = true;
				}
				else if(getFilteredData() > upperNoise)
				{
					inNoiseMargin = true;
				}
			}
		

			//Compute the angle correction
			if (angleA < angleB) 
			{
				angleD = 45.0 - (angleA + angleB) / 2.0;
			} else {
				angleD = 225.0 - (angleA + angleB) / 2.0;
			}
		} else {
			//Begin rotating
			navigator.setSpeeds(ROTATION_SPEED, -ROTATION_SPEED);
			
			//If the robot starts facing a space, adjust until it's not 
			while(!wall) {
				if(inNoiseMargin && (getFilteredData() < upperNoise)) {
					wall = true;
					inNoiseMargin = false;
				}
				else if(getFilteredData() < lowNoise) {
					inNoiseMargin = true;
				}
			}
			Sound.playTone(4000, 200);
			
			/*
			 * Loop while there is a wall
			 * When the robot detect the rising edge, compute the angle A to latch to
			 * Stop the robot
			 */
			while(wall){
				if(inNoiseMargin && (getFilteredData() > upperNoise))
				{
					angleA = latchEdge(true);
					Sound.playTone(4000, 200);
					navigator.stop();
					wall = false;
				}
				else if(getFilteredData() > lowNoise)
				{
					inNoiseMargin = true;
				}
			}

			//Rotate in other direction
			navigator.setSpeeds(-ROTATION_SPEED, ROTATION_SPEED);
			wall = true;
			inNoiseMargin = false;

			//Keep rotating till the robot finds the other angle, then latch to it
			while(wall){
				if(inNoiseMargin && (getFilteredData() > upperNoise))
				{
					angleB = latchEdge(true);
					Sound.playTone(4000, 100);
					navigator.stop();
					wall = false;
					inNoiseMargin = false;
				}
				else if(getFilteredData() < lowNoise)
				{
					inNoiseMargin = true;
				}
			}

			//Compute the angle correction
			if (angleA < angleB) 
			{
				angleD = 225.0 - (angleA + angleB) / 2.0;
			} else {
				angleD = 45.0 - (angleA + angleB) / 2.0;
			}
		}
		//Adjust the odometer with the correction
		pos[2] = Odometer.fixDegAngle(odo.getAng() + angleD);
		odo.setPosition(pos, new boolean[] { true, true, true });
	}

	/*
	 * move: false for falling edge, true for rising edge
	 */
	private double latchEdge(boolean move) {
		double ang1 = 0.0;
		double ang2 = 0.0;
		boolean inNoiseMargin = false;

		if (move) {
			boolean wall = true;
			while (wall) {
				if (inNoiseMargin && (getFilteredData() > lowNoise)) {
					ang2 = odo.getAng();
					wall = false;
				} else if (getFilteredData() > upperNoise) {
					ang1 = odo.getAng();
					inNoiseMargin = true;
				}
			}
		}
		else {
			boolean wall = false;
			while (!wall) {
				if (inNoiseMargin && (getFilteredData() < lowNoise)) {
					ang2 = odo.getAng();
					wall = true;
				} else if (getFilteredData() < upperNoise) {
					ang1 = odo.getAng();
					inNoiseMargin = true;
				}
			}
		}

		return (ang1 + ang2) / 2.0;
	}

	private float getFilteredData() {
		usSensor.fetchSample(usData, 0);
		float distance = usData[0];
		
		/*
		 * Filter for removing spurious 255 values
		 * Check for the value, if the US has encountered this value more than the threshold, return it
		 * If not reutrn increase count, return the last value that made sense
		 * If the US doesn't see a 255 value, lower the count
		 */
		if (distance == 255) 
		{
			if (count >= THRESHOLD) 
			{
				return 255;
			} 
			else 
			{
				count++;
				return lastValidDistance ;
			}
		} 
		else if (count > 0) 
		{
			count--;
		}
		//Set the last sensible distance
		lastValidDistance = distance;
		
		return distance;
	}

}
