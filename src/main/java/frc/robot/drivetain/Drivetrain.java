package frc.robot.drivetain;

import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.Percent;
import static edu.wpi.first.units.Units.Seconds;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import com.revrobotics.PersistMode;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.studica.frc.AHRS;
import com.studica.frc.AHRS.NavXComType;

import dev.doglog.DogLog;
import edu.wpi.first.math.estimator.DifferentialDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.DifferentialDriveWheelPositions;
import edu.wpi.first.math.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.units.measure.Dimensionless;
import edu.wpi.first.units.measure.LinearVelocity;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Subsystem;

public class Drivetrain implements Subsystem {
    public SparkMax FrontRightMotor, BackRightMotor, FrontLeftMotor,BackLeftMotor;
    public RelativeEncoder LeftEncoder , RightEncoder;
    public AHRS gyro;
    public Field2d field;

    public DifferentialDrivePoseEstimator PoseEstimator;

    private SparkMaxConfig FrontLeftConfig,FrontRightConfig , BackLeftConfig ,BackRightConfig;

    private static Drivetrain inst;

    private Drivetrain (){
        FrontLeftMotor = new SparkMax (constants.LeftID[0],MotorType.kBrushless);
        FrontRightMotor = new SparkMax(constants.RightID[0],MotorType.kBrushless);
        BackLeftMotor = new SparkMax(constants.LeftID[1],MotorType.kBrushless);
        BackRightMotor = new SparkMax(constants.RightID[1],MotorType.kBrushless);

        LeftEncoder = FrontLeftMotor.getEncoder();
        RightEncoder = FrontRightMotor.getEncoder();

        gyro = new AHRS(NavXComType.kMXP_SPI);

        PoseEstimator = new DifferentialDrivePoseEstimator(constants.kinematics, gyro.getRotation2d(), getPosition().leftMeters, getPosition().rightMeters, new Pose2d());
        field = new Field2d();

        FrontLeftConfig = new SparkMaxConfig();
        FrontRightConfig = new SparkMaxConfig();
        BackLeftConfig = new  SparkMaxConfig();
        BackRightConfig = new  SparkMaxConfig();

        FrontLeftConfig
            .idleMode(IdleMode.kBrake)
            .inverted(false)
            .voltageCompensation(12);
        FrontLeftConfig.encoder
            .positionConversionFactor(1/constants.GearRatio)
            .velocityConversionFactor(1/constants.GearRatio/60);
        BackLeftConfig
            .follow(FrontLeftMotor);
        FrontRightConfig
            .idleMode(IdleMode.kBrake)
            .inverted(true)
            .voltageCompensation(12);
        FrontRightConfig.encoder
            .positionConversionFactor(1/constants.GearRatio)
            .velocityConversionFactor(1/constants.GearRatio/60);
        BackRightConfig
            .follow(FrontRightMotor);  
            
        FrontLeftMotor.configure(FrontLeftConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
        FrontRightMotor.configure(FrontRightConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
        BackLeftMotor.configure(BackLeftConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
        BackRightMotor.configure(BackRightConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    }

    public DifferentialDriveWheelPositions getPosition(){
        return new DifferentialDriveWheelPositions(
        constants.WheelCirc.times(LeftEncoder.getPosition()), 
        constants.WheelCirc.times(RightEncoder.getPosition()));
    }

    public DifferentialDriveWheelSpeeds getSpeeds(){
        return new DifferentialDriveWheelSpeeds(
            constants.WheelCirc.per(Seconds).times(LeftEncoder.getVelocity()),
            constants.WheelCirc.per(Seconds).times(RightEncoder.getVelocity())
        );
    }

    public Command drive(Supplier<Dimensionless> drive, Supplier<Dimensionless> steer){
        return run(() -> {
            double LeftSpeed = drive.get().in(Percent)+steer.get().in(Percent);
            double RightSpeed = drive.get().in(Percent)-steer.get().in(Percent);
            LeftSpeed /= 100;
            RightSpeed /= 100;
            LeftSpeed = LeftSpeed > 1 ? 1 : LeftSpeed;
            RightSpeed = RightSpeed > 1 ? 1 : RightSpeed;
            LeftSpeed = LeftSpeed < -1 ? -1 : LeftSpeed;
            RightSpeed = RightSpeed < -1 ? -1 : RightSpeed;
            FrontLeftMotor.set(LeftSpeed);
            FrontRightMotor.set(RightSpeed);
        });
    }

    public Command drive(Supplier<ChassisSpeeds> spds){
        return run(() -> {
            ChassisSpeeds spd = spds.get();
            ChassisSpeeds.discretize(spd, 0.02);
            DifferentialDriveWheelSpeeds whlSpeed = constants.kinematics.toWheelSpeeds(spd);
            whlSpeed.desaturate(constants.MaxVelocity);

            FrontLeftMotor.set(whlSpeed.leftMetersPerSecond/constants.MaxVelocity.in(MetersPerSecond));
            FrontRightMotor.set(whlSpeed.rightMetersPerSecond/constants.MaxVelocity.in(MetersPerSecond));
        });
    }

    @Override
    public void periodic(){
        PoseEstimator.update(gyro.getRotation2d(), getPosition());
        DogLog.log("Drivetrain/WheelSpeeds", getSpeeds());
        DogLog.log("Drivetrain/WheelPosition", getPosition());
        DogLog.log("Drivetrain/GyroHeading", gyro.getRotation2d());
        DogLog.log("Drivetrain/RobotPose", PoseEstimator.getEstimatedPosition());
        field.setRobotPose(PoseEstimator.getEstimatedPosition());
        SmartDashboard.putData("Drivetrain/RobotPoseLegacy", field);

    }

    public static Drivetrain getInstance(){
        inst = inst == null ? new Drivetrain() : inst;
        return inst;
    }
}