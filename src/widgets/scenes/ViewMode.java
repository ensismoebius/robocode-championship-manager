package widgets.scenes;


enum ViewMode {
	CINEMATIC("CINEMATOGRÁFICA", -30, 45), FREE("LIVRE", -38, 38), ISOMETRIC("ISOMÉTRICA", -35.26, 12), TOP_DOWN("VISTA DE CIMA", -89.5, 12);

	final String label;
	final double pitch;
	final double fov;

	/** A camera view: its label, the pitch of the camera and its field of view. */
	ViewMode(String label, double pitch, double fov) {
		this.label = label;
		this.pitch = pitch;
		this.fov = fov;
	}
}
