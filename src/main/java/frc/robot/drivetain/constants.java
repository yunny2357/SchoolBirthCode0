package frc.robot.drivetain;

import static edu.wpi.first.units.Units.Centimeter;
import static edu.wpi.first.units.Units.Centimeters;

import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.units.measure.Distance;

public class constants {
    public static final int[] RightID = {11,12};
    public static final int[] LeftID = {13,14};

    public static final double GearRatio = 10.71;
    public static final Distance WheelDistance = Centimeters.of(50);
    public static final DifferentialDriveKinematics kinematics = new DifferentialDriveKinematics(WheelDistance);

}
