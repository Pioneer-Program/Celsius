package cute.ame.celsius.Command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class CelsiusDebugCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("celsius") // tbh copying Pioneer wasn't the greatest idea of all time, but i mean, it's 6am so yea renaming to "celsius" instead of "cdb" x>
            .requires(src -> src.hasPermission(2))
            .then(FluidDebugCommand.build())
            .then(ThermalDebugCommand.build())
        );
    }
}
