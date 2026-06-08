// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.Percent;

import dev.doglog.DogLog;
import dev.doglog.DogLogOptions;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.drivetain.Drivetrain;
import frc.robot.drivetain.constants;

public class RobotContainer {
  public Drivetrain drivetrain = Drivetrain.getInstance();
  public CommandXboxController controller = new CommandXboxController(0);
  public RobotContainer() {
    // drivetrain.setDefaultCommand(drivetrain.drive(
    //   () -> Percent.of(controller.getLeftY()*100),
    //   () -> Percent.of(controller.getRightX()*100)
    // ));

    drivetrain.setDefaultCommand(drivetrain.drive(() -> new ChassisSpeeds(
      constants.MaxVelocity.times(controller.getLeftX()),
      constants.MaxVelocity.times(controller.getLeftY()),
      constants.MaxOmega.times(controller.getRightX())
    )));
    configureBindings();

    DogLog.setOptions(new DogLogOptions()
    .withCaptureConsole(true)
    .withCaptureDs(true)
    .withNtPublish(true)
    .withCaptureNt(true));
    DogLog.setEnabled(true);
  }

  private void configureBindings() {}

  public Command getAutonomousCommand() {
    return Commands.print("No autonomous command configured");
  }
}
