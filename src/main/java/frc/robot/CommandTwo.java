package frc.robot;

import edu.wpi.first.wpilibj2.command.InstantCommand;

public class CommandTwo extends InstantCommand {

    public CommandTwo() {
        super(() -> System.out.println("CommandTwo initialize"));
      }
    
      @Override
      public boolean runsWhenDisabled() {
        return true;
      }
    
}
