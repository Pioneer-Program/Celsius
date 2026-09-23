package cute.ame.celsius.Command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class CelsiusDebugCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("cdb")
            .requires(src -> src.hasPermission(2))
            .then(FluidDebugCommand.build())
            .then(ThermalDebugCommand.build())
        );
    }
}
