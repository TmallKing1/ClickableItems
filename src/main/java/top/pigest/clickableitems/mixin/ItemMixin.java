package top.pigest.clickableitems.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(method = "use",at = @At(value = "HEAD"), cancellable = true)
    private void use(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if(!world.isClient && !user.isSpectator()) {
            ItemStack stack = user.getStackInHand(hand);
            if (hand == Hand.MAIN_HAND) {
                if (stack.getNbt() != null) {
                    NbtElement interaction = stack.getNbt().get("Interaction");
                    if (interaction != null) {
                        List<String> commands = new ArrayList<>();
                        boolean state = true;
                        boolean wave = false;
                        boolean consume = false;
                        int damage = 0;
                        if(interaction.getType() == NbtElement.STRING_TYPE) {
                            commands = Collections.singletonList(interaction.asString());
                        } else if(interaction.getType() == NbtElement.COMPOUND_TYPE) {
                            NbtCompound interaction1 = (NbtCompound) interaction;
                            NbtElement commands1 = interaction1.get("Commands");
                            wave = interaction1.getBoolean("Wave");
                            consume = interaction1.getBoolean("Consume");
                            damage = interaction1.getInt("Damage");
                            if(commands1 != null) {
                                if(commands1.getType() == NbtElement.STRING_TYPE) {
                                    commands = Collections.singletonList(commands1.asString());
                                } else if(commands1.getType() == NbtElement.LIST_TYPE) {
                                    NbtList commands2 = interaction1.getList("Commands", NbtElement.STRING_TYPE);
                                    if(commands2.isEmpty()) {
                                        state = false;
                                    }
                                    for (NbtElement element: commands2) {
                                        commands.add(element.asString());
                                    }
                                } else {
                                    state = false;
                                }
                            }
                        } else {
                            state = false;
                        }
                        if(!commands.stream().allMatch(s -> s.startsWith("/"))) {
                            state = false;
                        } else {
                            commands = commands.stream().map(s -> s.substring(1)).toList();
                        }
                        if(state) {
                            for(String command: commands) {
                                MinecraftServer server = user.getServer();
                                assert server != null;
                                server.getCommandManager().executeWithPrefix(new ServerCommandSource(server, user.getPos(), user.getRotationClient(), (ServerWorld) world, 2, "Server", user.getDisplayName(), server, user), command);
                            }
                            if (damage > 0) {
                                stack.damage(damage, user, playerEntity -> playerEntity.sendToolBreakStatus(hand));
                            }
                            if (consume) {
                                if (!user.isCreative()) {
                                    stack.decrement(1);
                                }
                            }
                            if (wave) {
                                cir.setReturnValue(TypedActionResult.success(stack));
                            }
                        }
                    }
                }
            }
        }
    }
}
