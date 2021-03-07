package frc.robot;

import edu.wpi.first.wpilibj2.command.InstantCommand;

public class CommandOne extends InstantCommand {

    public CommandOne() {
        super(() -> System.out.println("CommandOne initialize."));
      }
    
      @Override
      public boolean runsWhenDisabled() {
        return true;
      }
    
}
