package widgets;

/**
 * The machines of one robot: how fast its tracks run and its turret turns, where
 * it is, and the state of their sounds
 */
final class Machine {
	// Written by the JavaFX thread
	volatile double treadSpeed;
	volatile double turretSpeed;
	volatile double leftGain = 1;
	volatile double rightGain = 1;
	// Eased and played by the audio thread
	double left = 1;
	double right = 1;
	double treadLevel;
	double turretLevel;
	double humPhase;
	double gearPhase;
	double rumble;
	double servoPhase;
}
