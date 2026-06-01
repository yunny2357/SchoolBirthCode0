// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.revrobotics.PersistMode;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.studica.frc.AHRS;
import com.studica.frc.AHRS.NavXComType;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

public class Robot extends TimedRobot {

  public SparkMax RightMotor,LeftMotor;
  public RelativeEncoder RighRelativeEncoder,LeftRelativeEncoder;
  
  public SparkMaxConfig RightConfig , LeftConfig;

  public XboxController controller = new XboxController(0);

  public AHRS gyro;

  public Robot() {
    RightMotor = new SparkMax(12, MotorType.kBrushless);
    LeftMotor = new SparkMax(11, MotorType.kBrushless);
    RighRelativeEncoder = RightMotor.getEncoder();
    LeftRelativeEncoder = LeftMotor.getEncoder();
    gyro = new AHRS(NavXComType.kMXP_SPI);
    
    LeftConfig = new SparkMaxConfig();
    RightConfig = new SparkMaxConfig();

    LeftConfig
      .idleMode(IdleMode.kBrake)
      .inverted(false)
      .voltageCompensation(12)
      .smartCurrentLimit(40);
    
    LeftConfig.encoder
      .positionConversionFactor(1/10.71)
      .velocityConversionFactor(1/10.71/60);

    RightConfig
      .idleMode(IdleMode.kBrake)
      .inverted(true)
      .voltageCompensation(12)
      .smartCurrentLimit(40);
    
    RightConfig.encoder
      .positionConversionFactor(1/10.71)
      .velocityConversionFactor(1/10.71/60);
    
    LeftMotor.configure(LeftConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    RightMotor.configure(RightConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
  }

  @Override
  public void robotPeriodic() {
  }

  @Override
  public void disabledInit() {}

  @Override
  public void disabledPeriodic() {}

  @Override
  public void disabledExit() {}

  @Override
  public void autonomousInit() {
  }

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void autonomousExit() {}

  @Override
  public void teleopInit() {
    
  }

  @Override
  public void teleopPeriodic() {
    LeftMotor.set(controller.getLeftY());
    RightMotor.set(controller.getRightY());
  }

  @Override
  public void teleopExit() {}

  @Override
  public void testInit() {
  }

  @Override
  public void testPeriodic() {}

  @Override
  public void testExit() {}
}
