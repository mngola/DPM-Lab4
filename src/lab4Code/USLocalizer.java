package lab4Code;

import lejos.hardware.Sound;
import lejos.robotics.SampleProvider;

public class USLocalizer {
	public enum LocalizationType { FALLING_EDGE, RISING_EDGE };
	public static int ROTATION_SPEED = 30;

	private Odometer odo;
	private SampleProvider usSensor;
	private float[] usData;
	private LocalizationType locType;

	private Navigation navigator;

	double lowNoise = 47.0;
	double upperNoise = 53.0;

	public USLocalizer(Odometer odo,  SampleProvider usSensor, float[] usData, LocalizationType locType, Navigation navi) {
		this.odo = odo;
		this.usSensor = usSensor;
		this.usData = usData;
		this.locType = locType;
		navigator = navi;
	}

	public void doLocalization() {
		double [] pos = {0.0,0.0,0.0};
		double angleA=0.0, angleB=0.0,angleD;
		boolean wall = false, inNoiseMargin = false;

		if(getFilteredData() < (lowNoise + upperNoise)/2.0)
		{
			wall = true;
		}
		if (locType == LocalizationType.FALLING_EDGE) {

			navigator.setSpeeds(ROTATION_SPEED, -ROTATION_SPEED);

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

			angleA = latchEdge(false);
			Sound.playTone(4000, 200);
			navigator.stop();

			navigator.setSpeeds(-ROTATION_SPEED, ROTATION_SPEED);

			angleB = latchEdge(false);
			Sound.playTone(4000, 100);
			navigator.stop();

			if (angleA < angleB) 
			{
				angleD = 45.0 - (angleA + angleB) / 2.0;
			} else {
				angleD = 225.0 - (angleA + angleB) / 2.0;
			}
		} else {
			/*
			 * The robot should turn until it sees the wall, then look for the
			 * "rising edges:" the points where it no longer sees the wall.
			 * This is very similar to the FALLING_EDGE routine, but the robot
			 * will face toward the wall for most of it.
			 */

			navigator.setSpeeds(ROTATION_SPEED, -ROTATION_SPEED);
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

			angleA = latchEdge(true);
			Sound.playTone(4000, 200);
			navigator.stop();

			navigator.setSpeeds(-ROTATION_SPEED, ROTATION_SPEED);

			angleB = latchEdge(true);
			Sound.playTone(4000, 100);
			navigator.stop();

			if (angleA < angleB) 
			{
				angleD = 45.0 - (angleA + angleB) / 2.0;
			} else {
				angleD = 225.0 - (angleA + angleB) / 2.0;
			}
		}
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

		return distance;
	}

}
