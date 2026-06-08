package frc.robot.drivetain;

import static edu.wpi.first.units.Units.Centimeter;
import static edu.wpi.first.units.Units.Centimeters;
import static edu.wpi.first.units.Units.Inches;
import static edu.wpi.first.units.Units.Meter;
import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.Minutes;
import static edu.wpi.first.units.Units.RadiansPerSecond;

import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.units.measure.LinearVelocity;

public class constants {
    public static final int[] RightID = {11,12};
    public static final int[] LeftID = {13,14};

    public static final double GearRatio = 10.71;
    public static final Distance WheelCirc = Inches.of(6).times(Math.PI);
    public static final Distance WheelDistance = Centimeters.of(50);
    public static final DifferentialDriveKinematics kinematics = new DifferentialDriveKinematics(WheelDistance);
    public static final LinearVelocity MaxVelocity = WheelCirc.per(Minutes).times(5676).div(GearRatio);
    public static final AngularVelocity MaxOmega = RadiansPerSecond.of(MaxVelocity.in(MetersPerSecond)/WheelDistance.div(2).in(Meters));
}
