package com.mmap.client.mixin;

import com.mmap.client.command.EncryptCommand;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({class_634.class})
public abstract class ClientPlayNetworkHandlerMixin {
   public ClientPlayNetworkHandlerMixin() {
   }

   @Inject(
      method = {"sendCommand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mmapOnSendCommand(String command, CallbackInfoReturnable<Boolean> cir) {
      if (EncryptCommand.tryExecuteLocal(command)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"sendChatCommand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mmapOnSendChatCommand(String command, CallbackInfo ci) {
      if (EncryptCommand.tryExecuteLocal(command)) {
         ci.cancel();
      }
   }
}
