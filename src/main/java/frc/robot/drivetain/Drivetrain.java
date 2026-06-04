package frc.robot.drivetain;

import static edu.wpi.first.units.Units.Percent;

import java.util.function.Supplier;

import com.revrobotics.PersistMode;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;

import edu.wpi.first.units.measure.Dimensionless;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Subsystem;

public class Drivetrain implements Subsystem {
    public SparkMax FrontRightMotor, BackRightMotor, FrontLeftMotor,BackLeftMotor;
    public RelativeEncoder LeftEncoder , RightEncoder;

    private SparkMaxConfig FrontLeftConfig,FrontRightConfig , BackLeftConfig ,BackRightConfig;

    private static Drivetrain inst;

    private Drivetrain (){
        FrontLeftMotor = new SparkMax (constants.LeftID[0],MotorType.kBrushless);
        FrontRightMotor = new SparkMax(constants.RightID[0],MotorType.kBrushless);
        BackLeftMotor = new SparkMax(constants.LeftID[1],MotorType.kBrushless);
        BackRightMotor = new SparkMax(constants.RightID[1],MotorType.kBrushless);

        LeftEncoder = FrontLeftMotor.getEncoder();
        RightEncoder = FrontRightMotor.getEncoder();

        FrontLeftConfig = new SparkMaxConfig();
        FrontRightConfig = new SparkMaxConfig();
        BackLeftConfig = new  SparkMaxConfig();
        BackRightConfig = new  SparkMaxConfig();

        FrontLeftConfig
            .idleMode(IdleMode.kBrake)
            .inverted(false)
            .voltageCompensation(12);
        BackLeftConfig
            .follow(FrontLeftMotor);
        FrontRightConfig
            .idleMode(IdleMode.kBrake)
            .inverted(true)
            .voltageCompensation(12);
        BackRightConfig
            .follow(FrontRightMotor);  
            
        FrontLeftMotor.configure(FrontLeftConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
        FrontRightMotor.configure(FrontRightConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
        BackLeftMotor.configure(BackLeftConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
        BackRightMotor.configure(BackRightConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
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

    public static Drivetrain getInstance(){
        inst = inst == null ? new Drivetrain() : inst;
        return inst;
    }
}