// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.IterativeRobotBase;
import edu.wpi.first.wpilibj.SlewRateLimiter;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Watchdog;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.controller.RamseteController;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.trajectory.Trajectory;
import edu.wpi.first.wpilibj.trajectory.TrajectoryConfig;
import edu.wpi.first.wpilibj.trajectory.TrajectoryGenerator;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.ConditionalCommand;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;

import java.lang.reflect.Field;
import java.util.List;

public class Robot extends TimedRobot {
  private final XboxController m_controller = new XboxController(0);

  // Logitech controller Mode off, Dual-action (D)
  private final JoystickButton aButton = new JoystickButton(m_controller, 2);

  // Slew rate limiters to make joystick inputs more gentle; 1/3 sec from 0
  // to 1.
  private final SlewRateLimiter m_speedLimiter = new SlewRateLimiter(3);
  private final SlewRateLimiter m_rotLimiter = new SlewRateLimiter(3);

  private final Drivetrain m_drive = new Drivetrain();
  private final RamseteController m_ramsete = new RamseteController();
  private final Timer m_timer = new Timer();
  private Trajectory m_trajectory;

  private boolean toggleCommandState = false;

  @Override
  public void robotInit() {
    // Flush NetworkTables every loop. This ensures that robot pose and other values
    // are sent during every iteration.
    setNetworkTablesFlushEnabled(true);

    m_trajectory = TrajectoryGenerator.generateTrajectory(new Pose2d(2, 2, new Rotation2d()), List.of(),
        new Pose2d(6, 4, new Rotation2d()), new TrajectoryConfig(2, 2));

    configureButtons();
    
    if (Robot.isSimulation()) {
      // Set timeout for WatchDog higher to avoid false-positives from simulations
      try {
        Field field = IterativeRobotBase.class.getDeclaredField("m_watchdog");
        field.setAccessible(true);
        Watchdog watchdog = (Watchdog) field.get(this);
        watchdog.setTimeout(1.0);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
          e.printStackTrace();
        }
      }
    }

  @Override
  public void robotPeriodic() {
    m_drive.periodic();
    CommandScheduler.getInstance().run();
  }

  @Override
  public void autonomousInit() {
    m_timer.reset();
    m_timer.start();
    m_drive.resetOdometry(m_trajectory.getInitialPose());
  }

  @Override
  public void autonomousPeriodic() {
    double elapsed = m_timer.get();
    Trajectory.State reference = m_trajectory.sample(elapsed);
    ChassisSpeeds speeds = m_ramsete.calculate(m_drive.getPose(), reference);
    m_drive.drive(speeds.vxMetersPerSecond, speeds.omegaRadiansPerSecond);
  }

  @Override
  @SuppressWarnings("LocalVariableName")
  public void teleopPeriodic() {
    // Get the x speed. We are inverting this because Xbox controllers return
    // negative values when we push forward.
    double xSpeed =
        -m_speedLimiter.calculate(m_controller.getY(GenericHID.Hand.kLeft)) * Drivetrain.kMaxSpeed;

    // Get the rate of angular rotation. We are inverting this because we want a
    // positive value when we pull to the left (remember, CCW is positive in
    // mathematics). Xbox controllers return positive values when you pull to
    // the right by default.
    double rot =
        -m_rotLimiter.calculate(m_controller.getX(GenericHID.Hand.kRight))
            * Drivetrain.kMaxAngularSpeed;
    m_drive.drive(xSpeed, rot);
  }

  @Override
  public void simulationPeriodic() {
    m_drive.simulationPeriodic();
  }

  public void configureButtons() {
    SmartDashboard.putData("ToggleCommand",
      new ConditionalCommand(new CommandOne(), new CommandTwo(),
        ()->{
          System.out.println("SmartDashboard: Toggling state.");
          toggleCommandState = !toggleCommandState;
          return toggleCommandState;
        })
      );

    // Physical joystick button command connections
    aButton.whenPressed(new ConditionalCommand(new CommandOne(), new CommandTwo(),
      ()->{
        System.out.println("A Button: Toggling state.");
        toggleCommandState = !toggleCommandState;
        return toggleCommandState;
      })
    );
  }
}
